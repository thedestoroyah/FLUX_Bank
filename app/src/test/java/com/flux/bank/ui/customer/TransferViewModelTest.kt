package com.flux.bank.ui.customer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.model.Payee
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import com.flux.bank.domain.TransferError
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakePayeeRepository
import com.flux.bank.fakes.FakeTransactionRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import com.flux.bank.ui.customer.transfer.TransferMode
import com.flux.bank.ui.customer.transfer.TransferPrefill
import com.flux.bank.ui.customer.transfer.TransferViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime

class TransferViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private fun at(iso: String) = ZonedDateTime.parse(iso).toInstant().toEpochMilli()
    private val now = at("2026-10-27T09:30+07:00")

    private val accounts = FakeAccountRepository().apply {
        this.accounts += BankAccount(accountId = "chk", ownerUid = "c1", type = AccountType.CHECKING, accountNumber = "886812034471", balance = 8_250_000)
        this.accounts += BankAccount(accountId = "sav", ownerUid = "c1", type = AccountType.SAVING, accountNumber = "886844172093", balance = 120_000_000)
        this.accounts += BankAccount(accountId = "mor", ownerUid = "c1", type = AccountType.MORTGAGE, accountNumber = "886890215536", balance = 1_000_000)
        this.accounts += BankAccount(accountId = "bobchk", ownerUid = "bob", accountNumber = "886855309182")
    }
    private val payees = FakePayeeRepository(
        mutableMapOf(
            "886855309182" to Payee("bobchk", "886855309182", "bob", "Trần Thị Bình"),
            "886812034471" to Payee("chk", "886812034471", "c1", "Nguyễn Văn An")
        )
    )
    private val history = FakeTransactionRepository()

    private fun vm(mode: TransferMode = TransferMode.INTERNAL, kyc: String = KycStatus.VERIFIED, prefill: TransferPrefill? = null) =
        TransferViewModel(
            mode, accounts, payees, history,
            FakeUserRepository(listOf(customer("c1", kyc = kyc, name = "Nguyễn Văn An"))),
            FakeAuthRepository().apply { uid = "c1" }
        ) { now }.apply { load(prefill) }

    private fun TransferViewModel.s() = state.value!!

    @Test
    fun lookupShowsPayee() {
        val vm = vm()
        vm.setAccountNumber("8868 5530 9182")
        assertEquals("bobchk", vm.s().payee!!.accountId)
        assertNull(vm.s().recipientError)
    }

    @Test
    fun unknownNumberIsNotFound() {
        val vm = vm()
        vm.setAccountNumber("886899999999")
        assertNull(vm.s().payee)
        assertEquals(TransferError.PAYEE_NOT_FOUND, vm.s().recipientError)
    }

    @Test
    fun ownSourceNumberIsSameAccount() {
        val vm = vm()
        vm.setAccountNumber("886812034471")
        assertEquals(TransferError.SAME_ACCOUNT, vm.s().recipientError)
    }

    @Test
    fun staleLookupIsIgnored() {
        val vm = vm()
        payees.findGate = CompletableDeferred()
        vm.setAccountNumber("886855309182")
        vm.setAccountNumber("886899999999")
        payees.findGate!!.complete(Unit)
        assertNull(vm.s().payee)
        assertEquals(TransferError.PAYEE_NOT_FOUND, vm.s().recipientError)
    }

    @Test
    fun overBalanceBlocked() {
        val vm = vm()
        vm.setAccountNumber("886855309182")
        vm.setAmount("9.000.000")
        vm.confirm()
        assertEquals(TransferError.INSUFFICIENT_FUNDS, vm.s().amountError)
        assertNull(vm.askAuth.value)
    }

    @Test
    fun dailyLimitFromHistory() {
        history.items += Transaction(ownerUid = "c1", type = TransactionType.TRANSFER_OUT, amount = 150_000_000, createdAt = at("2026-10-27T08:00+07:00"))
        history.items += Transaction(ownerUid = "c1", type = TransactionType.TRANSFER_OUT, amount = 90_000_000, createdAt = at("2026-10-26T08:00+07:00"))
        val vm = vm()
        assertEquals(50_000_000L, vm.s().remainingToday)
        vm.selectSource("sav")
        vm.setAccountNumber("886855309182")
        vm.setAmount("60000000")
        vm.confirm()
        assertEquals(TransferError.DAILY_LIMIT, vm.s().amountError)
    }

    @Test
    fun notVerifiedBlocked() {
        val vm = vm(kyc = KycStatus.PENDING)
        assertTrue(vm.s().kycBlocked)
        vm.setAccountNumber("886855309182")
        vm.setAmount("500000")
        vm.confirm()
        assertNull(vm.askAuth.value)
    }

    @Test
    fun doubleConfirmAsksAuthOnce() {
        val vm = vm()
        var asked = 0
        vm.askAuth.observeForever { asked++ }
        vm.setAccountNumber("886855309182")
        vm.setAmount("500000")
        vm.confirm()
        vm.confirm()
        assertEquals(1, asked)
    }

    @Test
    fun internalTransferSucceeds() {
        val vm = vm()
        vm.setAccountNumber("886855309182")
        vm.setAmount("500.000")
        vm.confirm()
        assertEquals("TRAN THI BINH", vm.askAuth.value!!.peek().recipientName)
        vm.onAuthResult(true)
        val tx = accounts.transfers.single()
        assertEquals(TransactionType.TRANSFER_OUT, tx.type)
        assertEquals(500_000L, tx.amount)
        assertEquals("TRAN THI BINH", tx.counterpartyName)
        assertEquals("NGUYEN VAN AN chuyen tien", tx.description)
        assertNotNull(vm.done.value)
        assertEquals(7_750_000L, accounts.accounts.first { it.accountId == "chk" }.balance)
    }

    @Test
    fun authDeniedWritesNothing() {
        val vm = vm()
        vm.setAccountNumber("886855309182")
        vm.setAmount("500000")
        vm.confirm()
        vm.onAuthResult(false)
        assertTrue(accounts.transfers.isEmpty())
    }

    @Test
    fun ownModeTargetsExcludeSource() {
        val vm = vm(TransferMode.OWN)
        assertEquals(listOf("sav"), vm.s().ownTargets.map { it.accountId })
        vm.setAmount("1000000")
        vm.confirm()
        vm.onAuthResult(true)
        assertEquals(121_000_000L, accounts.accounts.first { it.accountId == "sav" }.balance)
    }

    @Test
    fun externalNeedsBankAndName() {
        val vm = vm(TransferMode.EXTERNAL)
        vm.setAccountNumber("0071000412345")
        vm.setAmount("2000000")
        vm.confirm()
        assertEquals(TransferError.NO_BANK, vm.s().recipientError)
        vm.selectBank("VCB")
        vm.confirm()
        assertEquals(TransferError.NO_NAME, vm.s().recipientError)
    }

    @Test
    fun externalTransferNormalizesName() {
        val vm = vm(TransferMode.EXTERNAL)
        vm.selectBank("VCB")
        vm.setAccountNumber("0071000412345")
        vm.setExternalName("lê văn cường")
        vm.setAmount("2000000")
        vm.confirm()
        vm.onAuthResult(true)
        val tx = accounts.transfers.single()
        assertEquals("LE VAN CUONG", tx.counterpartyName)
        assertEquals("VCB", tx.counterpartyBank)
        assertNull(tx.pairTxId)
    }

    @Test
    fun prefillFromRecent() {
        val vm = vm(prefill = TransferPrefill("886855309182"))
        assertEquals("bobchk", vm.s().payee!!.accountId)
    }
}
