package com.flux.bank.ui.customer.utility

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.flux.bank.FluxApp
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.TransactionRepository
import com.flux.bank.data.repository.UserRepository
import com.flux.bank.domain.utility.BookingCode
import com.flux.bank.domain.utility.CatalogItem
import com.flux.bank.domain.utility.CatalogKind
import com.flux.bank.domain.utility.ServiceCatalog
import com.flux.bank.domain.utility.ServicePayments
import java.time.LocalDate

data class CatalogState(
    val kind: CatalogKind,
    val filterId: String,
    val items: List<CatalogItem>,
    val date: LocalDate? = null,
    val itemId: String? = null,
    val option: String? = null,
    val quantity: Int = 1,
    val total: Long = 0
) {
    val item: CatalogItem? get() = items.firstOrNull { it.id == itemId }
}

/** Vé máy bay / vé phim / khách sạn / mua sắm (mockup F5 màn 4–7): lọc → chọn một mục → tuỳ chọn, số lượng → thanh toán. */
class CatalogViewModel(
    private val kind: CatalogKind,
    accounts: AccountRepository = FluxApp.instance.accountRepository,
    transactions: TransactionRepository = FluxApp.instance.transactionRepository,
    users: UserRepository = FluxApp.instance.userRepository,
    auth: AuthRepository = FluxApp.instance.authRepository,
    now: () -> Long = System::currentTimeMillis,
    private val codeGen: () -> String = { BookingCode.generate() }
) : ServicePaymentViewModel(accounts, transactions, users, auth, now) {

    private val _catalog = MutableLiveData(initial())
    val catalog: LiveData<CatalogState> = _catalog

    // Mã đặt chỗ của lần thanh toán đang chờ sheet: sinh một lần để biên lai và giao dịch cùng mã
    private var pendingCode: String? = null

    fun selectFilter(id: String) {
        if (id == current().filterId || ServiceCatalog.filters(kind).none { it.id == id }) return
        update { it.copy(filterId = id, items = ServiceCatalog.items(kind, id), itemId = null, option = null) }
    }

    fun setDate(date: LocalDate) {
        val range = ServiceCatalog.dateRange(kind, now()) ?: return
        if (date in range) update { it.copy(date = date) }
    }

    fun selectItem(id: String) {
        if (current().items.none { it.id == id }) return
        update { it.copy(itemId = id, option = null) }
    }

    /** Suất chiếu không thuộc phim đang chọn hoặc đã qua thì bỏ qua. */
    fun selectOption(option: String) {
        val item = current().item ?: return
        if (option !in item.options || ServiceCatalog.isShowtimePast(option, now())) return
        update { it.copy(option = option) }
    }

    fun increment() = update { it.copy(quantity = minOf(it.quantity + 1, kind.quantityRange.last)) }

    fun decrement() = update { it.copy(quantity = maxOf(it.quantity - 1, kind.quantityRange.first)) }

    override fun draft(): Draft? {
        val s = current()
        val item = s.item ?: return null
        if (item.options.isNotEmpty() && s.option == null) return null
        if (kind.needsDate && s.date == null) return null
        val code = pendingCode ?: codeGen().also { pendingCode = it }
        return Draft(ServicePayments.booking(kind, item, s.quantity, s.option, s.date, code), txId = null)
    }

    override fun onPaid() { pendingCode = null }

    override fun onAuthCancelled() { pendingCode = null }

    private fun initial(): CatalogState {
        val filterId = ServiceCatalog.filters(kind).first().id
        return CatalogState(kind, filterId, ServiceCatalog.items(kind, filterId), date = ServiceCatalog.dateRange(kind, now())?.start)
    }

    private fun update(block: (CatalogState) -> CatalogState) {
        val next = block(current())
        _catalog.value = next.copy(total = next.item?.let { ServiceCatalog.total(it, next.quantity) } ?: 0)
        revalidate()
    }

    override fun currentAmount(): Long? = current().takeIf { it.item != null }?.total

    private fun current() = _catalog.value!!
}
