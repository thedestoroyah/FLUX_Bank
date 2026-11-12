package com.flux.bank.domain.utility

import com.flux.bank.core.validation.Validators
import com.flux.bank.data.model.DetailRow
import com.flux.bank.data.model.ServicePayment
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Dựng [ServicePayment] cho từng loại tiện ích. Chữ ở đây là dữ liệu lưu vào giao dịch
 * (`description`, `details`) nên viết thẳng trong code, giống "… chuyen tien" của F4.
 */
object ServicePayments {
    private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val PERIOD: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/yyyy")

    fun bill(bill: Bill): ServicePayment {
        val electricity = bill.provider.kind == BillKind.ELECTRICITY
        val period = bill.period.format(PERIOD)
        return ServicePayment(
            service = if (electricity) "TIEN_DIEN" else "TIEN_NUOC",
            amount = bill.amount,
            merchant = bill.provider.displayName,
            reference = bill.customerCode,
            description = "${if (electricity) "Tiền điện" else "Tiền nước"} kỳ $period",
            paymentRef = bill.key,
            details = listOf(
                DetailRow("Nhà cung cấp", bill.provider.displayName),
                DetailRow("Mã khách hàng", bill.customerCode),
                DetailRow("Chủ hộ", bill.holderName),
                DetailRow("Kỳ", period),
                DetailRow("Tiêu thụ", "${bill.usage} ${if (electricity) "kWh" else "m³"}")
            )
        )
    }

    fun topUp(phone: String, carrier: Carrier, amount: Long): ServicePayment {
        val digits = Validators.normalizePhone(phone)
        val shown = formatPhone(digits)
        return ServicePayment(
            service = "NAP_DT", amount = amount, merchant = carrier.displayName, reference = digits,
            description = "Nạp ĐT $shown", paymentRef = null,
            details = listOf(DetailRow("Nhà mạng", carrier.displayName), DetailRow("Số điện thoại", shown))
        )
    }

    /** [option] là suất chiếu (phim); [date] là ngày bay / nhận phòng; [code] là mã đặt chỗ ghi vào `paymentRef`. */
    fun booking(kind: CatalogKind, item: CatalogItem, quantity: Int, option: String?, date: LocalDate?, code: String): ServicePayment {
        val (prefix, details) = when (kind) {
            CatalogKind.FLIGHT -> "Vé máy bay" to listOf(
                DetailRow("Chuyến bay", "${item.reference} · ${ServiceCatalog.filterLabel(kind, item.filterId)}"),
                DetailRow("Giờ bay", item.title),
                DetailRow("Ngày bay", "${date?.format(DATE).orEmpty()} · $quantity khách")
            )
            CatalogKind.MOVIE -> "Vé phim" to listOf(
                DetailRow("Rạp", item.merchant), DetailRow("Phim", item.title),
                DetailRow("Suất chiếu", option.orEmpty()), DetailRow("Số vé", quantity.toString())
            )
            CatalogKind.HOTEL -> "Khách sạn" to listOf(
                DetailRow("Khách sạn", item.title), DetailRow("Loại phòng", item.subtitle),
                DetailRow("Nhận phòng", date?.format(DATE).orEmpty()),
                DetailRow("Trả phòng", date?.plusDays(quantity.toLong())?.format(DATE).orEmpty()),
                DetailRow("Số đêm", quantity.toString())
            )
            CatalogKind.SHOP -> "Mua sắm" to listOf(DetailRow("Sản phẩm", item.title), DetailRow("Số lượng", quantity.toString()))
        }
        return ServicePayment(
            service = kind.service, amount = ServiceCatalog.total(item, quantity), merchant = item.merchant,
            reference = item.reference, description = "$prefix ${item.reference}", paymentRef = code, details = details
        )
    }

    /** "0912345678" → "0912 345 678". */
    fun formatPhone(digits: String): String =
        if (digits.length == PHONE_LENGTH) "${digits.take(4)} ${digits.substring(4, 7)} ${digits.substring(7)}" else digits

    private const val PHONE_LENGTH = 10
}
