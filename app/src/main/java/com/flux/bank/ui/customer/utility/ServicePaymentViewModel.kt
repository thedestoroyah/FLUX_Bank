package com.flux.bank.ui.customer.utility

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.R
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.model.ServicePayment
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.User
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AlreadyPaidException
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.InsufficientFundsException
import com.flux.bank.data.repository.TransactionRepository
import com.flux.bank.data.repository.UserRepository
import com.flux.bank.domain.TransferError
import com.flux.bank.domain.TransferPolicy
import com.flux.bank.ui.common.Event
import kotlinx.coroutines.launch

/** Phần chung của mọi màn thanh toán dịch vụ: nguồn tiền, hạn mức còn lại, eKYC, trạng thái ghi, lỗi. */
data class CheckoutState(
    val sources: List<BankAccount> = emptyList(),
    val sourceId: String? = null,
    val remainingToday: Long = TransferPolicy.DAILY_LIMIT,
    val amountError: TransferError? = null,
    val kycBlocked: Boolean = false,
    val submitting: Boolean = false,
    @StringRes val errorRes: Int? = null
) {
    val source: BankAccount? get() = sources.firstOrNull { it.accountId == sourceId }
}

/**
 * Luồng thanh toán dùng chung cho Hoá đơn, Nạp ĐT, Danh mục (spec F5 §4):
 * form hợp lệ ([draft]) → kiểm hạn mức/số dư → sheet xác thực ([askAuth]) → [AccountRepository.pay] → [done].
 * Lớp con chỉ lo form riêng và trả về [Draft] khi form đủ, null khi chưa (tự đặt lỗi form).
 */
abstract class ServicePaymentViewModel(
    private val accounts: AccountRepository,
    protected val transactions: TransactionRepository,
    private val users: UserRepository,
    private val auth: AuthRepository,
    protected val now: () -> Long
) : ViewModel() {

    /** Yêu cầu thanh toán sẵn sàng; [txId] cố định với hoá đơn, null thì để repository tự sinh. */
    data class Draft(val payment: ServicePayment, val txId: String?)

    private val _checkout = MutableLiveData(CheckoutState())
    val checkout: LiveData<CheckoutState> = _checkout

    private val _askAuth = MutableLiveData<Event<ServicePayment>>()
    val askAuth: LiveData<Event<ServicePayment>> = _askAuth

    private val _done = MutableLiveData<Event<Transaction>>()
    val done: LiveData<Event<Transaction>> = _done

    private var kycStatus: String? = null
    // Yêu cầu đã kiểm lúc bấm "Thanh toán", đang chờ sheet; khác null thì chặn mở sheet thứ hai
    private var pending: Draft? = null

    protected val uid: String? get() = auth.currentUid()

    /** Tải hồ sơ (eKYC), tài khoản nguồn (không gồm Mortgage) và hạn mức còn lại trong ngày. */
    fun load() {
        val uid = uid ?: return
        viewModelScope.launch {
            val user = users.getUser(uid)
            kycStatus = user.getOrNull()?.kycStatus
            val sources = loadSources(uid)
            val history = transactions.listRecent(uid).getOrDefault(emptyList())
            updateCheckout { s ->
                s.copy(
                    sources = sources,
                    sourceId = s.sourceId?.takeIf { id -> sources.any { it.accountId == id } } ?: sources.firstOrNull()?.accountId,
                    remainingToday = TransferPolicy.remainingToday(history, now()),
                    kycBlocked = user.isSuccess && kycStatus != KycStatus.VERIFIED,
                    // Lỗi đọc hồ sơ (mạng) không có nghĩa là chưa eKYC
                    errorRes = if (user.isFailure) R.string.err_generic else s.errorRes
                )
            }
            onLoaded(user.getOrNull())
        }
    }

    fun selectSource(accountId: String) = updateCheckout { it.copy(sourceId = accountId, amountError = null, errorRes = null) }

    /** Kiểm form, số tiền, số dư và hạn mức; hợp lệ thì mở sheet xác thực (chỉ một lần cho tới khi sheet trả kết quả). */
    fun confirm() {
        val s = _checkout.value ?: return
        if (kycStatus != KycStatus.VERIFIED || s.submitting || pending != null) return
        val source = s.source ?: return
        val draft = draft() ?: return
        val error = TransferPolicy.checkAmount(draft.payment.amount, source.balance, s.remainingToday)
        if (error != null) return updateCheckout { it.copy(amountError = error) }
        updateCheckout { it.copy(amountError = null, errorRes = null) }
        pending = draft
        _askAuth.value = Event(draft.payment)
    }

    fun onAuthResult(verified: Boolean) {
        val draft = pending ?: return
        pending = null
        if (!verified) return onAuthCancelled()
        val sourceId = _checkout.value?.sourceId ?: return
        updateCheckout { it.copy(submitting = true, errorRes = null) }
        viewModelScope.launch {
            accounts.pay(sourceId, draft.payment, draft.txId)
                .onSuccess { tx ->
                    updateCheckout { it.copy(submitting = false) }
                    onPaid()
                    _done.value = Event(tx)
                }
                .onFailure { e -> onPayFailed(e) }
        }
    }

    /** Form đã đủ để trả tiền chưa; lớp con đặt lỗi form của mình khi trả null. */
    protected abstract fun draft(): Draft?

    /** Gọi sau khi tải hồ sơ (ví dụ điền sẵn SĐT). */
    protected open fun onLoaded(user: User?) = Unit

    /** Rules chặn vì hoá đơn đã được trả ở nơi khác: lớp con tra lại trạng thái. */
    protected open suspend fun onAlreadyPaid() = Unit

    /** Ghi thành công (ví dụ bỏ mã đặt chỗ đã dùng để lần mua sau có mã mới). */
    protected open fun onPaid() = Unit

    /** Người dùng đóng sheet hoặc xác thực thất bại (ví dụ huỷ mã đặt chỗ đã sinh). */
    protected open fun onAuthCancelled() = Unit

    protected fun setError(@StringRes res: Int?) = updateCheckout { it.copy(errorRes = res) }

    private suspend fun onPayFailed(e: Throwable) {
        val uid = uid
        when (e) {
            is AlreadyPaidException -> {
                onAlreadyPaid()
                updateCheckout { it.copy(submitting = false, errorRes = R.string.err_already_paid) }
            }
            is InsufficientFundsException -> {
                // Số dư đã đổi từ nơi khác: tải lại để lần sau kiểm bằng số dư thật
                val fresh = uid?.let { loadSources(it) }
                updateCheckout {
                    it.copy(submitting = false, sources = fresh ?: it.sources, errorRes = R.string.err_insufficient_funds)
                }
            }
            else -> updateCheckout { it.copy(submitting = false, errorRes = R.string.err_generic) }
        }
        onAuthCancelled()
    }

    private suspend fun loadSources(uid: String) =
        accounts.listByOwner(uid).getOrNull().orEmpty().filter { it.type != AccountType.MORTGAGE }

    private fun updateCheckout(block: (CheckoutState) -> CheckoutState) {
        _checkout.value = block(_checkout.value ?: CheckoutState())
    }
}
