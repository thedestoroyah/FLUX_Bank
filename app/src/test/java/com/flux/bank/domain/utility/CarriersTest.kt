package com.flux.bank.domain.utility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarriersTest {

    private val table = mapOf(
        Carrier.VIETTEL to listOf("032", "033", "034", "035", "036", "037", "038", "039", "086", "096", "097", "098"),
        Carrier.VINAPHONE to listOf("081", "082", "083", "084", "085", "088", "091", "094"),
        Carrier.MOBIFONE to listOf("070", "076", "077", "078", "079", "089", "090", "093"),
        Carrier.VIETNAMOBILE to listOf("052", "056", "058", "092")
    )

    @Test
    fun everyPrefixInTheTableIsDetected() = table.forEach { (carrier, prefixes) ->
        prefixes.forEach { assertEquals("$it", carrier, Carriers.detect("${it}1234567")) }
    }

    @Test
    fun spacesAreIgnored() = assertEquals(Carrier.VINAPHONE, Carriers.detect("0912 345 678"))

    @Test
    fun unknownPrefixOrBadLengthIsNull() {
        assertNull(Carriers.detect("0111234567"))
        assertNull(Carriers.detect("091234567"))
        assertNull(Carriers.detect(""))
    }

    @Test
    fun denominations() =
        assertEquals(listOf(10_000L, 20_000L, 50_000L, 100_000L, 200_000L, 500_000L), Carriers.DENOMINATIONS)
}
