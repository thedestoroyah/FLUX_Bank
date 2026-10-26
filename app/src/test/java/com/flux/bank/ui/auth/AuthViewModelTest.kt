package com.flux.bank.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.data.model.UserRole
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val auth = FakeAuthRepository()

    private fun vm(users: FakeUserRepository) = AuthViewModel(auth, users)

    @Test
    fun firstLoginRoutesToChangePassword() {
        auth.loginResult = Result.success("c1")
        val vm = vm(FakeUserRepository(listOf(customer("c1").copy(mustChangePassword = true))))
        vm.login("c1@mail.com", "temp1234")
        assertTrue(vm.uiState.value is AuthUiState.NavigateToChangePassword)
    }

    @Test
    fun customerRoutesToCustomer() {
        auth.loginResult = Result.success("c1")
        val vm = vm(FakeUserRepository(listOf(customer("c1"))))
        vm.login("c1@mail.com", "pass1234")
        assertTrue(vm.uiState.value is AuthUiState.NavigateToCustomer)
    }

    @Test
    fun officerRoutesToOfficer() {
        auth.loginResult = Result.success("o1")
        val vm = vm(FakeUserRepository(listOf(customer("o1").copy(role = UserRole.BANK_OFFICER))))
        vm.login("o1@mail.com", "pass1234")
        assertTrue(vm.uiState.value is AuthUiState.NavigateToOfficer)
    }

    @Test
    fun blankEmailShowsVietnameseError() {
        val vm = vm(FakeUserRepository())
        vm.login("  ", "x")
        assertEquals(AuthUiState.Error(R.string.err_email_required), vm.uiState.value)
    }

    @Test
    fun wrongPasswordShowsGenericError() {
        auth.loginResult = Result.failure(IllegalStateException("INVALID_LOGIN_CREDENTIALS"))
        val vm = vm(FakeUserRepository())
        vm.login("a@b.com", "wrong")
        assertEquals(AuthUiState.Error(R.string.err_login_failed), vm.uiState.value)
    }

    @Test
    fun forgotPasswordNeedsValidEmail() {
        val vm = vm(FakeUserRepository())
        vm.forgotPassword("abc")
        assertEquals(AuthUiState.Error(R.string.err_email_invalid), vm.uiState.value)
        assertNull(auth.resetEmail)
        vm.forgotPassword("an@gmail.com")
        assertEquals("an@gmail.com", auth.resetEmail)
        assertEquals(AuthUiState.Info(R.string.msg_reset_email_sent), vm.uiState.value)
    }
}
