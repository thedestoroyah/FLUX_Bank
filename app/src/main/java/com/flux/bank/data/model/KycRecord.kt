package com.flux.bank.data.model

/**
 * Hồ sơ eKYC, document `kyc/{uid}`. Tách khỏi `users` để đọc hồ sơ không phải tải ảnh
 * và không chạm giới hạn 1 MB/document.
 *
 * @property faceEmbedding vector khuôn mặt (MobileFaceNet) tính từ ảnh selfie; rỗng nếu chưa tính.
 * @property submittedBy uid người nộp: chính khách hàng, hoặc nhân viên chụp hộ.
 */
data class KycRecord(
    val uid: String = "",
    val idCardBase64: String = "",
    val faceImageBase64: String = "",
    val faceEmbedding: List<Double> = emptyList(),
    val submittedAt: Long = System.currentTimeMillis(),
    val submittedBy: String = "",
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null
)
