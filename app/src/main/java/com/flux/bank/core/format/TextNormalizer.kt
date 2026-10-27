package com.flux.bank.core.format

import java.text.Normalizer

/** Chuẩn hoá chữ tiếng Việt cho tìm kiếm và tên chủ tài khoản. */
object TextNormalizer {
    private val MARKS = Regex("\\p{Mn}+")
    private val SPACES = Regex("\\s+")

    /** Bỏ dấu tiếng Việt, giữ hoa/thường: "Đặng Thu" -> "Dang Thu" (NFD không tách chữ đ nên đổi riêng). */
    fun stripDiacritics(text: String): String =
        MARKS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").replace('đ', 'd').replace('Đ', 'D')

    /** Tên hiển thị kiểu ngân hàng: in hoa, không dấu, một dấu cách giữa các từ ("TRAN THI BINH"). */
    fun holderName(fullName: String): String =
        stripDiacritics(fullName).trim().split(SPACES).joinToString(" ").uppercase()
}
