package com.flux.bank.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChangePasswordViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val auth = FakeAuthRepository().apply { uid = "c1" }
    private val users = FakeUserRepository(listOf(customer("c1").copy(mustChangePassword = true)))
    private val vm = ChangePasswordViewModel(auth, users)

    @Test
    fun rulesReflectInput() {
        val r = vm.rules("flux2026", "flux2026")
        assertTrue(r.lengthOk && r.lettersAndDigits && r.matches)
        assertFalse(vm.rules("abc", "abd").matches)
    }

    @Test
    fun mismatchIsRejected() {
        vm.submit("flux2026", "flux2027")
        assertEquals(ChangePasswordState.Error(R.string.err_password_mismatch), vm.state.value)
        assertNull(auth.changedPassword)
    }

    @Test
    fun weakPasswordIsRejected() {
        vm.submit("12345678", "12345678")
        assertEquals(ChangePasswordState.Error(R.string.err_password_weak), vm.state.value)
    }

    @Test
    fun successClearsMustChangeFlag() {
        vm.submit("flux2026", "flux2026")
        assertEquals("flux2026", auth.changedPassword)
        assertEquals("c1" to mapOf<String, Any?>("mustChangePassword" to false), users.updates.single())
        assertTrue(vm.state.value is ChangePasswordState.Done)
    }
}
