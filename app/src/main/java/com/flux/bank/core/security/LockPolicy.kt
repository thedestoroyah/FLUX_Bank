package com.flux.bank.core.security

/**
 * Quy tắc khi nào app tự khoá và đòi sinh trắc học.
 * Chỉ khoá khi có phiên Firebase, người dùng đã bật sinh trắc học và máy hỗ trợ — để không ai bị khoá ngoài.
 *
 * @param timeoutMs thời gian ở nền tối thiểu (ms) trước khi khoá lại khi quay về app.
 */
class LockPolicy(val timeoutMs: Long = 60_000L) {

    /** Khi mở app từ đầu (cold start). */
    fun onColdStart(hasSession: Boolean, biometricEnabled: Boolean, biometricAvailable: Boolean): Boolean =
        hasSession && biometricEnabled && biometricAvailable

    /** Khi app quay lại foreground; [backgroundedAtMs] null nghĩa là chưa từng xuống nền. */
    fun onForeground(
        backgroundedAtMs: Long?,
        nowMs: Long,
        hasSession: Boolean,
        biometricEnabled: Boolean,
        biometricAvailable: Boolean
    ): Boolean {
        if (backgroundedAtMs == null) return false
        return onColdStart(hasSession, biometricEnabled, biometricAvailable) && nowMs - backgroundedAtMs >= timeoutMs
    }
}
