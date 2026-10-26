package com.flux.bank.ui.customer.profile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.domain.CustomerOnboardingService
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

class EditProfileViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val auth = FakeAuthRepository().apply { uid = "c1" }
    private val users = FakeUserRepository(
        listOf(
            customer("c1", phone = "0912345678").copy(address = "Q.7"),
            customer("c2", phone = "0987111222")
        )
    )
    private val service = CustomerOnboardingService(auth, users, FakeAccountRepository()) { "c1" }
    private val vm by lazy { EditProfileViewModel(auth, users, service).apply { load() } }

    @Test
    fun addressOnlySavesWithoutOtp() {
        vm.save(phone = "0912 345 678", address = "Q.1")
        assertNull(vm.authRequest.value)
        assertEquals("c1" to mapOf<String, Any?>("address" to "Q.1"), users.updates.single())
        assertTrue(vm.state.value!!.saved)
    }

    @Test
    fun phoneChangeRequestsOtpBeforeWriting() {
        vm.save(phone = "0909000111", address = "Q.7")
        assertEquals("0909000111", vm.authRequest.value?.peek())
        assertTrue(users.phoneChanges.isEmpty())
        vm.onAuthResult(true)
        assertEquals(Triple("c1", "0912345678", "0909000111"), users.phoneChanges.single())
        assertTrue(vm.state.value!!.saved)
    }

    @Test
    fun cancelledOtpDoesNotWrite() {
        vm.save(phone = "0909000111", address = "Q.7")
        vm.onAuthResult(false)
        assertTrue(users.phoneChanges.isEmpty())
        assertTrue(users.updates.isEmpty())
    }

    @Test
    fun phoneOwnedByAnotherCustomerIsRejected() {
        vm.save(phone = "0987111222", address = "Q.7")
        assertEquals(R.string.err_phone_taken, vm.state.value!!.phoneError)
        assertNull(vm.authRequest.value)
    }

    @Test
    fun invalidPhoneIsRejected() {
        vm.save(phone = "12345", address = "Q.7")
        assertEquals(R.string.err_phone_invalid, vm.state.value!!.phoneError)
        assertNull(vm.authRequest.value)
    }
}
