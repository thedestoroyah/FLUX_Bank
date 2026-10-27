package com.flux.bank.core.format

import kotlin.math.abs

/** Định dạng số tiền VND theo kiểu Việt Nam: dấu chấm ngăn cách hàng nghìn. */
object MoneyFormatter {

    private const val MINUS = '−'

    /** 48250000 -> "48.250.000 ₫". Số âm có dấu trừ ở đầu. */
    fun formatVnd(amount: Long): String {
        val sign = if (amount < 0) MINUS.toString() else ""
        return "$sign${group(abs(amount))} ₫"
    }

    /** Dùng cho dòng giao dịch: 2000000 -> "+2.000.000", -55000 -> "−55.000" (không kèm ký hiệu ₫). */
    fun formatSignedVnd(amount: Long): String {
        val sign = if (amount < 0) MINUS else '+'
        return "$sign${group(abs(amount))}"
    }

    /** Lãi suất %/năm kiểu Việt Nam, bỏ số 0 thừa: 4.5 -> "4,5", 5.0 -> "5", 8.25 -> "8,25". */
    fun formatRate(rate: Double): String =
        "%.2f".format(java.util.Locale.ROOT, rate).trimEnd('0').trimEnd('.').replace('.', ',')

    private fun group(value: Long): String =
        value.toString().reversed().chunked(3).joinToString(".").reversed()
}
