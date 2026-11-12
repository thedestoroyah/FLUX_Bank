package com.flux.bank.domain.utility

import com.flux.bank.data.model.DetailRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ServicePaymentsTest {

    private val oct = YearMonth.of(2026, 10)
    private val nov20 = LocalDate.of(2026, 11, 20)

    private fun item(filterId: String = "f", reference: String = "REF") =
        CatalogItem(id = "i1", filterId = filterId, title = "Tiêu đề", subtitle = "Phụ", merchant = "Hãng", unitPrice = 1_890_000, reference = reference)

    @Test
    fun billPayment() {
        val bill = Bill(BillProvider.EVN_HCM, "PE01000123456", oct, "NGUYEN VAN AN", "12 Lê Lợi, Q.1", 412, 1_248_600, LocalDate.of(2026, 11, 25))
        val p = ServicePayments.bill(bill)
        assertEquals("TIEN_DIEN", p.service)
        assertEquals(1_248_600L, p.amount)
        assertEquals("EVN TP.HCM", p.merchant)
        assertEquals("PE01000123456", p.reference)
        assertEquals("Tiền điện kỳ 10/2026", p.description)
        assertEquals(bill.key, p.paymentRef)
        assertEquals(
            listOf(
                DetailRow("Nhà cung cấp", "EVN TP.HCM"), DetailRow("Mã khách hàng", "PE01000123456"),
                DetailRow("Chủ hộ", "NGUYEN VAN AN"), DetailRow("Kỳ", "10/2026"), DetailRow("Tiêu thụ", "412 kWh")
            ),
            p.details
        )
    }

    @Test
    fun waterUsesCubicMetres() {
        val bill = Bill(BillProvider.SAWACO, "2210456789", oct, "NGUYEN VAN AN", "12 Lê Lợi, Q.1", 18, 216_000, LocalDate.of(2026, 11, 25))
        val p = ServicePayments.bill(bill)
        assertEquals("TIEN_NUOC", p.service)
        assertEquals("Tiền nước kỳ 10/2026", p.description)
        assertEquals(DetailRow("Tiêu thụ", "18 m³"), p.details.last())
    }

    @Test
    fun topUpPayment() {
        val p = ServicePayments.topUp("0912 345678", Carrier.VINAPHONE, 100_000)
        assertEquals("NAP_DT", p.service)
        assertEquals(100_000L, p.amount)
        assertEquals("Vinaphone", p.merchant)
        assertEquals("0912345678", p.reference)
        assertEquals("Nạp ĐT 0912 345 678", p.description)
        assertNull(p.paymentRef)
        assertEquals(listOf(DetailRow("Nhà mạng", "Vinaphone"), DetailRow("Số điện thoại", "0912 345 678")), p.details)
    }

    @Test
    fun flightBooking() {
        val flight = item(filterId = "SGN-HAN", reference = "VN 213").copy(title = "06:00 → 08:10", merchant = "Vietnam Airlines")
        val p = ServicePayments.booking(CatalogKind.FLIGHT, flight, 2, null, nov20, "K7Q2MX")
        assertEquals("VE_MAY_BAY", p.service)
        assertEquals(3_780_000L, p.amount)
        assertEquals("Vietnam Airlines", p.merchant)
        assertEquals("VN 213", p.reference)
        assertEquals("K7Q2MX", p.paymentRef)
        assertEquals("Vé máy bay VN 213", p.description)
        assertEquals(
            listOf(
                DetailRow("Chuyến bay", "VN 213 · TP.HCM → Hà Nội"), DetailRow("Giờ bay", "06:00 → 08:10"),
                DetailRow("Ngày bay", "20/11/2026 · 2 khách")
            ),
            p.details
        )
    }

    @Test
    fun hotelBookingShowsCheckout() {
        val hotel = item(reference = "Sông Hàn Riverside").copy(title = "Sông Hàn Riverside", subtitle = "Phòng đôi", unitPrice = 1_350_000)
        val p = ServicePayments.booking(CatalogKind.HOTEL, hotel, 2, null, nov20, "ABC234")
        assertEquals(2_700_000L, p.amount)
        assertEquals("Khách sạn Sông Hàn Riverside", p.description)
        assertEquals(
            listOf(
                DetailRow("Khách sạn", "Sông Hàn Riverside"), DetailRow("Loại phòng", "Phòng đôi"),
                DetailRow("Nhận phòng", "20/11/2026"), DetailRow("Trả phòng", "22/11/2026"), DetailRow("Số đêm", "2")
            ),
            p.details
        )
    }

    @Test
    fun movieBooking() {
        val movie = item(reference = "Mùa Gió Chướng").copy(title = "Mùa Gió Chướng", merchant = "CGV Vincom Đồng Khởi", unitPrice = 95_000)
        val p = ServicePayments.booking(CatalogKind.MOVIE, movie, 2, "19:45", null, "ABC234")
        assertEquals(190_000L, p.amount)
        assertEquals("Vé phim Mùa Gió Chướng", p.description)
        assertEquals(
            listOf(DetailRow("Rạp", "CGV Vincom Đồng Khởi"), DetailRow("Phim", "Mùa Gió Chướng"), DetailRow("Suất chiếu", "19:45"), DetailRow("Số vé", "2")),
            p.details
        )
    }

    @Test
    fun shopBooking() {
        val product = item(reference = "Tai nghe không dây").copy(title = "Tai nghe không dây", merchant = "FLUX Shop", unitPrice = 1_490_000)
        val p = ServicePayments.booking(CatalogKind.SHOP, product, 1, null, null, "ABC234")
        assertEquals("MUA_SAM", p.service)
        assertEquals("FLUX Shop", p.merchant)
        assertEquals("Mua sắm Tai nghe không dây", p.description)
        assertEquals(listOf(DetailRow("Sản phẩm", "Tai nghe không dây"), DetailRow("Số lượng", "1")), p.details)
    }
}
