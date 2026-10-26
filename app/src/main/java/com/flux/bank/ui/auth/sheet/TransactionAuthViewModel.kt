package com.flux.bank.ui.auth.sheet

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.core.security.OtpManager
import com.flux.bank.core.security.OtpResult
import com.flux.bank.domain.FacePrecheck
import com.flux.bank.domain.FaceVerificationPolicy
import com.flux.bank.face.FaceMatcher
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.UUID

sealed class AuthSheetState {
    data class EnteringOtp(
        val digits: String = "",
        val remainingSec: Long = 0,
        @StringRes val errorRes: Int? = null,
        val attemptsLeft: Int = OtpManager.MAX_ATTEMPTS
    ) : AuthSheetState()

    data class FaceStep(
        val phase: FacePhase = FacePhase.CHECKING,
        val attemptsLeft: Int = MAX_FACE_ATTEMPTS,
        @StringRes val messageRes: Int? = null
    ) : AuthSheetState()

    data class Failed(@StringRes val messageRes: Int) : AuthSheetState()
    object Verified : AuthSheetState()
    object Locked : AuthSheetState()
}

/** Các pha của bước khuôn mặt: kiểm tra điều kiện → chờ chớp mắt → đối chiếu → khớp (hoặc bị chặn). */
enum class FacePhase { CHECKING, BLOCKED, BLINK, MATCHING, MATCHED }

/** Điều kiện và embedding tham chiếu (từ ảnh eKYC) của người đang đăng nhập. */
class FaceReference(val precheck: FacePrecheck, val embedding: FloatArray?)

const val MAX_FACE_ATTEMPTS = 3

/**
 * Logic của bottom sheet xác thực giao dịch: phát OTP gắn với một mã giao dịch mới, nhận từng chữ số từ bàn phím
 * riêng, kiểm tra mã; nếu số tiền thuộc diện giá trị cao thì chuyển sang bước khuôn mặt.
 *
 * @param deliver cách gửi mã tới người dùng (mặc định: SMS giả lập qua [com.flux.bank.core.security.OtpNotifier]).
 * @param loadFaceReference đọc điều kiện + embedding eKYC của người dùng hiện tại.
 */
