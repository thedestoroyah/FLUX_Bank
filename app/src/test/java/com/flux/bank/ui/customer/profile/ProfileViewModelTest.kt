package com.flux.bank.ui.customer.profile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun masksEmailAndNationalId() {
        assertEquals("min•••@gmail.com", ProfileViewModel.maskEmail("minhanh@gmail.com"))
        assertEquals("ab•••@x.vn", ProfileViewModel.maskEmail("ab@x.vn"))
        assertEquals("•••• •••• 1234", ProfileViewModel.maskNationalId("079203001234"))
    }

    @Test
    fun enablingBiometricWritesLocalAndRemote() {
        val auth = FakeAuthRepository().apply { uid = "c1" }
        val users = FakeUserRepository(listOf(customer("c1")))
        val local = mutableMapOf<String, Boolean>()
        val vm = ProfileViewModel(auth, users, setLocalBiometric = { uid, on -> local[uid] = on }).apply { load() }
        vm.setBiometric(true)
        assertEquals(true, local["c1"])
        assertEquals("c1" to mapOf<String, Any?>("biometricEnabled" to true), users.updates.single())
        assertTrue(vm.state.value!!.biometricEnabled)
    }
}
