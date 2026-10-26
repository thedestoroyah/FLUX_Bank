package com.flux.bank.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.flux.bank.R

/** Bọc AndroidX BiometricPrompt (vân tay / khuôn mặt của máy). */
object BiometricHelper {

    private const val AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK

    /** Máy có phần cứng VÀ đã đăng ký ít nhất một sinh trắc học. */
    fun canAuthenticate(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Hiện hộp thoại sinh trắc học của hệ thống.
     * [onError] chỉ được gọi khi hộp thoại đóng hẳn (huỷ, khoá tạm...), không gọi ở mỗi lần quét sai.
     */
    fun prompt(activity: FragmentActivity, onSuccess: () -> Unit, onError: (CharSequence) -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString)
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_title))
            .setSubtitle(activity.getString(R.string.biometric_subtitle))
            .setNegativeButtonText(activity.getString(R.string.biometric_negative))
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info)
    }
}
