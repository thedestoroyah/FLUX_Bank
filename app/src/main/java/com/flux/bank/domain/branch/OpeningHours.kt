package com.flux.bank.domain.branch

import com.flux.bank.domain.TransferPolicy
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Trạng thái mở cửa: đang mở tới [Open.closesAt], hoặc đóng tới [Closed.opensAt] (giờ Việt Nam). */
sealed interface OpenStatus {
    data class Open(val closesAt: LocalTime) : OpenStatus
    data class Closed(val opensAt: LocalDateTime) : OpenStatus
}

/** Giờ mở cửa chung của mọi chi nhánh: T2–T6 8:00–17:00, T7 8:00–12:00, CN nghỉ. Chỉ để hiển thị. */
object OpeningHours {

    private val OPEN = LocalTime.of(8, 0)
    private val WEEKDAY_CLOSE = LocalTime.of(17, 0)
    private val SATURDAY_CLOSE = LocalTime.of(12, 0)

    /** Trạng thái tại thời điểm [nowMillis], luôn tính theo giờ Việt Nam bất kể múi giờ của máy. */
    fun status(nowMillis: Long): OpenStatus {
        val now = Instant.ofEpochMilli(nowMillis).atZone(TransferPolicy.VN_ZONE).toLocalDateTime()
        val close = closingTime(now.toLocalDate())
        if (close != null && !now.toLocalTime().isBefore(OPEN) && now.toLocalTime().isBefore(close)) {
            return OpenStatus.Open(close)
        }
        var day = if (close != null && now.toLocalTime().isBefore(OPEN)) now.toLocalDate() else now.toLocalDate().plusDays(1)
        while (closingTime(day) == null) day = day.plusDays(1)
        return OpenStatus.Closed(day.atTime(OPEN))
    }

    private fun closingTime(day: LocalDate): LocalTime? = when (day.dayOfWeek) {
        DayOfWeek.SUNDAY -> null
        DayOfWeek.SATURDAY -> SATURDAY_CLOSE
        else -> WEEKDAY_CLOSE
    }
}
