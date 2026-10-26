package com.flux.bank.ui.ekyc

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.data.model.KycRecord
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.repository.KycRepository
import com.flux.bank.data.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ảnh đã chụp; tách khỏi Bitmap để ViewModel unit test được trên JVM. */
interface CapturedPhoto {
    /** Ảnh nén JPEG dạng Base64 để lưu Firestore. */
    fun toBase64(): String
}

/** Kết quả phân tích ảnh selfie: phải có đúng một khuôn mặt mới tính được embedding. */
sealed class FaceAnalysis {
    object NoFace : FaceAnalysis()
    object MultipleFaces : FaceAnalysis()
    class One(val embedding: FloatArray) : FaceAnalysis()
}

data class EkycUiState(
    val status: String = KycStatus.NOT_SUBMITTED,
    val rejectReason: String? = null,
    val hasIdPhoto: Boolean = false,
    val hasSelfie: Boolean = false,
    val loading: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val submitted: Boolean = false
) {
    val canSubmit get() = hasIdPhoto && hasSelfie && !loading && status != KycStatus.PENDING && status != KycStatus.VERIFIED
}

/**
 * Nộp hồ sơ eKYC (CCCD + selfie) cho [targetUid]; dùng chung cho khách tự nộp và nhân viên chụp hộ.
 * Selfie phải có đúng một khuôn mặt; embedding được tính trước khi gửi để dùng cho xác thực giao dịch.
 */
class EkycViewModel(
    private val kyc: KycRepository = FluxApp.instance.kycRepository,
    private val users: UserRepository = FluxApp.instance.userRepository,
    private val analyzeFace: suspend (CapturedPhoto) -> FaceAnalysis = { (it as BitmapPhoto).analyzeFace() },
    private val worker: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _state = MutableLiveData(EkycUiState())
    val state: LiveData<EkycUiState> = _state
    private var idPhoto: CapturedPhoto? = null
    private var selfie: CapturedPhoto? = null

    fun load(targetUid: String) {
        viewModelScope.launch {
            users.getUser(targetUid).onSuccess { user ->
                update { it.copy(status = user.kycStatus, rejectReason = user.kycRejectReason) }
            }
        }
    }

    fun setIdPhoto(photo: CapturedPhoto) {
        idPhoto = photo
        update { it.copy(hasIdPhoto = true, errorRes = null) }
    }

    fun setSelfie(photo: CapturedPhoto) {
        selfie = photo
        update { it.copy(hasSelfie = true, errorRes = null) }
    }

    fun submit(targetUid: String, submittedBy: String) {
        val id = idPhoto ?: return error(R.string.ekyc_need_id)
        val face = selfie ?: return error(R.string.ekyc_need_selfie)
        update { it.copy(loading = true, errorRes = null) }
        viewModelScope.launch {
            val analysis = withContext(worker) { runCatching { analyzeFace(face) }.getOrNull() }
            val embedding = (analysis as? FaceAnalysis.One)?.embedding
                ?: return@launch error(if (analysis == null) R.string.err_generic else R.string.ekyc_need_one_face)
            val record = withContext(worker) {
                KycRecord(
                    uid = targetUid,
                    idCardBase64 = id.toBase64(),
                    faceImageBase64 = face.toBase64(),
                    faceEmbedding = embedding.map { it.toDouble() },
                    submittedBy = submittedBy
                )
            }
            kyc.submit(record)
                .onSuccess { update { it.copy(loading = false, submitted = true, status = KycStatus.PENDING, rejectReason = null) } }
                .onFailure { error(R.string.err_generic) }
        }
    }

    private fun error(@StringRes res: Int) = update { it.copy(loading = false, errorRes = res) }

    private fun update(change: (EkycUiState) -> EkycUiState) {
        _state.value = change(_state.value ?: EkycUiState())
    }
}
