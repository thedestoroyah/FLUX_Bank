package com.flux.bank

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.flux.bank.core.security.AppLockObserver
import com.flux.bank.core.security.LockPolicy
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
import com.flux.bank.domain.CustomerOnboardingService
import com.flux.bank.utils.SessionManager

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
    val sessionManager: SessionManager by lazy { SessionManager(this) }
    val onboardingService: CustomerOnboardingService by lazy {
        CustomerOnboardingService(authRepository, userRepository, accountRepository) { authRepository.currentUid().orEmpty() }
    }
    val lockPolicy = LockPolicy()
    val appLockObserver: AppLockObserver by lazy { AppLockObserver(this, lockPolicy, authRepository, sessionManager) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        otpNotifier.createChannel()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLockObserver)
    }

    companion object {
        lateinit var instance: FluxApp
            private set
    }
}
