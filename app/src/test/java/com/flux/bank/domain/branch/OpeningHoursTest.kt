package com.flux.bank.domain.branch

import com.flux.bank.domain.TransferPolicy
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.TimeZone

class OpeningHoursTest {

    // 2026-11-10 là thứ Ba
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        LocalDateTime.of(y, m, d, h, min).atZone(TransferPolicy.VN_ZONE).toInstant().toEpochMilli()

    @Test
    fun weekdayMorningOpenUntil17() {
        assertEquals(OpenStatus.Open(LocalTime.of(17, 0)), OpeningHours.status(at(2026, 11, 10, 9)))
    }

    @Test
    fun weekdayEveningClosedUntilTomorrow8() {
        assertEquals(OpenStatus.Closed(LocalDateTime.of(2026, 11, 11, 8, 0)), OpeningHours.status(at(2026, 11, 10, 18)))
    }

    @Test
    fun saturdayAfternoonClosedUntilMonday() {
        assertEquals(OpenStatus.Closed(LocalDateTime.of(2026, 11, 16, 8, 0)), OpeningHours.status(at(2026, 11, 14, 13)))
    }

    @Test
    fun saturdayMorningOpenUntilNoon() {
        assertEquals(OpenStatus.Open(LocalTime.of(12, 0)), OpeningHours.status(at(2026, 11, 14, 10)))
    }

    @Test
    fun sundayClosed() {
        assertEquals(OpenStatus.Closed(LocalDateTime.of(2026, 11, 16, 8, 0)), OpeningHours.status(at(2026, 11, 15, 10)))
    }

    @Test
    fun beforeOpeningSameDay() {
        assertEquals(OpenStatus.Closed(LocalDateTime.of(2026, 11, 16, 8, 0)), OpeningHours.status(at(2026, 11, 16, 7)))
    }

    @Test
    fun usesVietnamTimeWhateverDeviceZone() {
        val saved = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            // 02:00 UTC thứ Ba = 09:00 giờ Việt Nam
            val millis = LocalDateTime.of(2026, 11, 10, 2, 0).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            assertEquals(OpenStatus.Open(LocalTime.of(17, 0)), OpeningHours.status(millis))
        } finally {
            TimeZone.setDefault(saved)
        }
    }
}
