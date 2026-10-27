package com.flux.bank.ui.officer.customers

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.Rates
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeRatesRepository
import com.flux.bank.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OpenAccountViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val accounts = FakeAccountRepository()
    private fun vm() = OpenAccountViewModel(accounts, FakeRatesRepository(Rates(mortgageRate = 8.5))).apply { load() }

    @Test
    fun openSavingCallsRepository() {
        val vm = vm()
        vm.submit("c1")
        assertEquals(AccountType.SAVING, accounts.opened.single().type)
        assertEquals("c1", accounts.opened.single().ownerUid)
        assertEquals(AccountType.SAVING, vm.opened.value!!.peek().type)
    }

    @Test
    fun mortgagePreviewUsesPolicyRate() {
        val vm = vm()
        vm.selectType(AccountType.MORTGAGE)
        vm.setLoanAmount("1.500.000.000")
        vm.setTerm(240)
        val s = vm.state.value!!
        assertEquals(13_017_349L, s.monthly)
        assertEquals(6_003_662L, s.biweekly)
        vm.submit("c1")
        val opened = accounts.opened.single()
        assertEquals(1_500_000_000L, opened.loanAmount)
        assertEquals(240, opened.termMonths)
        assertEquals(8.5, opened.annualRate, 0.0)
    }

    @Test
    fun rejectsZeroAmountAndBadTerm() {
        val vm = vm()
        vm.selectType(AccountType.MORTGAGE)
        vm.setLoanAmount("0")
        vm.submit("c1")
        assertEquals(R.string.err_loan_amount, vm.state.value!!.errorRes)
        vm.setLoanAmount("1000000")
        vm.setTerm(0)
        vm.submit("c1")
        assertEquals(R.string.err_loan_term, vm.state.value!!.errorRes)
        vm.setTerm(361)
        vm.submit("c1")
        assertEquals(R.string.err_loan_term, vm.state.value!!.errorRes)
        assertTrue(accounts.opened.isEmpty())
    }

    @Test
    fun secondSubmitIgnoredWhileSubmitting() {
        val vm = vm()
        accounts.openGate = CompletableDeferred()
        vm.submit("c1")
        vm.submit("c1")
        accounts.openGate!!.complete(Unit)
        assertEquals(1, accounts.opened.size)
    }
}
