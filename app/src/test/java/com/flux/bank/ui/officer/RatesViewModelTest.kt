package com.flux.bank.ui.officer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.data.model.Rates
import com.flux.bank.data.model.UserRole
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeRatesRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import com.flux.bank.ui.officer.rates.RateKind
import com.flux.bank.ui.officer.rates.RatesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RatesViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val rates = FakeRatesRepository()
    private val officer = customer("o1", name = "Nguyen Van A").copy(role = UserRole.BANK_OFFICER)
    private fun vm() = RatesViewModel(rates, FakeAuthRepository().apply { uid = "o1" }, FakeUserRepository(listOf(officer))) { 1_793_000_000_000L }
        .apply { load() }

    @Test
    fun loadShowsDefaultsWhenConfigMissing() {
        val s = vm().state.value!!
        assertEquals(4.5, s.rates.savingRate, 0.0)
        assertEquals(8.5, s.rates.mortgageRate, 0.0)
    }

    @Test
    fun acceptsCommaDecimal() {
        vm().save(RateKind.SAVING, "5,0")
        assertEquals(Rates(5.0, 8.5, 1_793_000_000_000L, "o1", "Nguyen Van A"), rates.saved.single())
    }

    @Test
    fun rejectsOutOfRange() {
        val vm = vm()
        vm.save(RateKind.SAVING, "30,01")
        assertEquals(R.string.err_rate_range, vm.state.value!!.errorRes)
        vm.save(RateKind.MORTGAGE, "0")
        assertEquals(R.string.err_rate_range, vm.state.value!!.errorRes)
        assertTrue(rates.saved.isEmpty())
    }

    @Test
    fun previewUsesExampleAmount() {
        val vm = vm()
        assertEquals(416_666L, vm.examplePreview(RateKind.SAVING, "5"))
        assertNull(vm.examplePreview(RateKind.SAVING, "x"))
    }
}
