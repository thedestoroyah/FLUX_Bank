package com.flux.bank.ui.customer.utility

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import com.flux.bank.data.repository.AlreadyPaidException
import com.flux.bank.domain.TransferError
import com.flux.bank.domain.utility.BillKind
import com.flux.bank.domain.utility.BillLookup
import com.flux.bank.domain.utility.BillProvider
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeTransactionRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime

class BillPayViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val now = ZonedDateTime.parse("2026-11-12T09:30+07:00").toInstant().toEpochMilli()
    private val code = "PE01000123456"

    private val accounts = FakeAccountRepository().apply {
        this.accounts += BankAccount(accountId = "chk", ownerUid = "c1", type = AccountType.CHECKING, accountNumber = "886812034471", balance = 8_250_000)
        this.accounts += BankAccount(accountId = "sav", ownerUid = "c1", type = AccountType.SAVING, accountNumber = "886844172093", balance = 120_000_000)
        this.accounts += BankAccount(accountId = "mor", ownerUid = "c1", type = AccountType.MORTGAGE, accountNumber = "886890215536", balance = 1_000_000)
    }
    private val history = FakeTransactionRepository()

    private fun vm(kyc: String = KycStatus.VERIFIED) = BillPayViewModel(
        BillKind.ELECTRICITY, accounts, history,
        FakeUserRepository(listOf(customer("c1", kyc = kyc, name = "Nguyễn Văn An"))),
        FakeAuthRepository().apply { uid = "c1" }
    ) { now }.apply { load() }

    private fun BillPayViewModel.b() = bill.value!!
    private fun BillPayViewModel.c() = checkout.value!!
    private fun BillPayViewModel.looked(c: String = code) = apply { setCode(c); lookup() }

    @Test
    fun startsOnFirstProviderAndSkipsMortgage() {
        val vm = vm()
        assertEquals(BillProvider.EVN_HCM, vm.b().provider)
        assertEquals(listOf("chk", "sav"), vm.c().sources.map { it.accountId })
        assertEquals("chk", vm.c().sourceId)
    }

    @Test
    fun lookupShowsBill() {
        val vm = vm().looked()
        assertNotNull(vm.b().bill)
        assertEquals(code, vm.b().bill!!.customerCode)
        assertNull(vm.b().paidTx)
    }

    @Test
    fun invalidCodeSetsErrorWithoutLookup() {
        val vm = vm().looked("PE123")
        assertTrue(vm.b().codeError)
        assertNull(vm.b().bill)
    }

    @Test
    fun code0000IsNotFound() {
        val vm = vm().looked("PE01000120000")
        assertTrue(vm.b().notFound)
        assertNull(vm.b().bill)
    }

    @Test
    fun editingCodeClearsBill() {
        val vm = vm().looked()
        vm.setCode("PE01000123457")
        assertNull(vm.b().bill)
        assertNull(vm.b().paidTx)
        assertFalse(vm.b().notFound)
        assertFalse(vm.b().codeError)
    }

    @Test
    fun changingProviderClearsBill() {
        val vm = vm().looked()
        vm.selectProvider(BillProvider.EVN_HN)
        assertNull(vm.b().bill)
        assertEquals(BillProvider.EVN_HN, vm.b().provider)
    }

    @Test
    fun alreadyPaidBillShowsPaidTx() {
        val key = BillLookup.lookup(BillProvider.EVN_HCM, code, BillLookup.periodFor(now))!!.key
        val paid = Transaction(id = "old", ownerUid = "c1", type = TransactionType.PAYMENT, amount = 1, paymentRef = key)
        history.items += paid
        val vm = vm().looked()
        assertEquals(paid, vm.b().paidTx)
        vm.confirm()
        assertNull(vm.askAuth.value)
    }

    @Test
    fun paidCheckFailureBlocksPayment() {
        history.failFind = true
        val vm = vm().looked()
        assertNotNull(vm.b().bill)
        assertEquals(R.string.err_generic, vm.c().errorRes)
        vm.confirm()
        assertNull(vm.askAuth.value)
    }

    @Test
    fun payWritesPaymentWithBillTxId() {
        val vm = vm().looked()
        val bill = vm.b().bill!!
        vm.confirm()
        assertEquals(bill.amount, vm.askAuth.value!!.peek().amount)
        vm.onAuthResult(true)
        val tx = accounts.payments.single()
        assertEquals(TransactionType.PAYMENT, tx.type)
        assertEquals(BillLookup.txIdFor("c1", bill), tx.id)
        assertEquals("TIEN_DIEN", tx.service)
        assertEquals(tx, vm.done.value!!.peek())
        assertEquals(8_250_000 - bill.amount, accounts.accounts.first { it.accountId == "chk" }.balance)
    }

    @Test
    fun payFromSelectedSource() {
        val vm = vm().looked()
        vm.selectSource("sav")
        vm.confirm()
        vm.onAuthResult(true)
        assertEquals("sav", accounts.payments.single().accountId)
    }

    @Test
    fun doubleConfirmAsksAuthOnce() {
        val vm = vm().looked()
        var asked = 0
        vm.askAuth.observeForever { asked++ }
        vm.confirm()
        vm.confirm()
        assertEquals(1, asked)
    }

    @Test
    fun alreadyPaidOnWriteShowsPaid() {
        val vm = vm().looked()
        val bill = vm.b().bill!!
        vm.confirm()
        val paidElsewhere = Transaction(id = "x", ownerUid = "c1", type = TransactionType.PAYMENT, amount = bill.amount, paymentRef = bill.key)
        history.items += paidElsewhere
        accounts.failNextMove = AlreadyPaidException()
        vm.onAuthResult(true)
        assertEquals(R.string.err_already_paid, vm.c().errorRes)
        assertEquals(paidElsewhere, vm.b().paidTx)
        assertNull(vm.done.value)
        assertFalse(vm.c().submitting)
    }

    @Test
    fun insufficientFundsShowsError() {
        accounts.accounts[0] = accounts.accounts[0].copy(balance = 100_000)
        val vm = vm().looked()
        vm.confirm()
        assertEquals(TransferError.INSUFFICIENT_FUNDS, vm.c().amountError)
        assertNull(vm.askAuth.value)
    }

    @Test
    fun unverifiedIsBlocked() {
        val vm = vm(kyc = KycStatus.PENDING).looked()
        assertTrue(vm.c().kycBlocked)
        vm.confirm()
        assertNull(vm.askAuth.value)
    }

    @Test
    fun deniedAuthDoesNotPay() {
        val vm = vm().looked()
        var asked = 0
        vm.askAuth.observeForever { asked++ }
        vm.confirm()
        vm.onAuthResult(false)
        assertTrue(accounts.payments.isEmpty())
        vm.confirm()
        assertEquals(2, asked)
    }
}
