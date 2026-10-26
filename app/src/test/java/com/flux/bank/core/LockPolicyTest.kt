package com.flux.bank.core

import com.flux.bank.core.security.LockPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPolicyTest {

    private val p = LockPolicy()

    @Test
    fun coldStartLocksOnlyWithSessionAndBiometric() {
        assertTrue(p.onColdStart(hasSession = true, biometricEnabled = true, biometricAvailable = true))
        assertFalse(p.onColdStart(hasSession = true, biometricEnabled = false, biometricAvailable = true))
        assertFalse(p.onColdStart(hasSession = false, biometricEnabled = true, biometricAvailable = true))
    }

    @Test
    fun neverLocksWithoutBiometricHardware() {
        assertFalse(p.onColdStart(hasSession = true, biometricEnabled = true, biometricAvailable = false))
        assertFalse(p.onForeground(0, 999_999, hasSession = true, biometricEnabled = true, biometricAvailable = false))
    }

    @Test
    fun foregroundLocksAtExactly60s() {
        assertTrue(p.onForeground(0, 60_000, hasSession = true, biometricEnabled = true, biometricAvailable = true))
        assertFalse(p.onForeground(0, 59_999, hasSession = true, biometricEnabled = true, biometricAvailable = true))
    }

    @Test
    fun foregroundWithoutBackgroundTimestampDoesNotLock() {
        assertFalse(p.onForeground(null, 10, hasSession = true, biometricEnabled = true, biometricAvailable = true))
    }
}
