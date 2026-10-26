package com.flux.bank.domain

import com.flux.bank.data.model.KycRecord
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.model.User

/** Kết quả kiểm tra điều kiện trước khi quét mặt. */
enum class FacePrecheck { OK, NOT_VERIFIED, NO_EMBEDDING }

/** Quy tắc khi nào giao dịch cần thêm bước xác thực khuôn mặt (ngoài OTP). */
object FaceVerificationPolicy {

    /** Giao dịch từ 10.000.000 ₫ trở lên là "giá trị cao". */
    const val HIGH_VALUE_THRESHOLD = 10_000_000L

    /** [amount] null nghĩa là thao tác không có số tiền (ví dụ đổi SĐT) — chỉ cần OTP. */
    fun requiresFace(amount: Long?): Boolean = amount != null && amount >= HIGH_VALUE_THRESHOLD

    /** Chỉ khách đã được duyệt eKYC và có embedding khuôn mặt mới đối chiếu được. */
    fun precheck(user: User, kyc: KycRecord?): FacePrecheck = when {
        user.kycStatus != KycStatus.VERIFIED -> FacePrecheck.NOT_VERIFIED
        kyc == null || kyc.faceEmbedding.isEmpty() -> FacePrecheck.NO_EMBEDDING
        else -> FacePrecheck.OK
    }
}
