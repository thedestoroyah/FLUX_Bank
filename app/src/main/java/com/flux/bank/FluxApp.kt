package com.flux.bank

import android.app.Application
import com.flux.bank.core.security.OtpManager
import com.flux.bank.core.security.OtpNotifier
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.FirebaseAuthRepository
import com.flux.bank.data.repository.FirestoreAccountRepository
import com.flux.bank.data.repository.FirestoreKycRepository
import com.flux.bank.data.repository.FirestoreUserRepository
import com.flux.bank.data.repository.KycRepository
import com.flux.bank.data.repository.UserRepository

/**
 * Application của FLUX: nơi khởi tạo và giữ các dịch vụ dùng chung (service locator đơn giản, không dùng DI).
 */
class FluxApp : Application() {

    val otpManager: OtpManager by lazy { OtpManager() }
    val otpNotifier: OtpNotifier by lazy { OtpNotifier(this) }
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
    val userRepository: UserRepository by lazy { FirestoreUserRepository() }
    val kycRepository: KycRepository by lazy { FirestoreKycRepository() }
    val accountRepository: AccountRepository by lazy { FirestoreAccountRepository() }

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
