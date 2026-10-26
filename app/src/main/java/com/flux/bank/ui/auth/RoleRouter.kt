package com.flux.bank.ui.auth

import android.app.Activity
import android.content.Intent
import com.flux.bank.FluxApp
import com.flux.bank.data.model.User
import com.flux.bank.data.model.UserRole
import com.flux.bank.ui.customer.CustomerMainActivity
import com.flux.bank.ui.officer.OfficerMainActivity

/** Mở màn hình chính đúng vai trò và xoá các màn đăng nhập khỏi back stack. */
object RoleRouter {

    fun start(activity: Activity, user: User) {
        FluxApp.instance.sessionManager.saveDisplayName(user.uid, user.fullName)
        val target = if (user.role == UserRole.BANK_OFFICER) OfficerMainActivity::class.java else CustomerMainActivity::class.java
        val intent = Intent(activity, target)
            .putExtra(EXTRA_USER_UID, user.uid)
            .putExtra(EXTRA_USER_NAME, user.fullName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(intent)
        activity.finish()
    }

    /** Đăng xuất hẳn và quay về màn đăng nhập. */
    fun signOut(activity: Activity) {
        val app = FluxApp.instance
        app.authRepository.currentUid()?.let { app.sessionManager.clearFor(it) }
        app.authRepository.logout()
        activity.startActivity(
            Intent(activity, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        activity.finish()
    }

    const val EXTRA_USER_UID = "extra_user_uid"
    const val EXTRA_USER_NAME = "extra_user_name"
}
