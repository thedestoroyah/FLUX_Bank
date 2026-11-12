import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

// Khoá VNPay sandbox đọc từ local.properties (vnpay.tmn.code, vnpay.hash.secret; không commit); thiếu thì để rỗng và app báo "Chưa cấu hình VNPay"
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val RELEASE_SIGNING_KEYS = listOf("release.store.file", "release.store.password", "release.key.alias", "release.key.password")

android {
    namespace = "com.flux.bank"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.flux.bank"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "VNPAY_TMN_CODE", "\"${localProps.getProperty("vnpay.tmn.code", "").trim()}\"")
        buildConfigField("String", "VNPAY_HASH_SECRET", "\"${localProps.getProperty("vnpay.hash.secret", "").trim()}\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    // Khoá upload cho Google Play (Play App Signing giữ khoá ký app). Keystore và mật khẩu chỉ nằm ở local.properties, xem README mục 9
    val missingSigningKeys = RELEASE_SIGNING_KEYS.filter { localProps.getProperty(it).isNullOrBlank() }
    if (missingSigningKeys.isEmpty()) {
        signingConfigs.create("release") {
            storeFile = file(localProps.getProperty("release.store.file"))
            storePassword = localProps.getProperty("release.store.password")
            keyAlias = localProps.getProperty("release.key.alias")
            keyPassword = localProps.getProperty("release.key.password")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (missingSigningKeys.isEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Thiếu khoá ký thì chỉ báo lỗi lúc đóng gói release (không phải lúc cấu hình), để máy không có keystore vẫn build debug và chạy test
val signingError = RELEASE_SIGNING_KEYS.filter { localProps.getProperty(it).isNullOrBlank() }
    .takeIf { it.isNotEmpty() }
    ?.let { "Thiếu cấu hình ký release trong local.properties: ${it.joinToString()}. Xem README mục 9." }
if (signingError != null) {
    tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" || it.name.startsWith("packageRelease") }
        .configureEach { doFirst { throw GradleException(signingError) } }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // --- Firebase BOM (Quản lý version đồng bộ) ---
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth") // Dùng cho Login, 2FA OTP
    implementation("com.google.firebase:firebase-firestore") // Lưu dữ liệu & ảnh Base64 eKYC
    implementation("com.google.firebase:firebase-ai") // Trợ lý AI (F7), Gemini Developer API qua Firebase AI Logic

    // --- Sinh trắc học (Vân tay / Khuôn mặt phần cứng) ---
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // --- Kiến trúc MVVM: ViewModel & Coroutines ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // --- UI: Fragment, danh sách, thẻ vuốt ngang ---
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.6")

    // --- Nhận diện & đối chiếu khuôn mặt on-device ---
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation("com.google.mlkit:barcode-scanning:17.3.0") // Quét QR nhận tiền (F4)
    implementation("com.google.zxing:core:3.5.3") // Sinh mã QR "Mã của tôi" (Apache-2.0)
    // --- Bản đồ chi nhánh và vị trí (F6) ---
    implementation("org.osmdroid:osmdroid-android:6.1.20") // Bản đồ OpenStreetMap, Apache-2.0, không key
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.ai.edge.litert:litert:1.4.2")
    implementation("androidx.camera:camera-core:1.6.2")
    implementation("androidx.camera:camera-camera2:1.6.2")
    implementation("androidx.camera:camera-lifecycle:1.6.2")
    implementation("androidx.camera:camera-view:1.6.2")

    // --- Test ---
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.json:json:20240303") // org.json của Android trong unit test chỉ là stub
}