package com.flux.bank.data.repository

import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Payee
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Danh bạ tra người nhận theo số tài khoản (collection `accountIndex`), vì không đọc được `accounts` của người khác. */
interface PayeeRepository {
    /** Tra `accountIndex/{accountNumber}`; không có thì `Result.success(null)`. */
    suspend fun find(accountNumber: String): Result<Payee?>

    /**
     * Ghi (lại) mục index cho các Checking/Saving trong [accounts] khi thiếu hoặc tên khác [fullName].
     * Bỏ qua tài khoản đã kiểm trong phiên này (mỗi lần mở trang chủ chỉ tốn đọc ở lần đầu).
     */
    suspend fun ensureIndexed(accounts: List<BankAccount>, fullName: String): Result<Unit>
}

class FirestorePayeeRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PayeeRepository {

    private val index get() = db.collection(COLLECTION)
    private val checked = mutableSetOf<String>()

    override suspend fun find(accountNumber: String): Result<Payee?> = runCatching {
        val doc = index.document(accountNumber).get().await()
        if (!doc.exists()) return@runCatching null
        Payee(
            accountId = doc.getString("accountId").orEmpty(),
            accountNumber = accountNumber,
            ownerUid = doc.getString("ownerUid").orEmpty(),
            holderName = doc.getString("holderName").orEmpty(),
            type = AccountType.valueOf(doc.getString("type") ?: AccountType.CHECKING.name)
        )
    }

    override suspend fun ensureIndexed(accounts: List<BankAccount>, fullName: String): Result<Unit> = runCatching {
        accounts.filter { it.type != AccountType.MORTGAGE && "${it.accountId}|$fullName" !in checked }.forEach { account ->
            val ref = index.document(account.accountNumber)
            val existing = ref.get().await()
            if (!existing.exists() || existing.getString("holderName") != fullName) {
                ref.set(
                    mapOf(
                        "accountId" to account.accountId, "ownerUid" to account.ownerUid,
                        "type" to account.type.name, "holderName" to fullName
                    )
                ).await()
            }
            checked += "${account.accountId}|$fullName"
        }
    }

    companion object {
        const val COLLECTION = "accountIndex"
    }
}
