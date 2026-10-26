package com.flux.bank.core

import com.flux.bank.core.validation.Validators
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
}
