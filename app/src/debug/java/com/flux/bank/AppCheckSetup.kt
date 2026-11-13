package com.flux.bank

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Bản debug: App Check dùng debug provider. Lần chạy đầu in debug token ra Logcat (tag `DebugAppCheckProvider`);
 * đăng ký token đó ở Firebase Console → App Check thì Trợ lý AI vẫn gọi được Gemini khi App Check bị bắt buộc.
 * Bản release có file cùng tên ở `src/release` dùng Play Integrity, nên debug provider không bao giờ vào bản phát hành.
 */
fun installAppCheck(app: FluxApp) {
    Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
