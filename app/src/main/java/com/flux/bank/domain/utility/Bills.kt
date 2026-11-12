package com.flux.bank.domain.utility

import com.flux.bank.domain.TransferPolicy
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Random

/** Loại hoá đơn: điện hoặc nước. */
enum class BillKind { ELECTRICITY, WATER }

/** Nhà cung cấp (giả lập). Tên enum nằm trong khoá hoá đơn và id giao dịch nên không được đổi. */
enum class BillProvider(val displayName: String, val kind: BillKind) {
    EVN_HCM("EVN TP.HCM", BillKind.ELECTRICITY),
    EVN_HN("EVN Hà Nội", BillKind.ELECTRICITY),
    EVN_SPC("EVN miền Nam", BillKind.ELECTRICITY),
    SAWACO("SAWACO", BillKind.WATER),
    HAWACO("Nước sạch Hà Nội", BillKind.WATER);

    companion object {
        fun forKind(kind: BillKind): List<BillProvider> = entries.filter { it.kind == kind }
    }
}

/** Hoá đơn một kỳ. [usage] là kWh (điện) hoặc m³ (nước). [key] định danh hoá đơn: nhà cung cấp + mã + kỳ. */
data class Bill(
    val provider: BillProvider,
    val customerCode: String,
    val period: YearMonth,
    val holderName: String,
    val address: String,
    val usage: Int,
    val amount: Long,
    val dueDate: LocalDate
) {
    val key: String get() = "${provider.name}:$customerCode:${period.format(BillLookup.PERIOD_KEY)}"
}

/**
 * Tra hoá đơn giả lập (spec F5 §4): cùng (nhà cung cấp, mã, kỳ) luôn ra cùng hoá đơn, để tra lại vẫn đúng số tiền.
 * Mã có 4 số cuối là `0000` coi như không tồn tại (để demo trường hợp lỗi).
 */
object BillLookup {
    val PERIOD_KEY: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM")
    private const val DUE_DAY = 25
    private const val NOT_FOUND_SUFFIX = "0000"

    private val ELECTRICITY_CODE = Regex("^PE\\d{11}$")
    private val WATER_CODE = Regex("^\\d{8,12}$")

    // Khoảng tiền và đơn giá cố định (₫), chỉ để số liệu trông hợp lý
    private val ELECTRICITY_RANGE = 150_000L..2_500_000L
    private val WATER_RANGE = 50_000L..600_000L
    private const val ELECTRICITY_UNIT_PRICE = 3_000L
    private const val WATER_UNIT_PRICE = 12_000L
    private const val ROUND_TO = 100L

    private val HOLDERS = listOf("NGUYEN VAN AN", "TRAN THI BINH", "LE VAN CUONG", "PHAM MINH DUC", "HOANG THI LAN", "VO QUOC HUY")
    private val ADDRESSES = listOf(
        "12 Lê Lợi, Q.1", "45 Nguyễn Huệ, Q.1", "8 Trần Hưng Đạo, Q.5",
        "102 Cách Mạng Tháng 8, Q.3", "27 Phan Đình Phùng, Ba Đình", "66 Bạch Đằng, Hải Châu"
    )

    /** Mã đã chuẩn hoá (bỏ khoảng trắng, chữ hoa) nếu đúng định dạng của [kind], ngược lại null. */
    fun normalizeCode(kind: BillKind, input: String): String? {
        val code = input.filterNot(Char::isWhitespace).uppercase()
        val pattern = if (kind == BillKind.ELECTRICITY) ELECTRICITY_CODE else WATER_CODE
        return code.takeIf(pattern::matches)
    }

    /** Kỳ hoá đơn = tháng trước của ngày [now] theo giờ Việt Nam. */
    fun periodFor(now: Long): YearMonth =
        YearMonth.from(Instant.ofEpochMilli(now).atZone(TransferPolicy.VN_ZONE)).minusMonths(1)

    /** Hoá đơn của [customerCode] (đã chuẩn hoá) ở kỳ [period]; null nếu mã "không tồn tại". */
    fun lookup(provider: BillProvider, customerCode: String, period: YearMonth): Bill? {
        if (customerCode.endsWith(NOT_FOUND_SUFFIX)) return null
        val random = Random("${provider.name}:$customerCode:$period".hashCode().toLong())
        val electricity = provider.kind == BillKind.ELECTRICITY
        val range = if (electricity) ELECTRICITY_RANGE else WATER_RANGE
        val raw = range.first + (random.nextDouble() * (range.last - range.first)).toLong()
        val amount = raw / ROUND_TO * ROUND_TO
        val unitPrice = if (electricity) ELECTRICITY_UNIT_PRICE else WATER_UNIT_PRICE
        return Bill(
            provider = provider,
            customerCode = customerCode,
            period = period,
            holderName = HOLDERS[random.nextInt(HOLDERS.size)],
            address = ADDRESSES[random.nextInt(ADDRESSES.size)],
            usage = maxOf(1L, amount / unitPrice).toInt(),
            amount = amount,
            dueDate = period.plusMonths(1).atDay(DUE_DAY)
        )
    }

    /** Id giao dịch cố định cho hoá đơn của [uid]: ghi lần hai cùng id bị rules chặn nên không trả trùng. */
    fun txIdFor(uid: String, bill: Bill): String =
        "bill_${uid}_${bill.provider.name}_${bill.customerCode}_${bill.period.format(PERIOD_KEY)}"
}
