package com.flux.bank.domain

import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime
import java.util.TimeZone

class TransferPolicyTest {

    private lateinit var originalZone: TimeZone

    // Máy chạy test để múi giờ UTC: hạn mức ngày vẫn phải tính theo ngày Việt Nam
    @Before fun utc() { originalZone = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("UTC")) }
    @After fun restore() = TimeZone.setDefault(originalZone)

    private fun at(iso: String) = ZonedDateTime.parse(iso).toInstant().toEpochMilli()
    private fun tx(type: TransactionType, amount: Long, at: Long) = Transaction(type = type, amount = amount, createdAt = at)

    @Test
    fun belowMinFirst() = assertEquals(TransferError.BELOW_MIN, TransferPolicy.checkAmount(9_999, 0, 0))

    @Test
    fun aboveMax() = assertEquals(TransferError.ABOVE_MAX, TransferPolicy.checkAmount(100_000_001, 500_000_000, 200_000_000))

    @Test
    fun overBalance() = assertEquals(TransferError.INSUFFICIENT_FUNDS, TransferPolicy.checkAmount(500_001, 500_000, 200_000_000))

    @Test
    fun overDaily() {
        assertEquals(TransferError.DAILY_LIMIT, TransferPolicy.checkAmount(60_000_000, 500_000_000, 50_000_000))
        assertNull(TransferPolicy.checkAmount(50_000_000, 500_000_000, 50_000_000))
    }

    @Test
    fun sentTodayCountsOnlyTransferOut() {
        val now = at("2026-10-27T09:30+07:00")
        val history = listOf(
            tx(TransactionType.TRANSFER_OUT, 150_000_000, at("2026-10-27T08:00+07:00")),
            tx(TransactionType.WITHDRAW, 5_000_000, at("2026-10-27T08:10+07:00")),
            tx(TransactionType.TRANSFER_IN, 9_000_000, at("2026-10-27T08:20+07:00"))
        )
        assertEquals(150_000_000L, TransferPolicy.sentToday(history, now))
        assertEquals(50_000_000L, TransferPolicy.remainingToday(history, now))
    }

    @Test
    fun dayBoundaryUsesVietnamTime() {
        val now = at("2026-10-28T00:01+07:00")
        val history = listOf(
            tx(TransactionType.TRANSFER_OUT, 1_000_000, at("2026-10-27T23:59+07:00")),
            tx(TransactionType.TRANSFER_OUT, 2_000_000, at("2026-10-28T00:00:30+07:00"))
        )
        assertEquals(2_000_000L, TransferPolicy.sentToday(history, now))
    }

    @Test
    fun accountNumberFormats() {
        assertTrue(TransferPolicy.isFluxAccountNumber("886812345678"))
        assertFalse(TransferPolicy.isFluxAccountNumber("886712345678"))
        assertFalse(TransferPolicy.isFluxAccountNumber("88681234567"))
        assertTrue(TransferPolicy.isExternalAccountNumber("007100041234"))
        assertFalse(TransferPolicy.isExternalAccountNumber("12345"))
        assertFalse(TransferPolicy.isExternalAccountNumber("12345678901234567890"))
    }

    @Test
    fun paymentsCountTowardDailyLimit() {
        val now = at("2026-11-12T10:00+07:00")
        val history = listOf(
            tx(TransactionType.TRANSFER_OUT, 50_000_000, now),
            tx(TransactionType.PAYMENT, 30_000_000, now),
            tx(TransactionType.WITHDRAW, 9_000_000, now)
        )
        assertEquals(80_000_000, TransferPolicy.sentToday(history, now))
    }
}
