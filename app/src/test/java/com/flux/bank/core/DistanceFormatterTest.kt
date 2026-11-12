package com.flux.bank.core

import com.flux.bank.core.format.DistanceFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceFormatterTest {

    @Test
    fun metersBelowOneKilometer() {
        assertEquals("850 m", DistanceFormatter.distance(850.4))
        assertEquals("999 m", DistanceFormatter.distance(999.0))
    }

    @Test
    fun kilometersWithCommaDecimal() {
        assertEquals("1,0 km", DistanceFormatter.distance(1000.0))
        assertEquals("2,1 km", DistanceFormatter.distance(2149.0))
    }

    @Test
    fun minutesRoundUp() {
        assertEquals(13, DistanceFormatter.minutes(777.0))
    }

    @Test
    fun durationRoundsUpToAtLeastOneMinute() {
        assertEquals(1, DistanceFormatter.minutes(5.0))
        assertEquals(1, DistanceFormatter.minutes(0.0))
    }
}
