package com.flux.bank.core.security

import android.app.Activity
import android.os.Bundle
import com.flux.bank.FluxApp

/**
 * Activity trong suốt nhận cú bấm vào notification OTP: chuyển mã cho sheet đang mở rồi đóng ngay,
 * đưa task của app lên trước (Android 12+ cấm dùng BroadcastReceiver làm trampoline).
 */
class OtpAutofillActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(OtpNotifier.EXTRA_OTP_CODE)?.let { FluxApp.instance.otpNotifier.requestAutofill(it) }
        finish()
    }
}
