package com.flux.bank.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.flux.bank.data.repository.AuthRepository

/**
 * Theo dõi cả tiến trình app (ProcessLifecycleOwner): ghi lại lúc xuống nền, và khi quay lại
 * nếu [LockPolicy] yêu cầu thì mở màn khoá ([startUnlock]).
 *
 * Lần lên foreground đầu tiên của tiến trình được coi là cold start, trừ khi LoginActivity/UnlockActivity
 * đã tự xử lý ([markColdStartHandled]). Nhờ vậy khi Android khôi phục thẳng màn chính sau khi kill tiến trình
 * ở nền, app vẫn bị khoá.
 */
class AppLockObserver(
    private val policy: LockPolicy,
    private val auth: AuthRepository,
    private val biometricEnabled: (String) -> Boolean,
    private val biometricAvailable: () -> Boolean,
    private val startUnlock: () -> Unit,
    private val now: () -> Long = System::currentTimeMillis
) : DefaultLifecycleObserver {

    private var backgroundedAt: Long? = null
    private var coldStartHandled = false

    /** Màn khoá đang hiện thì không mở thêm một màn khoá nữa. */
    var lockScreenVisible = false

    /** LoginActivity/UnlockActivity gọi khi chúng đã tự quyết định khoá lúc mở app. */
    fun markColdStartHandled() {
        coldStartHandled = true
    }

    override fun onStop(owner: LifecycleOwner) = onAppBackground()

    override fun onStart(owner: LifecycleOwner) = onAppForeground()

    fun onAppBackground() {
        backgroundedAt = now()
    }

    fun onAppForeground() {
        val firstStart = !coldStartHandled
        coldStartHandled = true
        val uid = auth.currentUid() ?: return
        if (lockScreenVisible) return
        val enabled = biometricEnabled(uid)
        val available = biometricAvailable()
        val lock = if (firstStart) {
            policy.onColdStart(hasSession = true, biometricEnabled = enabled, biometricAvailable = available)
        } else {
            policy.onForeground(backgroundedAt, now(), hasSession = true, biometricEnabled = enabled, biometricAvailable = available)
        }
        if (lock) startUnlock()
    }

    fun onUnlocked() {
        backgroundedAt = null
    }
}
