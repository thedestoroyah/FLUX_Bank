package com.flux.bank.data.repository

import com.flux.bank.data.model.Rates
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Lãi suất chung, document `config/rates`. */
interface RatesRepository {
    /** Trả [Rates] mặc định khi document chưa tồn tại. */
    suspend fun get(): Result<Rates>
    suspend fun save(rates: Rates): Result<Unit>
}

class FirestoreRatesRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : RatesRepository {

    private val doc get() = db.collection(COLLECTION).document(DOC_ID)

    override suspend fun get(): Result<Rates> = runCatching {
        doc.get().await().toObject(Rates::class.java) ?: Rates()
    }

    override suspend fun save(rates: Rates): Result<Unit> = runCatching {
        doc.set(rates).await()
        Unit
    }

    companion object {
        const val COLLECTION = "config"
        const val DOC_ID = "rates"
    }
}