class TransactionAuthViewModel(
    private val otp: OtpManager = FluxApp.instance.otpManager,
    private val deliver: (String) -> Unit = { FluxApp.instance.otpNotifier.deliver(it) },
    private val loadFaceReference: suspend () -> FaceReference = ::currentUserFaceReference
) : ViewModel() {

    private val _state = MutableLiveData<AuthSheetState>(AuthSheetState.EnteringOtp())
    val state: LiveData<AuthSheetState> = _state

    private var transactionId = ""
    private var amount: Long? = null
    private var started = false

    /** Bắt đầu phiên xác thực; gọi lại (ví dụ khi xoay màn hình) sẽ không phát mã mới. */
    fun start(amount: Long?) {
        if (started) return
        started = true
        this.amount = amount
        transactionId = UUID.randomUUID().toString()
        issue()
    }

    fun onDigit(digit: Char) = updateOtp { if (it.digits.length < CODE_LENGTH) it.copy(digits = it.digits + digit, errorRes = null) else it }

    fun onBackspace() = updateOtp { it.copy(digits = it.digits.dropLast(1)) }

    /** Tự điền mã khi người dùng chạm vào SMS giả lập. */
    fun fill(code: String) = updateOtp { it.copy(digits = code.filter(Char::isDigit).take(CODE_LENGTH), errorRes = null) }

    fun confirm() {
        val current = _state.value as? AuthSheetState.EnteringOtp ?: return
        when (otp.verify(transactionId, current.digits)) {
            OtpResult.VALID ->
                if (FaceVerificationPolicy.requiresFace(amount)) startFaceStep() else _state.value = AuthSheetState.Verified
            OtpResult.WRONG -> {
                val left = otp.attemptsLeft()
                _state.value = if (left == 0) AuthSheetState.Locked
                else current.copy(digits = "", errorRes = R.string.otp_wrong, attemptsLeft = left)
            }
            OtpResult.EXPIRED, OtpResult.NO_ACTIVE -> _state.value = current.copy(digits = "", errorRes = R.string.otp_expired)
            OtpResult.LOCKED -> _state.value = AuthSheetState.Locked
        }
    }

    fun resend() {
        if (_state.value !is AuthSheetState.EnteringOtp) return
        issue()
    }

    /** Gọi mỗi giây để cập nhật đồng hồ đếm ngược. */
    fun tick() = updateOtp { it.copy(remainingSec = otp.remainingSeconds()) }

    /** ML Kit đã thấy người dùng chớp mắt: chuyển sang đối chiếu khung hình kế tiếp. */
    fun onBlink() = updateFace { if (it.phase == FacePhase.BLINK) it.copy(phase = FacePhase.MATCHING, messageRes = null) else it }

    /** Embedding của khuôn mặt vừa chụp sau khi chớp mắt. */
    fun onFaceEmbedding(embedding: FloatArray) {
        val current = _state.value as? AuthSheetState.FaceStep ?: return
        if (current.phase != FacePhase.MATCHING) return
        val matched = runCatching { FaceMatcher.isMatch(embedding, reference ?: return) }.getOrDefault(false)
        _state.value = when {
            matched -> current.copy(phase = FacePhase.MATCHED, messageRes = null)
            current.attemptsLeft <= 1 -> AuthSheetState.Failed(R.string.face_failed)
            else -> current.copy(phase = FacePhase.BLINK, attemptsLeft = current.attemptsLeft - 1, messageRes = R.string.face_mismatch)
        }
    }

    /** UI đã hiện dấu tích "khớp": hoàn tất xác thực. */
    fun onMatchShown() {
        if ((_state.value as? AuthSheetState.FaceStep)?.phase == FacePhase.MATCHED) _state.value = AuthSheetState.Verified
    }

    private var reference: FloatArray? = null

    private fun startFaceStep() {
        _state.value = AuthSheetState.FaceStep()
        viewModelScope.launch {
            val ref = runCatching { loadFaceReference() }.getOrNull()
            _state.value = when (ref?.precheck) {
                null -> AuthSheetState.FaceStep(FacePhase.BLOCKED, messageRes = R.string.err_generic)
                FacePrecheck.NOT_VERIFIED -> AuthSheetState.FaceStep(FacePhase.BLOCKED, messageRes = R.string.face_not_verified)
                FacePrecheck.NO_EMBEDDING -> AuthSheetState.FaceStep(FacePhase.BLOCKED, messageRes = R.string.face_no_embedding)
                FacePrecheck.OK -> {
                    reference = ref.embedding
                    AuthSheetState.FaceStep(FacePhase.BLINK)
                }
            }
        }
    }

    private fun updateFace(change: (AuthSheetState.FaceStep) -> AuthSheetState.FaceStep) {
        val current = _state.value as? AuthSheetState.FaceStep ?: return
        _state.value = change(current)
    }

    private fun issue() {
        deliver(otp.issue(transactionId))
        _state.value = AuthSheetState.EnteringOtp(remainingSec = otp.remainingSeconds())
    }

    private fun updateOtp(change: (AuthSheetState.EnteringOtp) -> AuthSheetState.EnteringOtp) {
        val current = _state.value as? AuthSheetState.EnteringOtp ?: return
        _state.value = change(current)
    }

    companion object {
        const val CODE_LENGTH = 6
    }
}

/** Đọc hồ sơ + eKYC của người đang đăng nhập để biết có đối chiếu khuôn mặt được không. */
private suspend fun currentUserFaceReference(): FaceReference {
    val app = FluxApp.instance
    val uid = app.authRepository.currentUid() ?: error("Chưa đăng nhập")
    val user = app.userRepository.getUser(uid).getOrThrow()
    val kyc = app.kycRepository.get(uid).getOrNull()
    val embedding = kyc?.faceEmbedding?.map { it.toFloat() }?.toFloatArray()
    return FaceReference(FaceVerificationPolicy.precheck(user, kyc), embedding)
}
