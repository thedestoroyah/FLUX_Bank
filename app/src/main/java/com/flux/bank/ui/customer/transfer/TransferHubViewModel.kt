package com.flux.bank.ui.customer.transfer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.TransactionRepository
import kotlinx.coroutines.launch

/** Tab Chuyển tiền: danh sách "Người nhận gần đây" (mockup F4 màn 1). */
class TransferHubViewModel(
    private val transactions: TransactionRepository = FluxApp.instance.transactionRepository,
    private val auth: AuthRepository = FluxApp.instance.authRepository
) : ViewModel() {

    private val _recent = MutableLiveData<List<RecentPayee>>(emptyList())
    val recent: LiveData<List<RecentPayee>> = _recent

    fun load() {
        val uid = auth.currentUid() ?: return
        viewModelScope.launch {
            _recent.value = recentPayees(transactions.listRecent(uid).getOrDefault(emptyList()))
        }
    }

    companion object {
        const val MAX_RECENT = 5

        /** Tối đa [MAX_RECENT] người nhận khác nhau (theo ngân hàng + số tài khoản) từ [history] mới nhất trước. */
        fun recentPayees(history: List<Transaction>): List<RecentPayee> = history
            .filter { it.type == TransactionType.TRANSFER_OUT }
            .map { RecentPayee(it.counterpartyBank, it.counterpartyAccount, it.counterpartyName) }
            .distinctBy { it.bankCode to it.accountNumber }
            .take(MAX_RECENT)
    }
}
