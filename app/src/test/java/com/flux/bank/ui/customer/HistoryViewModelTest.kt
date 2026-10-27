package com.flux.bank.ui.customer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Transaction
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeTransactionRepository
import com.flux.bank.testutil.MainDispatcherRule
import com.flux.bank.ui.customer.history.DayLabel
import com.flux.bank.ui.customer.history.HistoryRow
import com.flux.bank.ui.customer.history.HistoryViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HistoryViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) = LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()
    private val accounts = FakeAccountRepository().apply {
        this.accounts += BankAccount(accountId = "chk", ownerUid = "c1", type = AccountType.CHECKING)
        this.accounts += BankAccount(accountId = "sav", ownerUid = "c1", type = AccountType.SAVING)
        this.accounts += BankAccount(accountId = "mor", ownerUid = "c1", type = AccountType.MORTGAGE)
    }
    private val transactions = FakeTransactionRepository()
    private fun vm() = HistoryViewModel(transactions, accounts, FakeAuthRepository().apply { uid = "c1" }, zone) { LocalDate.of(2026, 10, 27) }
        .apply { load() }

    @Test
    fun groupsByLocalDay() {
        transactions.items += Transaction(id = "t1", ownerUid = "c1", accountId = "sav", createdAt = at(2026, 10, 27, 0, 1))
        transactions.items += Transaction(id = "t2", ownerUid = "c1", accountId = "chk", createdAt = at(2026, 10, 26, 23, 59))
        transactions.items += Transaction(id = "t3", ownerUid = "c1", accountId = "sav", createdAt = at(2026, 10, 24, 18, 40))
        val rows = vm().rows.value!!
        assertEquals(
            listOf(
                HistoryRow.Header(DayLabel.Today), HistoryRow.Item(transactions.items[0]),
                HistoryRow.Header(DayLabel.Yesterday), HistoryRow.Item(transactions.items[1]),
                HistoryRow.Header(DayLabel.On(LocalDate.of(2026, 10, 24))), HistoryRow.Item(transactions.items[2])
            ),
            rows
        )
    }

    @Test
    fun filterByAccount() {
        transactions.items += Transaction(id = "t1", ownerUid = "c1", accountId = "sav", createdAt = at(2026, 10, 27, 9, 0))
        transactions.items += Transaction(id = "t2", ownerUid = "c1", accountId = "chk", createdAt = at(2026, 10, 27, 8, 0))
        val vm = vm()
        assertEquals(listOf("chk", "sav"), vm.chips.value!!.map { it.accountId })
        vm.filter("chk")
        assertEquals(listOf("t2"), vm.rows.value!!.filterIsInstance<HistoryRow.Item>().map { it.tx.id })
        vm.filter(null)
        assertEquals(2, vm.rows.value!!.filterIsInstance<HistoryRow.Item>().size)
    }

    @Test
    fun emptyHistoryHasNoRows() {
        assertTrue(vm().rows.value!!.isEmpty())
    }
}
