package com.flux.bank

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Bản release: App Check dùng Play Integrity. Chỉ app cài từ Google Play mới lấy được token hợp lệ;
 * bản cài tay bằng adb bị từ chối thì Trợ lý AI rơi về chế độ cơ bản (không crash).
 */
fun installAppCheck(app: FluxApp) {
    Firebase.appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
}
