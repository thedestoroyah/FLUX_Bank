package com.flux.bank.ui.customer.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.TransactionRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Nhãn nhóm ngày trong lịch sử. */
sealed class DayLabel {
    object Today : DayLabel()
    object Yesterday : DayLabel()
    data class On(val date: LocalDate) : DayLabel()
}

/** Một dòng của danh sách lịch sử: tiêu đề ngày hoặc một giao dịch. */
sealed class HistoryRow {
    data class Header(val day: DayLabel) : HistoryRow()
    data class Item(val tx: Transaction) : HistoryRow()
}

/**
 * Tab Lịch sử (mockup màn 5): 100 giao dịch mới nhất của khách, nhóm theo ngày theo múi giờ [zone], lọc theo tài khoản.
 *
 * @param today ngày hiện tại, tách ra để test nhãn "Hôm nay"/"Hôm qua".
 */
class HistoryViewModel(
    private val transactions: TransactionRepository = FluxApp.instance.transactionRepository,
    private val accounts: AccountRepository = FluxApp.instance.accountRepository,
    private val auth: AuthRepository = FluxApp.instance.authRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _rows = MutableLiveData<List<HistoryRow>>(emptyList())
    val rows: LiveData<List<HistoryRow>> = _rows

    private val _chips = MutableLiveData<List<BankAccount>>(emptyList())
    val chips: LiveData<List<BankAccount>> = _chips

    private val _failed = MutableLiveData(false)
    val failed: LiveData<Boolean> = _failed

    private var all: List<Transaction> = emptyList()
    private var filterId: String? = null

    fun load() {
        val uid = auth.currentUid() ?: return
        viewModelScope.launch {
            val result = transactions.listRecent(uid)
            _failed.value = result.isFailure
            all = result.getOrNull().orEmpty()
            val used = all.map { it.accountId }.toSet()
            _chips.value = accounts.listByOwner(uid).getOrNull().orEmpty().filter { it.accountId in used }
            publish()
        }
    }

    /** [accountId] null = tất cả tài khoản. */
    fun filter(accountId: String?) {
        filterId = accountId
        publish()
    }

    fun selectedFilter(): String? = filterId

    private fun publish() {
        val list = all.filter { filterId == null || it.accountId == filterId }.sortedByDescending { it.createdAt }
        val now = today()
        _rows.value = list.groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }
            .flatMap { (date, items) -> listOf(HistoryRow.Header(labelFor(date, now))) + items.map { HistoryRow.Item(it) } }
    }

    private fun labelFor(date: LocalDate, now: LocalDate): DayLabel = when (date) {
        now -> DayLabel.Today
        now.minusDays(1) -> DayLabel.Yesterday
        else -> DayLabel.On(date)
    }
}
