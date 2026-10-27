package com.flux.bank.data.repository

import com.flux.bank.data.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/** Lịch sử giao dịch (collection `transactions`). Ghi giao dịch nằm trong [AccountRepository]. */
interface TransactionRepository {
    /** [limit] giao dịch mới nhất của [ownerUid], mới nhất trước. Cần composite index (ownerUid, createdAt desc). */
    suspend fun listRecent(ownerUid: String, limit: Long = DEFAULT_LIMIT): Result<List<Transaction>>

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

    companion object {
        const val COLLECTION = "transactions"
    }
}
