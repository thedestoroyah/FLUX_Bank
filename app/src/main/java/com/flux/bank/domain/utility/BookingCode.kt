package com.flux.bank.domain.utility

import java.security.SecureRandom
import java.util.Random

/** Mã đặt chỗ in trên biên lai vé/khách sạn/đơn hàng: 6 ký tự, bỏ `0 O 1 I` cho dễ đọc. */
object BookingCode {
    const val LENGTH = 6
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(random: Random = SecureRandom()): String =
        (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
}
