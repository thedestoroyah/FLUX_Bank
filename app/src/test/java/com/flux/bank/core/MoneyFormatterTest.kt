package com.flux.bank.core

import com.flux.bank.core.format.MoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    @Test
    fun formatsVndWithDotSeparatorAndSymbol() {
        assertEquals("48.250.000 ₫", MoneyFormatter.formatVnd(48_250_000))
        assertEquals("0 ₫", MoneyFormatter.formatVnd(0))
        assertEquals("999 ₫", MoneyFormatter.formatVnd(999))
    }

    @Test
    fun formatsSignedAmountsForListRows() {
        assertEquals("+2.000.000", MoneyFormatter.formatSignedVnd(2_000_000))
        assertEquals("−55.000", MoneyFormatter.formatSignedVnd(-55_000))
    }
}
