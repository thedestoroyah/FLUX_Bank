package com.flux.bank

import android.app.Application
import com.flux.bank.core.security.OtpManager
import com.flux.bank.core.security.OtpNotifier

/**
 * Application của FLUX: nơi khởi tạo và giữ các dịch vụ dùng chung (service locator đơn giản, không dùng DI).
 */
class FluxApp : Application() {

    val otpManager: OtpManager by lazy { OtpManager() }
    val otpNotifier: OtpNotifier by lazy { OtpNotifier(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        otpNotifier.createChannel()
    }

    companion object {
        lateinit var instance: FluxApp
            private set
    }
}
