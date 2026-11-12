package com.flux.bank.domain.utility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class BookingCodeTest {

    @Test
    fun codeHasSixCharsFromAlphabet() {
        val code = BookingCode.generate(Random(1))
        assertEquals(6, code.length)
        assertTrue(code.all { it in BookingCode.ALPHABET })
    }

    @Test
    fun ambiguousCharsNeverAppear() {
        val random = Random(2)
        repeat(1_000) { assertTrue(BookingCode.generate(random).none { it in "0O1I" }) }
    }
}
