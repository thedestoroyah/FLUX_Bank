package com.flux.bank.ui.officer.ekyc

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.data.model.KycRecord
import com.flux.bank.data.model.User
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.KycRepository
import com.flux.bank.ui.common.Event
import kotlinx.coroutines.launch

/** Lý do từ chối có sẵn; [OTHER] cho phép nhân viên tự nhập. */
enum class RejectReason(@StringRes val label: Int) {
    BLURRY_ID(R.string.reject_blurry_id),
    UNCLEAR_FACE(R.string.reject_unclear_face),
    MISMATCH(R.string.reject_mismatch),
    OTHER(R.string.reject_other)
}

/**
 * Hàng đợi duyệt eKYC của nhân viên (dùng chung giữa màn danh sách và màn chi tiết qua activityViewModels).
 *
 * [reasonText] đổi lý do có sẵn thành chữ để lưu (UI gán getString; test dùng mặc định).
 */
class EkycReviewViewModel(
    private val kyc: KycRepository = FluxApp.instance.kycRepository,
    private val auth: AuthRepository = FluxApp.instance.authRepository
) : ViewModel() {

    private val _queue = MutableLiveData<List<Pair<User, KycRecord>>>(emptyList())
    val queue: LiveData<List<Pair<User, KycRecord>>> = _queue

    private val _selected = MutableLiveData<Pair<User, KycRecord>?>()
    val selected: LiveData<Pair<User, KycRecord>?> = _selected

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    private val _message = MutableLiveData<Event<Int>>()
    val message: LiveData<Event<Int>> = _message

    var reasonText: (RejectReason) -> String = { it.name }

    fun load() {
        viewModelScope.launch {
            _queue.value = kyc.pendingQueue().getOrDefault(emptyList())
            _loading.value = false
        }
    }

    fun select(uid: String) {
        _selected.value = _queue.value?.firstOrNull { it.first.uid == uid }
    }

    fun approve() = decide(approve = true, reason = null)

    /** Trả về thông báo lỗi nếu chưa có lý do hợp lệ; null nếu đã gửi quyết định. */
    @StringRes
    fun reject(reason: RejectReason?, otherText: String): Int? {
        val text = when (reason) {
            null -> null
            RejectReason.OTHER -> otherText.trim().takeIf { it.isNotEmpty() }
            else -> reasonText(reason)
        } ?: return R.string.reject_reason_required
        decide(approve = false, reason = text)
        return null
    }

    private fun decide(approve: Boolean, reason: String?) {
        val (user, _) = _selected.value ?: return
        val officer = auth.currentUid() ?: return
        viewModelScope.launch {
            kyc.review(user.uid, approve, reason, officer)
                .onSuccess {
                    _queue.value = _queue.value.orEmpty().filterNot { it.first.uid == user.uid }
                    _selected.value = null
                    _message.value = Event(if (approve) R.string.review_approved else R.string.review_rejected)
                }
                .onFailure { _message.value = Event(R.string.err_generic) }
        }
    }

    companion object {
        /** Hồ sơ do nhân viên chụp hộ (người nộp khác chính khách hàng). */
        fun isAssisted(record: KycRecord): Boolean = record.submittedBy.isNotEmpty() && record.submittedBy != record.uid
    }
}
