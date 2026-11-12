package com.flux.bank.domain.utility

import com.flux.bank.domain.TransferPolicy
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Bốn loại danh mục dùng chung một màn (spec F5 §4). [service] trùng tên enum `Utility` của ô tiện ích;
 * [quantityRange] là số khách / vé / đêm / sản phẩm cho phép; [needsDate] = cần chọn ngày.
 */
enum class CatalogKind(val service: String, val quantityRange: IntRange, val needsDate: Boolean) {
    FLIGHT("VE_MAY_BAY", 1..9, true),
    MOVIE("VE_PHIM", 1..8, false),
    HOTEL("KHACH_SAN", 1..14, true),
    SHOP("MUA_SAM", 1..10, false)
}

data class CatalogFilter(val id: String, val label: String)

/**
 * Một mục chọn được trong danh mục. [reference] là định danh ghi vào giao dịch và lịch sử
 * (số hiệu chuyến bay; các loại khác = tên). [options] là giờ suất chiếu "HH:mm" (chỉ phim).
 */
data class CatalogItem(
    val id: String,
    val filterId: String,
    val title: String,
    val subtitle: String,
    val merchant: String,
    val unitPrice: Long,
    val options: List<String> = emptyList(),
    val stars: Int = 0,
    val reference: String = title
)

/** Dữ liệu danh mục giả lập nằm sẵn trong app (không đọc Firestore). Tên phim, khách sạn, sản phẩm là tên tự đặt. */
object ServiceCatalog {
    const val SHOP_ALL = "all"
    const val SHOP_MERCHANT = "FLUX Shop"
    private const val BOOKING_WINDOW_DAYS = 60L

    private const val VNA = "Vietnam Airlines"
    private const val VJ = "Vietjet Air"
    private const val QH = "Bamboo Airways"
    private const val CGV = "CGV Vincom Đồng Khởi"
    private const val GALAXY = "Galaxy Nguyễn Du"

    private fun flight(id: String, route: String, times: String, airline: String, number: String, price: Long) =
        CatalogItem(id, route, times, "$airline · $number", airline, price, reference = number)

    private fun movie(id: String, cinemaId: String, cinema: String, title: String, info: String, price: Long, showtimes: List<String>) =
        CatalogItem(id, cinemaId, title, info, cinema, price, options = showtimes)

    private fun hotel(id: String, city: String, name: String, room: String, stars: Int, price: Long) =
        CatalogItem(id, city, name, room, name, price, stars = stars)

    private fun product(id: String, group: String, name: String, price: Long) =
        CatalogItem(id, group, name, "", SHOP_MERCHANT, price)

    private val FILTERS = mapOf(
        CatalogKind.FLIGHT to listOf(
            CatalogFilter("SGN-HAN", "TP.HCM → Hà Nội"), CatalogFilter("SGN-DAD", "TP.HCM → Đà Nẵng"), CatalogFilter("HAN-SGN", "Hà Nội → TP.HCM")
        ),
        CatalogKind.MOVIE to listOf(CatalogFilter("cgv", CGV), CatalogFilter("galaxy", GALAXY)),
        CatalogKind.HOTEL to listOf(CatalogFilter("hcm", "TP.HCM"), CatalogFilter("dad", "Đà Nẵng"), CatalogFilter("han", "Hà Nội")),
        CatalogKind.SHOP to listOf(
            CatalogFilter(SHOP_ALL, "Tất cả"), CatalogFilter("electronics", "Điện tử"), CatalogFilter("home", "Gia dụng"), CatalogFilter("fashion", "Thời trang")
        )
    )

