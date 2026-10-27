package com.flux.bank.core

import com.flux.bank.core.validation.AmountError
import com.flux.bank.core.validation.Validators
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun validatesVnPhone() {
        assertEquals("0912345678", Validators.normalizePhone(" 0912 345 678 "))
        assertTrue(Validators.isValidVnPhone("0912345678"))
        assertFalse(Validators.isValidVnPhone("912345678"))
        assertFalse(Validators.isValidVnPhone("09123456789"))
        assertFalse(Validators.isValidVnPhone("09123a5678"))
    }

    @Test
    fun validatesNationalIdAndEmail() {
        assertTrue(Validators.isValidNationalId("079203001234"))
        assertFalse(Validators.isValidNationalId("07920300123"))
        assertTrue(Validators.isValidEmail("an.nguyen@gmail.com"))
        assertFalse(Validators.isValidEmail("an.nguyen@"))
        assertFalse(Validators.isValidEmail(""))
    }

    @Test
    fun passwordNeedsEightCharsWithLettersAndDigits() {
        assertTrue(Validators.isStrongEnoughPassword("flux2026"))
        assertFalse(Validators.isStrongEnoughPassword("12345678"))
        assertFalse(Validators.isStrongEnoughPassword("abcdefgh"))
        assertFalse(Validators.isStrongEnoughPassword("abc1"))
    }

    @Test
    fun amountLimits() {
        assertEquals(AmountError.BELOW_MIN, Validators.amountError(true, 9_999, 0))
        assertEquals(AmountError.ABOVE_DEPOSIT_MAX, Validators.amountError(true, 100_000_001, 0))
        assertNull(Validators.amountError(true, 100_000_000, 0))
        assertEquals(AmountError.INSUFFICIENT_FUNDS, Validators.amountError(false, 500_000, 499_999))
        assertNull(Validators.amountError(false, 500_000, 500_000))
    }

    @Test
    fun acceptsCommaDecimal() {
        assertEquals(5.0, Validators.parseRate("5,0")!!, 0.0)
        assertEquals(5.0, Validators.parseRate("5.0")!!, 0.0)
        assertEquals(4.5, Validators.parseRate(" 4,5 ")!!, 0.0)
    }

    @Test
    fun rejectsOutOfRange() {
        assertNull(Validators.parseRate("0"))
        assertNull(Validators.parseRate("30,01"))
        assertNull(Validators.parseRate("abc"))
        assertNull(Validators.parseRate(""))
        assertEquals(30.0, Validators.parseRate("30")!!, 0.0)
    }

    @Test
    fun parseAmountHandlesSeparatorsEmptyAndOverflow() {
        assertEquals(2_000_000L, Validators.parseAmount("2.000.000"))
        assertEquals(2_000_000L, Validators.parseAmount("2000000"))
        assertEquals(0L, Validators.parseAmount(""))
        assertEquals(0L, Validators.parseAmount("000"))
        assertEquals(999_999_999_999L, Validators.parseAmount("9".repeat(20)))
    }
}
