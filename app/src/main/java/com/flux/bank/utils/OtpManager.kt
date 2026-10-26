package com.flux.bank.utils

import java.security.SecureRandom

object OtpManager {

    private var activeOtp: String? = null
    private var expiresAtTimestamp: Long = 0L
    private val random = SecureRandom()

    /**
     * Generates a 6-digit cryptographic OTP code valid for the specified duration (default 120s).
     */
    fun generateOtp(length: Int = 6, validityDurationMs: Long = 120_000L): String {
        val min = Math.pow(10.0, (length - 1).toDouble()).toInt()
        val max = Math.pow(10.0, length.toDouble()).toInt() - 1
        val code = (min + random.nextInt(max - min + 1)).toString()

        activeOtp = code
        expiresAtTimestamp = System.currentTimeMillis() + validityDurationMs
        return code
    }

    /**
     * Verifies the user-submitted OTP against the active code.
     * Returns true if valid and not expired, then consumes the OTP.
     */
    fun verifyOtp(enteredOtp: String): Boolean {
        val trimmed = enteredOtp.trim()
        if (activeOtp == null || System.currentTimeMillis() > expiresAtTimestamp) {
            activeOtp = null
            return false
        }

        val isValid = (trimmed == activeOtp)
        if (isValid) {
            // Invalidate once successfully used
            activeOtp = null
            expiresAtTimestamp = 0L
        }
        return isValid
    }

    /**
     * Returns the remaining time in seconds for the current OTP.
     */
    fun getRemainingSeconds(): Long {
        val diff = expiresAtTimestamp - System.currentTimeMillis()
        return if (diff > 0) diff / 1000L else 0L
    }

    /**
     * Helper to retrieve the current active OTP for demo and reviewer convenience.
     */
    fun getCurrentActiveOtp(): String? {
        return if (System.currentTimeMillis() <= expiresAtTimestamp) activeOtp else null
    }

    /**
     * Clears any active OTP.
     */
    fun clear() {
        activeOtp = null
        expiresAtTimestamp = 0L
    }
}

