package com.flux.bank.data.repository

import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

/** Tài khoản ngân hàng (collection `accounts`). */
interface AccountRepository {
    /** Tạo tài khoản Checking số dư 0 cho [ownerUid]. */
    suspend fun createChecking(ownerUid: String): Result<BankAccount>
    suspend fun listByOwner(ownerUid: String): Result<List<BankAccount>>
}

class FirestoreAccountRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AccountRepository {

    private val accounts get() = db.collection(COLLECTION)
    private val random = SecureRandom()

    override suspend fun createChecking(ownerUid: String): Result<BankAccount> = runCatching {
        val number = uniqueAccountNumber()
        val doc = accounts.document()
        val account = BankAccount(accountId = doc.id, ownerUid = ownerUid, type = AccountType.CHECKING, accountNumber = number)
        doc.set(account).await()
        account
    }

    override suspend fun listByOwner(ownerUid: String): Result<List<BankAccount>> = runCatching {
        accounts.whereEqualTo("ownerUid", ownerUid).get().await().toObjects(BankAccount::class.java)
            .sortedBy { it.type.ordinal }
    }

    /** Số tài khoản = "8868" + 8 chữ số ngẫu nhiên; thử lại một lần nếu trùng. */
    private suspend fun uniqueAccountNumber(): String {
        repeat(2) {
            val candidate = PREFIX + "%08d".format(random.nextInt(100_000_000))
            val taken = !accounts.whereEqualTo("accountNumber", candidate).limit(1).get().await().isEmpty
            if (!taken) return candidate
        }
        error("Không sinh được số tài khoản, vui lòng thử lại")
    }

    private companion object {
        const val COLLECTION = "accounts"
        const val PREFIX = "8868"
    }
}
