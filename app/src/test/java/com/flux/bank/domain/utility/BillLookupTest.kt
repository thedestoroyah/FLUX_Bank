package com.flux.bank.domain.utility

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.Random
import java.util.TimeZone

class BillLookupTest {

    private lateinit var originalZone: TimeZone

    // Máy chạy test để UTC: kỳ hoá đơn vẫn phải tính theo giờ Việt Nam
    @Before fun utc() { originalZone = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("UTC")) }
    @After fun restore() = TimeZone.setDefault(originalZone)

    private val oct = YearMonth.of(2026, 10)

    @Test
    fun electricityCodeIsPePlus11Digits() {
        assertEquals("PE01000123456", BillLookup.normalizeCode(BillKind.ELECTRICITY, " pe01000123456 "))
        assertNull(BillLookup.normalizeCode(BillKind.ELECTRICITY, "PE0100012345"))
        assertNull(BillLookup.normalizeCode(BillKind.ELECTRICITY, "01000123456"))
    }

    @Test
    fun waterCodeIs8To12Digits() {
        assertEquals("2210456789", BillLookup.normalizeCode(BillKind.WATER, "2210 456 789"))
        assertNull(BillLookup.normalizeCode(BillKind.WATER, "1234567"))
        assertNull(BillLookup.normalizeCode(BillKind.WATER, "1234567890123"))
        assertNull(BillLookup.normalizeCode(BillKind.WATER, "PE01000123456"))
    }

    @Test
    fun lookupIsStable() = assertEquals(
        BillLookup.lookup(BillProvider.EVN_HCM, "PE01000123456", oct),
        BillLookup.lookup(BillProvider.EVN_HCM, "PE01000123456", oct)
    )

    @Test
    fun differentCodesGiveDifferentBills() {
        val amounts = (1..20).map { BillLookup.lookup(BillProvider.EVN_HCM, "PE0100012%04d".format(it), oct)!!.amount }.toSet()
        assertTrue(amounts.size > 10)
    }

    @Test
    fun amountsStayInRangeAndRoundTo100() {
        val random = Random(7)
        repeat(200) {
            val digits = (1..11).joinToString("") { random.nextInt(10).toString() }.replaceRange(7, 11, "%04d".format(1 + random.nextInt(9998)))
            val power = BillLookup.lookup(BillProvider.EVN_HN, "PE$digits", oct)!!
            assertTrue(power.amount in 150_000L..2_500_000L)
            assertEquals(0L, power.amount % 100)
            val water = BillLookup.lookup(BillProvider.SAWACO, digits, oct)!!
            assertTrue(water.amount in 50_000L..600_000L)
            assertEquals(0L, water.amount % 100)
            assertTrue(power.usage > 0 && water.usage > 0)
        }
    }

    @Test
    fun codeEndingIn0000IsNotFound() {
        assertNull(BillLookup.lookup(BillProvider.SAWACO, "22100000", oct))
        assertNull(BillLookup.lookup(BillProvider.EVN_HCM, "PE01000120000", oct))
    }

    @Test
    fun periodUsesVietnamTime() {
        // 2026-10-31T18:00Z là 01:00 ngày 1/11 giờ VN → kỳ = 10/2026
        assertEquals(oct, BillLookup.periodFor(Instant.parse("2026-10-31T18:00:00Z").toEpochMilli()))
        // 2026-10-31T16:00Z là 23:00 ngày 31/10 giờ VN → kỳ = 9/2026
        assertEquals(YearMonth.of(2026, 9), BillLookup.periodFor(Instant.parse("2026-10-31T16:00:00Z").toEpochMilli()))
    }

    @Test
    fun dueDateIs25thOfNextMonth() =
        assertEquals(LocalDate.of(2026, 11, 25), BillLookup.lookup(BillProvider.EVN_HN, "PE01000123456", oct)!!.dueDate)

    @Test
    fun keyAndTxIdEncodeProviderCodeAndPeriod() {
        val bill = BillLookup.lookup(BillProvider.EVN_HCM, "PE01000123456", oct)
        assertNotNull(bill)
        assertEquals("EVN_HCM:PE01000123456:202610", bill!!.key)
        assertEquals("bill_c1_EVN_HCM_PE01000123456_202610", BillLookup.txIdFor("c1", bill))
    }

    @Test
    fun providersSplitByKind() {
        assertEquals(listOf(BillProvider.EVN_HCM, BillProvider.EVN_HN, BillProvider.EVN_SPC), BillProvider.forKind(BillKind.ELECTRICITY))
        assertEquals(listOf(BillProvider.SAWACO, BillProvider.HAWACO), BillProvider.forKind(BillKind.WATER))
    }
}
