package com.flux.bank.data.model

enum class UserRole {
    CUSTOMER,
    BANK_OFFICER
}

/** Giá trị trạng thái eKYC lưu trong field `users.kycStatus`. */
object KycStatus {
    const val NOT_SUBMITTED = "NOT_SUBMITTED"
    const val PENDING = "PENDING"
    const val VERIFIED = "VERIFIED"
    const val REJECTED = "REJECTED"
}

/**
 * Hồ sơ người dùng, document `users/{uid}`.
 * Field boolean không có tiền tố `is` để Firestore map đúng tên field.
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val nationalId: String = "",
    val address: String = "",
    val avatarBase64: String? = null,
    val role: UserRole = UserRole.CUSTOMER,
    val kycStatus: String = KycStatus.NOT_SUBMITTED,
    val kycRejectReason: String? = null,
    val biometricEnabled: Boolean = false,
    val mustChangePassword: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null
)
