package com.flux.bank.ui.auth.sheet

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.core.security.OtpManager
import com.flux.bank.core.security.OtpResult
import com.flux.bank.domain.FaceVerificationPolicy
import java.util.UUID

sealed class AuthSheetState {
    data class EnteringOtp(
        val digits: String = "",
        val remainingSec: Long = 0,
        @StringRes val errorRes: Int? = null,
        val attemptsLeft: Int = OtpManager.MAX_ATTEMPTS
    ) : AuthSheetState()

    object FaceStep : AuthSheetState()
    object Verified : AuthSheetState()
    object Locked : AuthSheetState()
}

/**
 * Logic của bottom sheet xác thực giao dịch: phát OTP gắn với một mã giao dịch mới, nhận từng chữ số từ bàn phím
 * riêng, kiểm tra mã; nếu số tiền thuộc diện giá trị cao thì chuyển sang bước khuôn mặt.
 *
 * @param deliver cách gửi mã tới người dùng (mặc định: SMS giả lập qua [com.flux.bank.core.security.OtpNotifier]).
 */
class TransactionAuthViewModel(
    private val otp: OtpManager = FluxApp.instance.otpManager,
    private val deliver: (String) -> Unit = { FluxApp.instance.otpNotifier.deliver(it) }
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
            OtpResult.VALID -> _state.value =
                if (FaceVerificationPolicy.requiresFace(amount)) AuthSheetState.FaceStep else AuthSheetState.Verified
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
