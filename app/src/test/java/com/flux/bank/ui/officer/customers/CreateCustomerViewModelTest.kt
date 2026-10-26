package com.flux.bank.ui.officer.customers

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.domain.CustomerOnboardingService
import com.flux.bank.domain.NewCustomerInput
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreateCustomerViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val users = FakeUserRepository(listOf(customer("c1", phone = "0912345678")))
    private val service = CustomerOnboardingService(auth, users, FakeAccountRepository()) { "o1" }
    private val vm by lazy { CreateCustomerViewModel(service) }

    private fun input(phone: String = "0987000111", email: String = "trang@gmail.com") =
        NewCustomerInput("Phạm Thu Trang", email, phone, "079301004567", "45 Lê Lợi", "flux2026")

    @Test
    fun duplicatePhoneMapsToPhoneField() {
        vm.submit(input(phone = "0912345678"))
        assertEquals(mapOf(CustomerField.PHONE to R.string.err_phone_taken), vm.state.value!!.fieldErrors)
        assertNull(vm.created.value)
    }

    @Test
    fun invalidEmailMapsToEmailField() {
        vm.submit(input(email = "trang@"))
        assertEquals(R.string.err_email_invalid, vm.state.value!!.fieldErrors[CustomerField.EMAIL])
    }

    @Test
    fun successExposesTempPasswordOnce() {
        vm.submit(input())
        val event = vm.created.value!!
        assertEquals("flux2026", event.consume()!!.tempPassword)
        assertNull(event.consume())
        assertTrue(vm.state.value!!.fieldErrors.isEmpty())
    }

    @Test
    fun generatedPasswordsAreStrong() {
        repeat(50) {
            val pw = CreateCustomerViewModel.generatePassword()
            assertEquals(10, pw.length)
            assertTrue(pw.any(Char::isLetter) && pw.any(Char::isDigit))
        }
    }
}
