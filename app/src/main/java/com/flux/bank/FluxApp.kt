package com.flux.bank

import android.app.Application
import android.content.Intent
import com.flux.bank.ui.auth.UnlockActivity
import com.flux.bank.utils.BiometricHelper
import androidx.lifecycle.ProcessLifecycleOwner
import com.flux.bank.core.security.AppLockObserver
import com.flux.bank.core.security.LockPolicy
import com.flux.bank.core.security.OtpManager
import com.flux.bank.core.security.OtpNotifier
import com.flux.bank.data.assistant.AssistantClient
import com.flux.bank.data.assistant.GeminiAssistantClient
import com.flux.bank.data.location.FusedLocationSource
import com.flux.bank.data.location.LocationSource
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.routing.OsrmWalkingRouter
import com.flux.bank.data.routing.WalkingRouter
import com.flux.bank.domain.branch.BranchRecommender
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.FirebaseAuthRepository
import com.flux.bank.data.repository.FirestoreAccountRepository
import com.flux.bank.data.repository.FirestoreKycRepository
import com.flux.bank.data.repository.FirestorePayeeRepository
import com.flux.bank.data.repository.FirestoreRatesRepository
import com.flux.bank.data.repository.FirestoreTransactionRepository
import com.flux.bank.data.repository.FirestoreUserRepository
import com.flux.bank.data.repository.KycRepository
import com.flux.bank.data.repository.PayeeRepository
import com.flux.bank.data.repository.RatesRepository
import com.flux.bank.data.repository.TransactionRepository
import com.flux.bank.data.repository.UserRepository
import com.flux.bank.domain.CustomerOnboardingService
import com.flux.bank.payment.VnPay
import com.flux.bank.payment.VnPayConfig
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
    val transactionRepository: TransactionRepository by lazy { FirestoreTransactionRepository() }
    val ratesRepository: RatesRepository by lazy { FirestoreRatesRepository() }
    val payeeRepository: PayeeRepository by lazy { FirestorePayeeRepository() }
    val sessionManager: SessionManager by lazy { SessionManager(this) }
    val onboardingService: CustomerOnboardingService by lazy {
        CustomerOnboardingService(authRepository, userRepository, accountRepository) { authRepository.currentUid().orEmpty() }
    }
    /** Cổng VNPay sandbox; null khi `local.properties` chưa có khoá. */
    val vnPay: VnPay? by lazy {
        BuildConfig.VNPAY_TMN_CODE.takeIf { it.isNotBlank() }?.let { VnPay(VnPayConfig(it, BuildConfig.VNPAY_HASH_SECRET)) }
    }
    /** Chỉ đường đi bộ (F6): server OSRM công cộng, miễn phí, không key. */
    val assistantClient: AssistantClient by lazy { GeminiAssistantClient() }
    val walkingRouter: WalkingRouter by lazy { OsrmWalkingRouter() }
    val branchRecommender: BranchRecommender by lazy { BranchRecommender(walkingRouter) }
    val locationSource: LocationSource by lazy { FusedLocationSource(this) }
    val lockPolicy = LockPolicy()
    val appLockObserver: AppLockObserver by lazy {
        AppLockObserver(
            policy = lockPolicy,
            auth = authRepository,
            biometricEnabled = sessionManager::isBiometricEnabled,
            biometricAvailable = { BiometricHelper.canAuthenticate(this) },
            startUnlock = {
                startActivity(
                    UnlockActivity.intent(this, UnlockActivity.Mode.RESUME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        installAppCheck(this)
        otpNotifier.createChannel()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLockObserver)
    }

    companion object {
        lateinit var instance: FluxApp
            private set
    }
}
