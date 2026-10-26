package com.flux.bank

import android.app.Application

/**
 * Application của FLUX: nơi khởi tạo và giữ các dịch vụ dùng chung (service locator đơn giản).
 * Các repository, OtpManager... được thêm vào đây ở các task sau.
 */
class FluxApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: FluxApp
            private set
    }
}
