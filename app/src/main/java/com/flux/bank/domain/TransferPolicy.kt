package com.flux.bank.domain

import com.flux.bank.core.validation.Validators
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import java.time.Instant
import java.time.ZoneId

/** Lý do một lần chuyển tiền không hợp lệ (hiện ngay dưới ô tương ứng). */
enum class TransferError {
    BELOW_MIN, ABOVE_MAX, INSUFFICIENT_FUNDS, DAILY_LIMIT, SAME_ACCOUNT,
    INVALID_ACCOUNT_NUMBER, PAYEE_NOT_FOUND, NO_BANK, NO_NAME
}

/** Kiểm tra chuyển tiền trước khi ghi: hạn mức mỗi lần, mỗi ngày (giờ Việt Nam), số dư, định dạng số tài khoản. */
object TransferPolicy {
    const val MAX_PER_TRANSFER = 100_000_000L
    const val DAILY_LIMIT = 200_000_000L
    const val MAX_DESCRIPTION = 100
    val VN_ZONE: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    private val FLUX_NUMBER = Regex("^8868\\d{8}$")
    private val EXTERNAL_NUMBER = Regex("^\\d{6,19}$")

    /**
     * Tổng tiền chuyển đi và thanh toán dịch vụ (TRANSFER_OUT + PAYMENT, chung một hạn mức) trong cùng ngày Việt Nam với [now].
     * shortcut: chỉ cộng trong 100 giao dịch mới nhất (listRecent), nâng cấp: query theo ngày nếu khách giao dịch nhiều hơn.
     */
    fun sentToday(history: List<Transaction>, now: Long): Long {
        val today = dayOf(now)
        return history.filter { (it.type == TransactionType.TRANSFER_OUT || it.type == TransactionType.PAYMENT) && dayOf(it.createdAt) == today }.sumOf { it.amount }
    }

    fun remainingToday(history: List<Transaction>, now: Long): Long = maxOf(0L, DAILY_LIMIT - sentToday(history, now))

    /** Lỗi đầu tiên theo thứ tự: dưới mức tối thiểu, quá mức mỗi lần, quá số dư, quá hạn mức còn lại; null nếu hợp lệ. */
    fun checkAmount(amount: Long, balance: Long, remainingToday: Long): TransferError? = when {
        amount < Validators.MIN_AMOUNT -> TransferError.BELOW_MIN
        amount > MAX_PER_TRANSFER -> TransferError.ABOVE_MAX
        amount > balance -> TransferError.INSUFFICIENT_FUNDS
        amount > remainingToday -> TransferError.DAILY_LIMIT
        else -> null
    }

    fun isFluxAccountNumber(s: String): Boolean = FLUX_NUMBER.matches(s)

    fun isExternalAccountNumber(s: String): Boolean = EXTERNAL_NUMBER.matches(s)

    private fun dayOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(VN_ZONE).toLocalDate()
}
