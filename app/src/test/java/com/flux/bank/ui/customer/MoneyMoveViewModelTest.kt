package com.flux.bank.ui.customer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.core.validation.AmountError
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.repository.InsufficientFundsException
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import com.flux.bank.ui.customer.account.MoneyMoveViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MoneyMoveViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val accounts = FakeAccountRepository().apply {
        this.accounts += BankAccount(accountId = "chk", ownerUid = "c1", type = AccountType.CHECKING, balance = 499_999)
        this.accounts += BankAccount(accountId = "sav", ownerUid = "c1", type = AccountType.SAVING, balance = 120_000_000)
        this.accounts += BankAccount(accountId = "mor", ownerUid = "c1", type = AccountType.MORTGAGE, balance = 1_000_000)
    }

    private fun vm(deposit: Boolean, kyc: String = KycStatus.VERIFIED, preselect: String? = null) = MoneyMoveViewModel(
        deposit, accounts, FakeUserRepository(listOf(customer("c1", kyc = kyc))), FakeAuthRepository().apply { uid = "c1" }
    ).apply { load(preselect) }

    @Test
    fun listsOnlyCheckingAndSavingAndDefaultsToChecking() {
        val s = vm(deposit = true).state.value!!
        assertEquals(listOf("chk", "sav"), s.accounts.map { it.accountId })
        assertEquals("chk", s.selectedId)
        assertEquals("sav", vm(deposit = true, preselect = "sav").state.value!!.selectedId)
    }

    @Test
    fun notVerifiedIsBlocked() {
        val vm = vm(deposit = true, kyc = KycStatus.PENDING)
        assertTrue(vm.state.value!!.kycBlocked)
        vm.setAmount("2.000.000")
        vm.confirm()
        assertNull(vm.askAuth.value)
    }

    @Test
    fun withdrawOverBalanceShowsError() {
        val vm = vm(deposit = false)
        vm.setAmount("500000")
        vm.confirm()
        assertEquals(AmountError.INSUFFICIENT_FUNDS, vm.state.value!!.amountError)
        assertNull(vm.askAuth.value)
    }

    @Test
    fun balanceAfterPreview() {
        val vm = vm(deposit = true, preselect = "sav")
        vm.setAmount("2.000.000")
        assertEquals(122_000_000L, vm.state.value!!.balanceAfter)
    }

    @Test
    fun verifiedAuthDepositsOnceAndEmitsReceipt() {
        val vm = vm(deposit = true, preselect = "sav")
        vm.setAmount("2.000.000")
        vm.confirm()
        assertEquals(2_000_000L, vm.askAuth.value!!.peek())
        vm.onAuthResult(true)
        assertEquals(listOf("sav" to 2_000_000L), accounts.deposits)
        assertEquals(122_000_000L, vm.done.value!!.peek().balanceAfter)
        assertFalse(vm.state.value!!.submitting)
    }

    @Test
    fun secondConfirmWhileSubmittingIsIgnored() {
        val vm = vm(deposit = true)
        vm.setAmount("100000")
        accounts.moveGate = CompletableDeferred()
        vm.confirm()
        vm.confirm()
        vm.onAuthResult(true)
        vm.confirm()
        vm.onAuthResult(true)
        accounts.moveGate!!.complete(Unit)
        assertEquals(1, accounts.deposits.size)
    }

    @Test
    fun cancelledAuthResetsWithoutWriting() {
        val vm = vm(deposit = true)
        vm.setAmount("100000")
        vm.confirm()
        vm.onAuthResult(false)
        assertTrue(accounts.deposits.isEmpty())
        assertFalse(vm.state.value!!.submitting)
    }

    @Test
    fun insufficientFundsFromRepositoryShowsMessage() {
        val vm = vm(deposit = false, preselect = "sav")
        vm.setAmount("1.000.000")
        accounts.failNextMove = InsufficientFundsException()
        vm.confirm()
        vm.onAuthResult(true)
        assertEquals(R.string.err_insufficient_funds, vm.state.value!!.errorRes)
        assertNull(vm.done.value)
    }

    @Test
    fun profileLoadFailureIsNotTreatedAsMissingKyc() {
        val vm = MoneyMoveViewModel(true, accounts, FakeUserRepository(), FakeAuthRepository().apply { uid = "c1" }).apply { load(null) }
        assertFalse(vm.state.value!!.kycBlocked)
        assertEquals(R.string.err_generic, vm.state.value!!.errorRes)
        vm.setAmount("100000")
        vm.confirm()
        assertNull(vm.askAuth.value)
    }

    @Test
    fun noNegativeBalancePreviewWhenWithdrawingTooMuch() {
        val vm = vm(deposit = false)
        vm.setAmount("500000")
        assertNull(vm.state.value!!.balanceAfter)
    }
}
