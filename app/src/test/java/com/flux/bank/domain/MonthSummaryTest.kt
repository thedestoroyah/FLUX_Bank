package com.flux.bank.domain

import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class MonthSummaryTest {

    private val vn = ZoneId.of("Asia/Ho_Chi_Minh")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime.of(y, m, d, h, min).atZone(vn).toInstant().toEpochMilli()

    @Test
    fun countsOnlyThatMonthInVietnamTime() {
        val all = listOf(
            Transaction(id = "late", type = TransactionType.PAYMENT, amount = 2_434_700, createdAt = at(2026, 11, 30, 23, 30)),
            Transaction(id = "in", type = TransactionType.TRANSFER_IN, amount = 500_000, createdAt = at(2026, 11, 10, 14, 31)),
            Transaction(id = "dec", type = TransactionType.WITHDRAW, amount = 100_000, createdAt = at(2026, 12, 1, 0, 5)),
            Transaction(id = "oct", type = TransactionType.DEPOSIT, amount = 1_000_000, createdAt = at(2026, 10, 31, 23, 59))
        )

        val s = MonthSummary.of(all, YearMonth.of(2026, 11))

        assertEquals(500_000L, s.incoming)
        assertEquals(1, s.incomingCount)
        assertEquals(2_434_700L, s.outgoing)
        assertEquals(1, s.outgoingCount)
        assertEquals(listOf("late", "in"), s.transactions.map { it.id })
    }

    @Test
    fun emptyMonth() {
        val s = MonthSummary.of(emptyList(), YearMonth.of(2026, 11))

        assertEquals(0L, s.incoming)
        assertEquals(0, s.incomingCount)
        assertEquals(0L, s.outgoing)
        assertEquals(0, s.outgoingCount)
        assertTrue(s.transactions.isEmpty())
    }
}
