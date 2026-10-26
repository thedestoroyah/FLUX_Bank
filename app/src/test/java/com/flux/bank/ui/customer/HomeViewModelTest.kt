package com.flux.bank.ui.customer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.data.model.BankAccount
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import com.flux.bank.ui.customer.home.HomeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val auth = FakeAuthRepository().apply { uid = "c1" }
    private val users = FakeUserRepository(listOf(customer("c1", name = "Nguyễn Minh Anh")))
    private val accounts = FakeAccountRepository().apply {
        this.accounts += BankAccount(accountId = "a1", ownerUid = "c1", accountNumber = "886812345678", balance = 48_250_000)
    }

    private fun vm(hour: Int = 9) = HomeViewModel(auth, users, accounts) { hour }

    @Test
    fun greetingDependsOnHour() {
        assertEquals(R.string.greeting_morning, HomeViewModel.greetingFor(8))
        assertEquals(R.string.greeting_afternoon, HomeViewModel.greetingFor(14))
        assertEquals(R.string.greeting_evening, HomeViewModel.greetingFor(20))
    }

    @Test
    fun loadsNameAndFormattedBalance() {
        val vm = vm().apply { load() }
        val state = vm.state.value!!
        assertEquals("Minh Anh", state.firstName)
        assertEquals(R.string.greeting_morning, state.greeting)
        val card = state.accounts.single()
        assertEquals("48.250.000 ₫", card.balanceText)
        assertEquals("•••• 5678", card.maskedNumber)
        assertFalse(state.loading)
    }

    @Test
    fun toggleHidesBalance() {
        val vm = vm().apply { load() }
        vm.toggleBalance()
        val card = vm.state.value!!.accounts.single()
        assertTrue(vm.state.value!!.balanceHidden)
        assertEquals("••••••• ₫", card.balanceText)
    }

    @Test
    fun shortNamesUseLastWord() {
        assertEquals("An", HomeViewModel.shortName("Trần An"))
        assertEquals("Minh Anh", HomeViewModel.shortName("Nguyễn Minh Anh"))
        assertEquals("", HomeViewModel.shortName(" "))
    }
}