    private val ITEMS = mapOf(
        CatalogKind.FLIGHT to listOf(
            flight("f1", "SGN-HAN", "06:00 → 08:10", VNA, "VN 213", 1_890_000),
            flight("f2", "SGN-HAN", "09:30 → 11:35", VJ, "VJ 125", 1_290_000),
            flight("f3", "SGN-HAN", "14:15 → 16:20", QH, "QH 205", 1_450_000),
            flight("f4", "SGN-DAD", "07:10 → 08:30", VNA, "VN 128", 1_150_000),
            flight("f5", "SGN-DAD", "12:40 → 14:00", VJ, "VJ 622", 790_000),
            flight("f6", "SGN-DAD", "18:20 → 19:40", QH, "QH 152", 920_000),
            flight("f7", "HAN-SGN", "06:30 → 08:40", VNA, "VN 208", 1_950_000),
            flight("f8", "HAN-SGN", "11:00 → 13:05", VJ, "VJ 136", 1_190_000),
            flight("f9", "HAN-SGN", "20:15 → 22:20", QH, "QH 206", 1_390_000)
        ),
        CatalogKind.MOVIE to listOf(
            movie("m1", "cgv", CGV, "Mùa Gió Chướng", "Tâm lý · 118 phút · T13", 95_000, listOf("14:30", "19:45", "21:50")),
            movie("m2", "cgv", CGV, "Biệt Đội Sao Băng", "Hành động · 126 phút · T16", 110_000, listOf("15:00", "18:30", "22:10")),
            movie("m3", "cgv", CGV, "Chú Mèo Phi Hành Gia", "Hoạt hình · 96 phút · P", 85_000, listOf("09:30", "13:15", "16:40")),
            movie("m4", "galaxy", GALAXY, "Mùa Gió Chướng", "Tâm lý · 118 phút · T13", 90_000, listOf("13:00", "17:20", "20:30")),
            movie("m5", "galaxy", GALAXY, "Thành Phố Không Ngủ", "Kinh dị · 104 phút · T18", 100_000, listOf("19:00", "21:15", "23:00"))
        ),
        CatalogKind.HOTEL to listOf(
            hotel("h1", "hcm", "Sài Gòn Central Hotel", "Phòng đôi", 4, 1_250_000),
            hotel("h2", "hcm", "Bến Thành Boutique", "Phòng tiêu chuẩn", 3, 750_000),
            hotel("h3", "dad", "Sông Hàn Riverside", "Phòng đôi", 4, 1_350_000),
            hotel("h4", "dad", "Mỹ Khê Beach Stay", "Phòng đôi", 3, 820_000),
            hotel("h5", "dad", "Sơn Trà Ocean Resort", "Phòng hướng biển", 5, 3_200_000),
            hotel("h6", "han", "Hồ Gươm Heritage", "Phòng đôi", 4, 1_450_000),
            hotel("h7", "han", "Phố Cổ Homestay", "Phòng tiêu chuẩn", 2, 520_000)
        ),
        CatalogKind.SHOP to listOf(
            product("p1", "electronics", "Tai nghe không dây", 1_490_000),
            product("p2", "electronics", "Sạc dự phòng 20.000 mAh", 590_000),
            product("p3", "electronics", "Loa bluetooth mini", 890_000),
            product("p4", "home", "Bình giữ nhiệt 500 ml", 290_000),
            product("p5", "home", "Nồi chiên không dầu 5 lít", 1_890_000),
            product("p6", "home", "Bộ dao nhà bếp 5 món", 450_000),
            product("p7", "fashion", "Áo thun cotton", 199_000),
            product("p8", "fashion", "Balo laptop 15\"", 690_000),
            product("p9", "fashion", "Giày chạy bộ", 1_290_000)
        )
    )

    fun filters(kind: CatalogKind): List<CatalogFilter> = FILTERS.getValue(kind)

    /** Mục thuộc [filterId]; với SHOP, [SHOP_ALL] trả về mọi sản phẩm. */
    fun items(kind: CatalogKind, filterId: String): List<CatalogItem> {
        val all = ITEMS.getValue(kind)
        return if (kind == CatalogKind.SHOP && filterId == SHOP_ALL) all else all.filter { it.filterId == filterId }
    }

    fun filterLabel(kind: CatalogKind, filterId: String): String =
        filters(kind).firstOrNull { it.id == filterId }?.label.orEmpty()

    fun total(item: CatalogItem, quantity: Int): Long = item.unitPrice * quantity

    /** Khoảng ngày chọn được theo giờ VN: bay từ ngày mai, nhận phòng từ hôm nay, tối đa 60 ngày tới; null nếu không cần ngày. */
    fun dateRange(kind: CatalogKind, now: Long): ClosedRange<LocalDate>? {
        val today = today(now)
        return when (kind) {
            CatalogKind.FLIGHT -> today.plusDays(1)..today.plusDays(BOOKING_WINDOW_DAYS)
            CatalogKind.HOTEL -> today..today.plusDays(BOOKING_WINDOW_DAYS)
            else -> null
        }
    }

    /** Suất chiếu hôm nay lúc [time] ("HH:mm") đã qua so với [now] (giờ VN). */
    fun isShowtimePast(time: String, now: Long): Boolean =
        LocalTime.parse(time) <= Instant.ofEpochMilli(now).atZone(TransferPolicy.VN_ZONE).toLocalTime()

    fun today(now: Long): LocalDate = Instant.ofEpochMilli(now).atZone(TransferPolicy.VN_ZONE).toLocalDate()
}
