package com.flux.bank

import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.model.User
import com.flux.bank.data.model.UserRole
import com.flux.bank.utils.OtpManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecurityUnitTest {

    @Before
    fun setUp() {
        OtpManager.clear()
    }

    @Test
    fun testGenerateAndVerifyValidOtp() {
        val otp = OtpManager.generateOtp()
        assertEquals(6, otp.length)
        assertTrue(otp.all { it.isDigit() })
        assertEquals(otp, OtpManager.getCurrentActiveOtp())
        assertTrue(OtpManager.getRemainingSeconds() > 0L)

        // Valid verification
        val isVerified = OtpManager.verifyOtp(otp)
        assertTrue(isVerified)

        // After successful verification, code must be consumed
        assertNull(OtpManager.getCurrentActiveOtp())
    }

    @Test
    fun testOtpSingleUseAndWrongCode() {
        val otp = OtpManager.generateOtp()

        // Wrong code fails
        val wrongCode = if (otp == "123456") "654321" else "123456"
        val wrongResult = OtpManager.verifyOtp(wrongCode)
        assertFalse(wrongResult)

        // Correct code still works
        val correctResult = OtpManager.verifyOtp(otp)
        assertTrue(correctResult)

        // Re-use of same code fails
        val reUseResult = OtpManager.verifyOtp(otp)
        assertFalse(reUseResult)
    }

    @Test
    fun testOtpClear() {
        val otp = OtpManager.generateOtp()
        assertNotNull(OtpManager.getCurrentActiveOtp())

        OtpManager.clear()
        assertNull(OtpManager.getCurrentActiveOtp())
        assertFalse(OtpManager.verifyOtp(otp))
    }

    @Test
    fun testKycStatusConstants() {
        assertEquals("NOT_SUBMITTED", KycStatus.NOT_SUBMITTED)
        assertEquals("PENDING", KycStatus.PENDING)
        assertEquals("VERIFIED", KycStatus.VERIFIED)
        assertEquals("REJECTED", KycStatus.REJECTED)
    }

    @Test
    fun testUserSecurityFields() {
        val userDefault = User()
        assertEquals(KycStatus.NOT_SUBMITTED, userDefault.kycStatus)
        assertFalse(userDefault.isBiometricEnabled)

        val userCustom = User(
            uid = "u_999",
            email = "alice@fluxbank.com",
            fullName = "Alice Wonderland",
            role = UserRole.CUSTOMER,
            kycStatus = KycStatus.VERIFIED,
            isBiometricEnabled = true
        )
        assertEquals(KycStatus.VERIFIED, userCustom.kycStatus)
        assertTrue(userCustom.isBiometricEnabled)
    }
}
