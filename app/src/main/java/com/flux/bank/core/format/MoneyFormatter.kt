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

    private fun group(value: Long): String =
        value.toString().reversed().chunked(3).joinToString(".").reversed()
}
