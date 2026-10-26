plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

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
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
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
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-auth-ktx") // Dùng cho Login, 2FA OTP
    implementation("com.google.firebase:firebase-firestore-ktx") // Lưu dữ liệu & ảnh Base64 eKYC

    // --- Sinh trắc học (Vân tay / Khuôn mặt phần cứng) ---
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // --- Kiến trúc MVVM: ViewModel & Coroutines ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // --- Xử lý tải ảnh (khi cần load ảnh Base64 hoặc URL) ---
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // --- Google Maps & GPS (cho mục tìm đường chi nhánh ngân hàng) ---
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // --- Mạng & API (Gọi AI Chatbot và cổng thanh toán) ---
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}