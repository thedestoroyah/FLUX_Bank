package com.flux.bank.ui.customer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Rates
import com.flux.bank.data.model.Transaction
import com.flux.bank.domain.PaymentFrequency
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeRatesRepository
import com.flux.bank.fakes.FakeTransactionRepository
import com.flux.bank.testutil.MainDispatcherRule
import com.flux.bank.ui.customer.account.AccountDetailViewModel
import com.flux.bank.ui.customer.account.SavingInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AccountDetailViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val start = LocalDate.of(2026, 10, 5).atStartOfDay(zone).toInstant().toEpochMilli()
    private val accounts = FakeAccountRepository().apply {
        this.accounts += BankAccount(accountId = "chk", ownerUid = "c1", type = AccountType.CHECKING, balance = 8_250_000)
        this.accounts += BankAccount(accountId = "sav", ownerUid = "c1", type = AccountType.SAVING, balance = 120_000_000)
        this.accounts += BankAccount(
            accountId = "mor", ownerUid = "c1", type = AccountType.MORTGAGE, balance = 1_500_000_000,
            loanAmount = 1_500_000_000, annualRate = 8.5, termMonths = 240, startDate = start
        )
    }
    private val rates = FakeRatesRepository(Rates(savingRate = 4.5))
    private val transactions = FakeTransactionRepository()
    private fun vm(id: String) = AccountDetailViewModel(
        accounts, rates, transactions, FakeAuthRepository().apply { uid = "c1" }, zone
    ) { LocalDate.of(2026, 10, 27) }.apply { load(id) }

    @Test
    fun savingShowsLiveRate() {
        val vm = vm("sav")
        assertEquals(SavingInfo(4.5, 450_000, 5_400_000), vm.state.value!!.saving)
        rates.rates = Rates(savingRate = 5.0)
        vm.load("sav")
        assertEquals(500_000L, vm.state.value!!.saving!!.monthlyInterest)
    }

    @Test
    fun checkingHasNoInterestBlock() {
        val s = vm("chk").state.value!!
        assertNull(s.saving)
        assertNull(s.mortgage)
        assertEquals(8_250_000L, s.account!!.balance)
    }

    @Test
    fun mortgageToggle() {
        val vm = vm("mor")
        val monthly = vm.state.value!!.mortgage!!
        assertEquals(PaymentFrequency.MONTHLY, monthly.frequency)
        assertEquals(13_017_349L, monthly.installment)
        assertEquals(240, monthly.periods)
        assertEquals(LocalDate.of(2026, 11, 5), monthly.nextDue)
        assertEquals(1_624_163_760L, monthly.totalInterest)
        assertEquals(3_124_163_760L, monthly.totalPayable)
        vm.setFrequency(PaymentFrequency.BIWEEKLY)
        val biweekly = vm.state.value!!.mortgage!!
        assertEquals(6_003_662L, biweekly.installment)
        assertEquals(520, biweekly.periods)
        assertEquals(LocalDate.of(2026, 11, 2), biweekly.nextDue)
        assertEquals(1_621_904_240L, biweekly.totalInterest)
    }

    @Test
    fun recentFiltersByAccountAndTakesFive() {
        repeat(7) { i -> transactions.items += Transaction(id = "s$i", accountId = "sav", ownerUid = "c1", createdAt = 1_000L + i) }
        transactions.items += Transaction(id = "c0", accountId = "chk", ownerUid = "c1", createdAt = 5_000L)
        val recent = vm("sav").state.value!!.recent
        assertEquals(listOf("s6", "s5", "s4", "s3", "s2"), recent.map { it.id })
    }
}
