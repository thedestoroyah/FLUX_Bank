package com.flux.bank.data.repository

import com.flux.bank.data.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/** Lịch sử giao dịch (collection `transactions`). Ghi giao dịch nằm trong [AccountRepository]. */
interface TransactionRepository {
    /** [limit] giao dịch mới nhất của [ownerUid], mới nhất trước. Cần composite index (ownerUid, createdAt desc). */
    suspend fun listRecent(ownerUid: String, limit: Long = DEFAULT_LIMIT): Result<List<Transaction>>

    /** Giao dịch của [ownerUid] có `paymentRef` = [paymentRef] (hoá đơn đã trả), null nếu chưa có. Chỉ điều kiện bằng nên không cần index ghép. */
    suspend fun findByPaymentRef(ownerUid: String, paymentRef: String): Result<Transaction?>

    companion object {
        const val DEFAULT_LIMIT = 100L
    }
}

class FirestoreTransactionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : TransactionRepository {

    override suspend fun listRecent(ownerUid: String, limit: Long): Result<List<Transaction>> = runCatching {
        db.collection(COLLECTION)
            .whereEqualTo("ownerUid", ownerUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get().await()
            .toObjects(Transaction::class.java)
    }

    override suspend fun findByPaymentRef(ownerUid: String, paymentRef: String): Result<Transaction?> = runCatching {
        db.collection(COLLECTION)
            .whereEqualTo("ownerUid", ownerUid)
            .whereEqualTo("paymentRef", paymentRef)
            .limit(1)
            .get().await()
            .toObjects(Transaction::class.java)
            .firstOrNull()
    }

    companion object {
        const val COLLECTION = "transactions"
    }
}
