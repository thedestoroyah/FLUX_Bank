package com.flux.bank.domain

/** Quy tắc khi nào giao dịch cần thêm bước xác thực khuôn mặt (ngoài OTP). */
object FaceVerificationPolicy {

    /** Giao dịch từ 10.000.000 ₫ trở lên là "giá trị cao". */
    const val HIGH_VALUE_THRESHOLD = 10_000_000L

    /** [amount] null nghĩa là thao tác không có số tiền (ví dụ đổi SĐT) — chỉ cần OTP. */
    fun requiresFace(amount: Long?): Boolean = amount != null && amount >= HIGH_VALUE_THRESHOLD
}
