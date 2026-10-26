package com.flux.bank.core.security

import java.security.SecureRandom

/** Kết quả kiểm tra một mã OTP. */
enum class OtpResult { VALID, WRONG, EXPIRED, LOCKED, NO_ACTIVE }

/**
 * Sinh và kiểm tra mã OTP 6 số cho đúng một giao dịch tại một thời điểm.
 *
 * Quy tắc: hiệu lực [VALIDITY_MS], dùng một lần, gắn với `transactionId`,
 * sai quá [MAX_ATTEMPTS] lần thì khoá cho tới khi phát mã mới.
 *
 * @param now nguồn thời gian (ms), tách ra để test được hết hạn.
 */
class OtpManager(private val now: () -> Long = System::currentTimeMillis) {

    private val random = SecureRandom()
    private var activeTransactionId: String? = null
    private var activeCode: String? = null
    private var expiresAt = 0L
    private var wrongAttempts = 0

    /** Phát mã mới cho [transactionId]; mã cũ (nếu có) mất hiệu lực. Trả về mã 6 số. */
    fun issue(transactionId: String): String {
        val code = "%06d".format(random.nextInt(1_000_000))
        activeTransactionId = transactionId
        activeCode = code
        expiresAt = now() + VALIDITY_MS
        wrongAttempts = 0
        return code
    }

    /** Kiểm tra [code] cho [transactionId]. Mã đúng sẽ bị huỷ ngay (dùng một lần). */
    fun verify(transactionId: String, code: String): OtpResult {
        val expected = activeCode ?: return OtpResult.NO_ACTIVE
        if (wrongAttempts >= MAX_ATTEMPTS) return OtpResult.LOCKED
        if (now() > expiresAt) {
            clear()
            return OtpResult.EXPIRED
        }
        // Sai giao dịch cũng tính là một lần nhập sai
        if (transactionId != activeTransactionId || code.trim() != expected) {
            wrongAttempts++
            return OtpResult.WRONG
        }
        clear()
        return OtpResult.VALID
    }

    fun remainingSeconds(): Long = if (activeCode == null) 0L else ((expiresAt - now()) / 1000L).coerceAtLeast(0L)

    fun attemptsLeft(): Int = (MAX_ATTEMPTS - wrongAttempts).coerceAtLeast(0)

    fun clear() {
        activeTransactionId = null
        activeCode = null
        expiresAt = 0L
        wrongAttempts = 0
    }

    companion object {
        const val VALIDITY_MS = 120_000L
        const val MAX_ATTEMPTS = 5
    }
}
