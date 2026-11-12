package com.flux.bank.domain.utility

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.TimeZone

class ServiceCatalogTest {

    private lateinit var originalZone: TimeZone

    @Before fun utc() { originalZone = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("UTC")) }
    @After fun restore() = TimeZone.setDefault(originalZone)

    private fun at(iso: String) = ZonedDateTime.parse(iso).toInstant().toEpochMilli()

    @Test
    fun everyKindHasFiltersWithItems() = CatalogKind.entries.forEach { kind ->
        val filters = ServiceCatalog.filters(kind)
        assertTrue("$kind", filters.size >= 2)
        filters.forEach { f -> assertTrue("$kind/${f.id}", ServiceCatalog.items(kind, f.id).size >= 2) }
    }

    @Test
    fun shopHasAllFilterAndEightProducts() {
        assertEquals(listOf("all", "electronics", "home", "fashion"), ServiceCatalog.filters(CatalogKind.SHOP).map { it.id })
        assertTrue(ServiceCatalog.items(CatalogKind.SHOP, "all").size >= 8)
        assertTrue(ServiceCatalog.items(CatalogKind.SHOP, "home").all { it.filterId == "home" })
    }

    @Test
    fun itemIdsAreUniqueAcrossCatalog() {
        val ids = CatalogKind.entries.flatMap { k ->
            ServiceCatalog.filters(k).filter { it.id != "all" }.flatMap { ServiceCatalog.items(k, it.id) }
        }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun shopAllIsUnionOfGroups() {
        val groups = ServiceCatalog.filters(CatalogKind.SHOP).filter { it.id != "all" }
            .flatMap { ServiceCatalog.items(CatalogKind.SHOP, it.id) }
        assertEquals(groups.map { it.id }.toSet(), ServiceCatalog.items(CatalogKind.SHOP, "all").map { it.id }.toSet())
    }

    @Test
    fun moviesHaveShowtimesAndPositivePrices() {
        ServiceCatalog.filters(CatalogKind.MOVIE).flatMap { ServiceCatalog.items(CatalogKind.MOVIE, it.id) }
            .forEach { assertTrue(it.title, it.options.size >= 2) }
        CatalogKind.entries.forEach { k ->
            ServiceCatalog.filters(k).flatMap { ServiceCatalog.items(k, it.id) }.forEach { assertTrue(it.unitPrice >= 10_000) }
        }
    }

    @Test
    fun totalIsUnitPriceTimesQuantity() {
        val item = ServiceCatalog.items(CatalogKind.HOTEL, ServiceCatalog.filters(CatalogKind.HOTEL).first().id).first()
        assertEquals(item.unitPrice * 3, ServiceCatalog.total(item, 3))
    }

    @Test
    fun dateRangesUseVietnamDay() {
        // 2026-11-12T20:00Z là 03:00 ngày 13/11 giờ VN
        val now = at("2026-11-12T20:00Z")
        assertEquals(LocalDate.of(2026, 11, 14)..LocalDate.of(2027, 1, 12), ServiceCatalog.dateRange(CatalogKind.FLIGHT, now))
        assertEquals(LocalDate.of(2026, 11, 13)..LocalDate.of(2027, 1, 12), ServiceCatalog.dateRange(CatalogKind.HOTEL, now))
        assertNull(ServiceCatalog.dateRange(CatalogKind.MOVIE, now))
        assertNull(ServiceCatalog.dateRange(CatalogKind.SHOP, now))
    }

    @Test
    fun showtimePastUsesVietnamTime() {
        val now = at("2026-11-12T10:00+07:00")
        assertTrue(ServiceCatalog.isShowtimePast("09:00", now))
        assertFalse(ServiceCatalog.isShowtimePast("21:50", now))
    }

    @Test
    fun kindsMapToUtilityServices() = assertEquals(
        listOf("VE_MAY_BAY", "VE_PHIM", "KHACH_SAN", "MUA_SAM"), CatalogKind.entries.map { it.service }
    )
}
