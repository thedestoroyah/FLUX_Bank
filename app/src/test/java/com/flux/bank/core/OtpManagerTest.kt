package com.flux.bank.core

import com.flux.bank.core.security.OtpManager
import com.flux.bank.core.security.OtpResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OtpManagerTest {

    private var t = 0L
    private lateinit var m: OtpManager

    @Before
    fun setUp() {
        t = 0L
        m = OtpManager { t }
    }

    private fun wrongCodeFor(code: String) = if (code == "000000") "111111" else "000000"

    @Test
    fun validOnceThenNoActive() {
        val c = m.issue("tx1")
        assertEquals(OtpResult.VALID, m.verify("tx1", c))
        assertEquals(OtpResult.NO_ACTIVE, m.verify("tx1", c))
    }

    @Test
    fun codeIsBoundToItsTransaction() {
        val c = m.issue("tx1")
        assertEquals(OtpResult.WRONG, m.verify("tx2", c))
    }

    @Test
    fun expiresAfter120s() {
        val c = m.issue("tx1")
        t = 120_001
        assertEquals(OtpResult.EXPIRED, m.verify("tx1", c))
    }

    @Test
    fun stillValidAtExactly120s() {
        val c = m.issue("tx1")
        t = 120_000
        assertEquals(OtpResult.VALID, m.verify("tx1", c))
    }

    @Test
    fun locksAfterFiveWrongAttempts() {
        val c = m.issue("tx1")
        repeat(5) { assertEquals(OtpResult.WRONG, m.verify("tx1", wrongCodeFor(c))) }
        assertEquals(OtpResult.LOCKED, m.verify("tx1", c))
    }

    @Test
    fun attemptsLeftDecreases() {
        val c = m.issue("tx1")
        assertEquals(5, m.attemptsLeft())
        m.verify("tx1", wrongCodeFor(c))
        assertEquals(4, m.attemptsLeft())
    }

    @Test
    fun resendInvalidatesOldCode() {
        val old = m.issue("tx1")
        val new = m.issue("tx1")
        if (old != new) assertEquals(OtpResult.WRONG, m.verify("tx1", old))
        assertEquals(OtpResult.VALID, m.verify("tx1", new))
    }

    @Test
    fun codesAreSixDigits() {
        repeat(200) { assertTrue(m.issue("t").matches(Regex("\\d{6}"))) }
    }

    @Test
    fun remainingSecondsCountsDown() {
        m.issue("tx1")
        assertEquals(120L, m.remainingSeconds())
        t = 30_500
        assertEquals(89L, m.remainingSeconds())
        t = 200_000
        assertEquals(0L, m.remainingSeconds())
    }
}
