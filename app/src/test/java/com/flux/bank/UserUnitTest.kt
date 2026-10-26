package com.flux.bank

import com.flux.bank.data.model.User
import com.flux.bank.data.model.UserRole
import com.flux.bank.ui.auth.AuthUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserUnitTest {

    @Test
    fun testUserDefaultValues() {
        val user = User()
        assertEquals("", user.uid)
        assertEquals("", user.email)
        assertEquals("", user.fullName)
        assertEquals("", user.phoneNumber)
        assertEquals(UserRole.CUSTOMER, user.role)
        assertNull(user.idCardBase64)
        assertNull(user.faceImageBase64)
        assertTrue(user.createdAt > 0L)
    }

    @Test
    fun testUserCustomValuesAndOfficerRole() {
        val now = System.currentTimeMillis()
        val officer = User(
            uid = "officer_123",
            email = "officer@fluxbank.com",
            fullName = "Staff John",
            phoneNumber = "+84987654321",
            role = UserRole.BANK_OFFICER,
            idCardBase64 = "base64_id_dummy",
            faceImageBase64 = "base64_face_dummy",
            createdAt = now
        )

        assertEquals("officer_123", officer.uid)
        assertEquals("officer@fluxbank.com", officer.email)
        assertEquals(UserRole.BANK_OFFICER, officer.role)
        assertEquals("base64_id_dummy", officer.idCardBase64)
        assertEquals("base64_face_dummy", officer.faceImageBase64)
        assertEquals(now, officer.createdAt)
    }

    @Test
    fun testAuthUiStateTypes() {
        val customer = User(uid = "c_1", fullName = "Alice", role = UserRole.CUSTOMER)
        val officer = User(uid = "o_1", fullName = "Bob", role = UserRole.BANK_OFFICER)

        val idleState: AuthUiState = AuthUiState.Idle
        val loadingState: AuthUiState = AuthUiState.Loading
        val errorState: AuthUiState = AuthUiState.Error("Invalid credentials")
        val customerNavState: AuthUiState = AuthUiState.NavigateToCustomer(customer)
        val officerNavState: AuthUiState = AuthUiState.NavigateToOfficer(officer)

        assertNotNull(idleState)
        assertNotNull(loadingState)
        assertEquals("Invalid credentials", (errorState as AuthUiState.Error).message)
        assertEquals(UserRole.CUSTOMER, (customerNavState as AuthUiState.NavigateToCustomer).user.role)
        assertEquals(UserRole.BANK_OFFICER, (officerNavState as AuthUiState.NavigateToOfficer).user.role)
    }
}

