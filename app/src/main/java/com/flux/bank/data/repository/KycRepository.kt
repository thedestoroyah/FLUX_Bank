package com.flux.bank.data.repository

import com.flux.bank.data.model.KycRecord
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Hồ sơ eKYC (collection `kyc`) và trạng thái eKYC trên `users`. */
interface KycRepository {
    /** Ghi `kyc/{uid}` và đặt `users.kycStatus = PENDING`, xoá lý do từ chối cũ, trong một batch. */
    suspend fun submit(record: KycRecord): Result<Unit>
    suspend fun get(uid: String): Result<KycRecord?>
    suspend fun pendingQueue(): Result<List<Pair<User, KycRecord>>>

    /** Nhân viên duyệt ([approve] = true) hoặc từ chối kèm [reason]. */
    suspend fun review(uid: String, approve: Boolean, reason: String?, officerUid: String): Result<Unit>
}

class FirestoreKycRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : KycRepository {

    private val kyc get() = db.collection(COLLECTION)
    private val users get() = db.collection(FirestoreUserRepository.COLLECTION)

    override suspend fun submit(record: KycRecord): Result<Unit> = runCatching {
        db.batch()
            .set(kyc.document(record.uid), record)
            .update(users.document(record.uid), mapOf("kycStatus" to KycStatus.PENDING, "kycRejectReason" to null))
            .commit().await()
        Unit
    }

    override suspend fun get(uid: String): Result<KycRecord?> = runCatching {
        kyc.document(uid).get().await().toObject(KycRecord::class.java)
    }

    // Hàng đợi nhỏ nên đọc từng hồ sơ kyc theo uid là chấp nhận được
    override suspend fun pendingQueue(): Result<List<Pair<User, KycRecord>>> = runCatching {
        users.whereEqualTo("kycStatus", KycStatus.PENDING).get().await()
            .toObjects(User::class.java)
            .mapNotNull { user -> get(user.uid).getOrThrow()?.let { user to it } }
            .sortedBy { it.second.submittedAt }
    }

    override suspend fun review(uid: String, approve: Boolean, reason: String?, officerUid: String): Result<Unit> =
        runCatching {
            val status = if (approve) KycStatus.VERIFIED else KycStatus.REJECTED
            db.batch()
                .update(users.document(uid), mapOf("kycStatus" to status, "kycRejectReason" to if (approve) null else reason))
                .update(kyc.document(uid), mapOf("reviewedBy" to officerUid, "reviewedAt" to System.currentTimeMillis()))
                .commit().await()
            Unit
        }

    companion object {
        const val COLLECTION = "kyc"
    }
}
