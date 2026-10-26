package com.flux.bank.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "flux_bank_session"
        private const val KEY_UID = "saved_uid"
        private const val KEY_EMAIL = "saved_email"
        private const val KEY_FULL_NAME = "saved_full_name"
        private const val KEY_ROLE = "saved_role"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }

    fun saveUserSession(uid: String, email: String, role: String, fullName: String) {
        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROLE, role)
            .putString(KEY_FULL_NAME, fullName)
            .apply()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun getSavedUid(): String? = prefs.getString(KEY_UID, null)
    fun getSavedEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getSavedRole(): String? = prefs.getString(KEY_ROLE, null)
    fun getSavedFullName(): String? = prefs.getString(KEY_FULL_NAME, null)

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}

