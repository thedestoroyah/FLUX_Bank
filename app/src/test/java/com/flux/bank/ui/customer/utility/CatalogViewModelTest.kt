package com.flux.bank.ui.customer.utility

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import com.flux.bank.domain.TransferError
import com.flux.bank.domain.utility.CatalogKind
import com.flux.bank.domain.utility.ServiceCatalog
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeTransactionRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class CatalogViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val now = ZonedDateTime.parse("2026-11-12T10:00+07:00").toInstant().toEpochMilli()
    private val accounts = FakeAccountRepository().apply {
        this.accounts += BankAccount(accountId = "chk", ownerUid = "c1", type = AccountType.CHECKING, accountNumber = "886812034471", balance = 8_250_000)
    }
    private val history = FakeTransactionRepository()

    private fun vm(kind: CatalogKind) = CatalogViewModel(
        kind, accounts, history,
        FakeUserRepository(listOf(customer("c1", kyc = KycStatus.VERIFIED))),
        FakeAuthRepository().apply { uid = "c1" }, { now }, { "K7Q2MX" }
    ).apply { load() }

    private fun CatalogViewModel.s() = catalog.value!!

    @Test
    fun startsOnFirstFilterWithDefaultDate() {
        val flight = vm(CatalogKind.FLIGHT)
        assertEquals("SGN-HAN", flight.s().filterId)
        assertEquals(ServiceCatalog.items(CatalogKind.FLIGHT, "SGN-HAN"), flight.s().items)
        assertEquals(LocalDate.of(2026, 11, 13), flight.s().date)
        assertEquals(LocalDate.of(2026, 11, 12), vm(CatalogKind.HOTEL).s().date)
        assertNull(vm(CatalogKind.MOVIE).s().date)
    }

    @Test
    fun totalFollowsItemAndQuantity() {
        val vm = vm(CatalogKind.FLIGHT)
        vm.selectItem("f1")
        vm.increment(); vm.increment()
        assertEquals(3, vm.s().quantity)
        assertEquals(1_890_000L * 3, vm.s().total)
    }

    @Test
    fun quantityClampedToRange() {
        val vm = vm(CatalogKind.FLIGHT)
        repeat(20) { vm.increment() }
        assertEquals(9, vm.s().quantity)
        repeat(20) { vm.decrement() }
        assertEquals(1, vm.s().quantity)
    }

    @Test
    fun changingFilterClearsItem() {
        val vm = vm(CatalogKind.MOVIE)
        vm.selectItem("m1")
        vm.selectOption("19:45")
        vm.selectFilter("galaxy")
        assertNull(vm.s().itemId)
        assertNull(vm.s().option)
        assertEquals(0L, vm.s().total)
        assertEquals(ServiceCatalog.items(CatalogKind.MOVIE, "galaxy"), vm.s().items)
    }

    @Test
    fun itemFromOtherFilterIgnored() {
        val vm = vm(CatalogKind.FLIGHT)
        vm.selectItem("f4")
        assertNull(vm.s().itemId)
    }

    @Test
    fun dateOutsideRangeIgnored() {
        val vm = vm(CatalogKind.FLIGHT)
        vm.setDate(LocalDate.of(2026, 11, 12))
        assertEquals(LocalDate.of(2026, 11, 13), vm.s().date)
        vm.setDate(LocalDate.of(2026, 11, 20))
        assertEquals(LocalDate.of(2026, 11, 20), vm.s().date)
    }

    @Test
    fun movieNeedsShowtime() {
        val vm = vm(CatalogKind.MOVIE)
        vm.selectItem("m3")
        vm.confirm()
        assertNull(vm.askAuth.value)
        vm.selectOption("09:30")
        assertNull(vm.s().option)
        vm.selectOption("16:40")
        vm.confirm()
        assertEquals(85_000L, vm.askAuth.value!!.peek().amount)
    }

    @Test
    fun noItemBlocksConfirm() {
        val vm = vm(CatalogKind.SHOP)
        vm.confirm()
        assertNull(vm.askAuth.value)
    }

    @Test
    fun totalOverLimitBlocksConfirm() {
        history.items += Transaction(ownerUid = "c1", type = TransactionType.TRANSFER_OUT, amount = 199_500_000, createdAt = now)
        val vm = vm(CatalogKind.FLIGHT)
        vm.selectItem("f1")
        vm.confirm()
        assertEquals(TransferError.DAILY_LIMIT, vm.checkout.value!!.amountError)
        assertNull(vm.askAuth.value)
    }

    @Test
    fun payBookingUsesGeneratedCode() {
        val vm = vm(CatalogKind.FLIGHT)
        vm.selectItem("f1")
        vm.increment()
        vm.confirm()
        vm.onAuthResult(true)
        val tx = accounts.payments.single()
        assertEquals("K7Q2MX", tx.paymentRef)
        assertEquals("VE_MAY_BAY", tx.service)
        assertEquals(3_780_000L, tx.amount)
        assertEquals("VN 213", tx.counterpartyAccount)
    }

    @Test
    fun totalOverLimitShowsErrorBeforeConfirm() {
        history.items += Transaction(ownerUid = "c1", type = TransactionType.TRANSFER_OUT, amount = 199_500_000, createdAt = now)
        val vm = vm(CatalogKind.FLIGHT)
        vm.selectItem("f1")
        assertEquals(TransferError.DAILY_LIMIT, vm.checkout.value!!.amountError)
    }

    @Test
    fun loweringQuantityClearsAmountError() {
        val vm = vm(CatalogKind.FLIGHT)
        vm.selectItem("f1")
        repeat(4) { vm.increment() }
        assertEquals(TransferError.INSUFFICIENT_FUNDS, vm.checkout.value!!.amountError)
        vm.decrement()
        assertNull(vm.checkout.value!!.amountError)
    }

    @Test
    fun changingFilterClearsAmountError() {
        val vm = vm(CatalogKind.FLIGHT)
        vm.selectItem("f1")
        repeat(4) { vm.increment() }
        vm.selectFilter("SGN-DAD")
        assertNull(vm.checkout.value!!.amountError)
    }
}
