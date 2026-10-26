package com.flux.bank.core.security

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.ui.auth.UnlockActivity
import com.flux.bank.utils.BiometricHelper
import com.flux.bank.utils.SessionManager

/**
 * Theo dõi cả tiến trình app (ProcessLifecycleOwner): ghi lại lúc xuống nền, và khi quay lại
 * nếu [LockPolicy] yêu cầu thì phủ [UnlockActivity] lên màn hình đang dùng.
 */
class AppLockObserver(
    private val context: Context,
    private val policy: LockPolicy,
    private val auth: AuthRepository,
    private val session: SessionManager,
    private val now: () -> Long = System::currentTimeMillis
) : DefaultLifecycleObserver {

    private var backgroundedAt: Long? = null

    /** Màn khoá đang hiện thì không mở thêm một màn khoá nữa. */
    var lockScreenVisible = false

    override fun onStop(owner: LifecycleOwner) {
        backgroundedAt = now()
    }

    override fun onStart(owner: LifecycleOwner) {
        val uid = auth.currentUid()
        val lock = !lockScreenVisible && uid != null && policy.onForeground(
            backgroundedAtMs = backgroundedAt,
            nowMs = now(),
            hasSession = true,
            biometricEnabled = session.isBiometricEnabled(uid),
            biometricAvailable = BiometricHelper.canAuthenticate(context)
        )
        if (lock) {
            context.startActivity(
                UnlockActivity.intent(context, UnlockActivity.Mode.RESUME)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun onUnlocked() {
        backgroundedAt = null
    }
}
