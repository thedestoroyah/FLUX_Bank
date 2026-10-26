package com.flux.bank.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Lưu cục bộ những gì cần biết TRƯỚC khi đọc được Firestore: cờ sinh trắc học theo uid (để quyết định khoá app
 * lúc mở), tên hiển thị trên màn mở khoá, và email đăng nhập gần nhất. Phiên đăng nhập thật do Firebase giữ.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isBiometricEnabled(uid: String): Boolean = prefs.getBoolean(KEY_BIOMETRIC + uid, false)

    fun setBiometricEnabled(uid: String, enabled: Boolean) =
        prefs.edit().putBoolean(KEY_BIOMETRIC + uid, enabled).apply()

    fun displayName(uid: String): String? = prefs.getString(KEY_NAME + uid, null)

    fun saveDisplayName(uid: String, name: String) = prefs.edit().putString(KEY_NAME + uid, name).apply()

    fun lastEmail(): String? = prefs.getString(KEY_LAST_EMAIL, null)

    fun saveLastEmail(email: String) = prefs.edit().putString(KEY_LAST_EMAIL, email).apply()

    /** Đăng xuất hẳn: xoá dữ liệu gắn với [uid], giữ lại email gần nhất để điền sẵn. */
    fun clearFor(uid: String) = prefs.edit().remove(KEY_BIOMETRIC + uid).remove(KEY_NAME + uid).apply()

    private companion object {
        const val PREF_NAME = "flux_bank_session"
        const val KEY_BIOMETRIC = "biometric_"
        const val KEY_NAME = "name_"
        const val KEY_LAST_EMAIL = "last_email"
    }
}
