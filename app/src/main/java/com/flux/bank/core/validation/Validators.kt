package com.flux.bank.core.validation

/** Lý do số tiền nạp/rút không hợp lệ. */
enum class AmountError { BELOW_MIN, ABOVE_DEPOSIT_MAX, INSUFFICIENT_FUNDS }

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

    const val MIN_AMOUNT = 10_000L
    const val MAX_DEPOSIT = 100_000_000L
    const val MAX_AMOUNT_DIGITS = 12
    private const val MAX_RATE = 30.0

    /** Kiểm tra số tiền nạp ([isDeposit]) hoặc rút so với [balance]; null nếu hợp lệ. */
    fun amountError(isDeposit: Boolean, amount: Long, balance: Long): AmountError? = when {
        amount < MIN_AMOUNT -> AmountError.BELOW_MIN
        isDeposit && amount > MAX_DEPOSIT -> AmountError.ABOVE_DEPOSIT_MAX
        !isDeposit && amount > balance -> AmountError.INSUFFICIENT_FUNDS
        else -> null
    }

    /** Lãi suất %/năm, nhận cả dấu phẩy thập phân ("5,0"); null nếu không phải số hoặc ngoài (0, 30]. */
    fun parseRate(input: String): Double? =
        input.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it <= MAX_RATE }

    /** Số tiền người dùng gõ: chỉ giữ chữ số, tối đa 12 chữ số để không tràn Long; rỗng → 0. */
    fun parseAmount(input: String): Long =
        input.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS).toLongOrNull() ?: 0L
}
