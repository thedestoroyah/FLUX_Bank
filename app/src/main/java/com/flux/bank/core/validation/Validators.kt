package com.flux.bank.core.validation

/** Kiểm tra dữ liệu nhập; thuần Kotlin để unit test được trên JVM. */
object Validators {

    // Không dùng android.util.Patterns để test chạy được trên JVM
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val VN_PHONE = Regex("^0\\d{9}$")
    private val NATIONAL_ID = Regex("^\\d{12}$")
    private const val MIN_PASSWORD_LENGTH = 8

    fun isValidEmail(s: String): Boolean = EMAIL.matches(s.trim())

    /** Bỏ mọi khoảng trắng: " 0912 345 678 " -> "0912345678". */
    fun normalizePhone(s: String): String = s.filterNot { it.isWhitespace() }

    /** SĐT Việt Nam: 10 số, bắt đầu bằng 0 (sau khi normalize). */
    fun isValidVnPhone(s: String): Boolean = VN_PHONE.matches(normalizePhone(s))

    /** CCCD: đúng 12 chữ số. */
    fun isValidNationalId(s: String): Boolean = NATIONAL_ID.matches(s.filterNot { it.isWhitespace() })

    /** Mật khẩu tối thiểu 8 ký tự, có cả chữ và số. */
    fun isStrongEnoughPassword(s: String): Boolean =
        s.length >= MIN_PASSWORD_LENGTH && s.any { it.isLetter() } && s.any { it.isDigit() }
}
