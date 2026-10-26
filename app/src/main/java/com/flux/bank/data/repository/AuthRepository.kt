package com.flux.bank.data.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/** Đăng nhập, phiên và tạo tài khoản Auth cho khách hàng (do nhân viên thực hiện). */
interface AuthRepository {
    /** Đăng nhập bằng email/mật khẩu. Trả về uid. */
    suspend fun login(email: String, password: String): Result<String>
    fun currentUid(): String?
    fun currentEmail(): String?
    fun logout()
    suspend fun changePassword(newPassword: String): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /**
     * Tạo user Auth mới trên một FirebaseApp phụ nên nhân viên KHÔNG bị đăng xuất.
     * User mới vẫn đăng nhập trên app phụ cho tới khi gọi [releaseCreatedAuthUser] hoặc [deleteCreatedAuthUser].
     */
    suspend fun createAuthUser(email: String, tempPassword: String): Result<String>

    /** Rollback: xoá user vừa tạo (khi ghi hồ sơ thất bại). */
    suspend fun deleteCreatedAuthUser(): Result<Unit>

    /** Onboarding thành công: đăng xuất user mới khỏi app phụ. */
    fun releaseCreatedAuthUser()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    private val onboardingAuth: FirebaseAuth by lazy {
        val primary = FirebaseApp.getInstance()
        val app = FirebaseApp.getApps(primary.applicationContext).firstOrNull { it.name == ONBOARDING_APP }
            ?: FirebaseApp.initializeApp(primary.applicationContext, primary.options, ONBOARDING_APP)
        FirebaseAuth.getInstance(app)
    }

    override suspend fun login(email: String, password: String): Result<String> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await().user?.uid
            ?: error("Đăng nhập không trả về người dùng")
    }

    override fun currentUid(): String? = auth.currentUser?.uid

    override fun currentEmail(): String? = auth.currentUser?.email

    override fun logout() = auth.signOut()

    override suspend fun changePassword(newPassword: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Chưa đăng nhập")
        user.updatePassword(newPassword).await()
        Unit
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    override suspend fun createAuthUser(email: String, tempPassword: String): Result<String> = runCatching {
        onboardingAuth.createUserWithEmailAndPassword(email.trim(), tempPassword).await().user?.uid
            ?: error("Không tạo được tài khoản")
    }

    override suspend fun deleteCreatedAuthUser(): Result<Unit> = runCatching {
        onboardingAuth.currentUser?.delete()?.await()
        onboardingAuth.signOut()
    }

    override fun releaseCreatedAuthUser() = onboardingAuth.signOut()

    private companion object {
        const val ONBOARDING_APP = "onboarding"
    }
}
