package com.flux.bank.ui.customer.utility

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.KycStatus
import com.flux.bank.domain.utility.Carrier
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeTransactionRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TopUpViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val accounts = FakeAccountRepository().apply {
        this.accounts += BankAccount(accountId = "chk", ownerUid = "c1", type = AccountType.CHECKING, accountNumber = "886812034471", balance = 8_250_000)
    }

    private fun vm() = TopUpViewModel(
        accounts, FakeTransactionRepository(),
        FakeUserRepository(listOf(customer("c1", phone = "0912345678", kyc = KycStatus.VERIFIED))),
        FakeAuthRepository().apply { uid = "c1" }
    ) { 0L }.apply { load() }

    private fun TopUpViewModel.t() = topUp.value!!

    @Test
    fun prefillsOwnPhone() {
        val vm = vm()
        assertEquals("0912345678", vm.t().phone)
        assertEquals(Carrier.VINAPHONE, vm.t().carrier)
        assertFalse(vm.t().phoneError)
    }

    @Test
    fun editingPhoneRedetectsCarrier() {
        val vm = vm()
        vm.setPhone("0981 234 567")
        assertEquals(Carrier.VIETTEL, vm.t().carrier)
    }

    @Test
    fun unknownPrefixBlocksConfirm() {
        val vm = vm()
        vm.setPhone("0111234567")
        vm.selectAmount(100_000)
        assertTrue(vm.t().phoneError)
        assertNull(vm.t().carrier)
        vm.confirm()
        assertNull(vm.askAuth.value)
    }

    @Test
    fun partialPhoneShowsNoErrorUntilConfirm() {
        val vm = vm()
        vm.setPhone("0912")
        vm.selectAmount(100_000)
        assertFalse(vm.t().phoneError)
        vm.confirm()
        assertTrue(vm.t().phoneError)
        assertNull(vm.askAuth.value)
    }

    @Test
    fun noAmountBlocksConfirm() {
        val vm = vm()
        vm.confirm()
        assertNull(vm.askAuth.value)
    }

    @Test
    fun amountOutsideDenominationsIgnored() {
        val vm = vm()
        vm.selectAmount(15_000)
        assertNull(vm.t().amount)
    }

    @Test
    fun payTopUp() {
        val vm = vm()
        vm.selectAmount(100_000)
        vm.confirm()
        vm.onAuthResult(true)
        val tx = accounts.payments.single()
        assertEquals("NAP_DT", tx.service)
        assertEquals(100_000L, tx.amount)
        assertEquals("0912345678", tx.counterpartyAccount)
        assertEquals("pay0", tx.id)
    }

    @Test
    fun amountAboveBalanceShowsErrorOnSelect() {
        accounts.accounts[0] = accounts.accounts[0].copy(balance = 50_000)
        val vm = vm()
        vm.selectAmount(100_000)
        assertEquals(com.flux.bank.domain.TransferError.INSUFFICIENT_FUNDS, vm.checkout.value!!.amountError)
        vm.selectAmount(50_000)
        assertNull(vm.checkout.value!!.amountError)
    }
}
