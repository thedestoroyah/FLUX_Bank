package com.flux.bank.ui.customer.utility

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.TransactionRepository
import com.flux.bank.data.repository.UserRepository
import com.flux.bank.domain.utility.Bill
import com.flux.bank.domain.utility.BillKind
import com.flux.bank.domain.utility.BillLookup
import com.flux.bank.domain.utility.BillProvider
import com.flux.bank.domain.utility.ServicePayments
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Form hoá đơn. [paidChecked] = đã biết chắc hoá đơn đã trả hay chưa (tra `paymentRef` thành công);
 * chưa biết thì không cho trả để tránh trả trùng.
 */
data class BillState(
    val provider: BillProvider,
    val code: String = "",
    val codeError: Boolean = false,
    val looking: Boolean = false,
    val bill: Bill? = null,
    val notFound: Boolean = false,
    val paidTx: Transaction? = null,
    val paidChecked: Boolean = false
)

/** Trả tiền điện / nước (mockup F5 màn 1–2): chọn nhà cung cấp → mã khách hàng → tra cứu → thanh toán. */
class BillPayViewModel(
    private val kind: BillKind,
    accounts: AccountRepository = FluxApp.instance.accountRepository,
    transactions: TransactionRepository = FluxApp.instance.transactionRepository,
    users: UserRepository = FluxApp.instance.userRepository,
    auth: AuthRepository = FluxApp.instance.authRepository,
    now: () -> Long = System::currentTimeMillis
) : ServicePaymentViewModel(accounts, transactions, users, auth, now) {

    private val _bill = MutableLiveData(BillState(BillProvider.forKind(kind).first()))
    val bill: LiveData<BillState> = _bill

    private var lookupJob: Job? = null

    fun selectProvider(provider: BillProvider) {
        if (provider.kind != kind || provider == current().provider) return
        lookupJob?.cancel()
        _bill.value = current().copy(provider = provider, bill = null, notFound = false, paidTx = null, paidChecked = false, looking = false)
    }

    /** Sửa mã thì bỏ hoá đơn đã tra (để không trả nhầm hoá đơn của mã cũ). */
    fun setCode(input: String) {
        if (input == current().code) return
        lookupJob?.cancel()
        _bill.value = current().copy(
            code = input, codeError = false, bill = null, notFound = false, paidTx = null, paidChecked = false, looking = false
        )
    }

    fun lookup() {
        val s = current()
        val code = BillLookup.normalizeCode(kind, s.code) ?: return run { _bill.value = s.copy(codeError = true) }
        val bill = BillLookup.lookup(s.provider, code, BillLookup.periodFor(now()))
            ?: return run { _bill.value = s.copy(notFound = true, bill = null) }
        _bill.value = s.copy(bill = bill, notFound = false, codeError = false, looking = true)
        lookupJob?.cancel()
        lookupJob = viewModelScope.launch { checkPaid(bill) }
    }

    override fun draft(): Draft? {
        val s = current()
        val bill = s.bill ?: return null
        if (s.looking || !s.paidChecked || s.paidTx != null) return null
        val uid = uid ?: return null
        return Draft(ServicePayments.bill(bill), BillLookup.txIdFor(uid, bill))
    }

    override suspend fun onAlreadyPaid() {
        current().bill?.let { checkPaid(it) }
    }

    private suspend fun checkPaid(bill: Bill) {
        val uid = uid ?: return
        val result = transactions.findByPaymentRef(uid, bill.key)
        if (current().bill != bill) return
        _bill.value = current().copy(looking = false, paidTx = result.getOrNull(), paidChecked = result.isSuccess)
        if (result.isFailure) setError(R.string.err_generic)
    }

    private fun current() = _bill.value!!
}
