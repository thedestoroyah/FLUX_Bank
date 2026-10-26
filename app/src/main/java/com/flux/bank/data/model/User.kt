package com.flux.bank.data.model

enum class UserRole {
    CUSTOMER,
    BANK_OFFICER
}

object KycStatus {
    const val NOT_SUBMITTED = "NOT_SUBMITTED"
    const val PENDING = "PENDING"
    const val VERIFIED = "VERIFIED"
    const val REJECTED = "REJECTED"
}

data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val role: UserRole = UserRole.CUSTOMER,
    val idCardBase64: String? = null,
    val faceImageBase64: String? = null,
    val kycStatus: String = KycStatus.NOT_SUBMITTED,
    val isBiometricEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
