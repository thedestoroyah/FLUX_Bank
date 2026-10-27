package com.flux.bank.data.repository

import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

/** Rút quá số dư (số dư có thể đã đổi từ máy khác sau khi người dùng xem trước). */
class InsufficientFundsException : Exception("Số dư không đủ")

/** Tài khoản ngân hàng (collection `accounts`). */
interface AccountRepository {
    /** Tạo tài khoản Checking số dư 0 cho [ownerUid]. */
    suspend fun createChecking(ownerUid: String): Result<BankAccount>
    suspend fun listByOwner(ownerUid: String): Result<List<BankAccount>>
    suspend fun getAccount(accountId: String): Result<BankAccount>

    /** Nhân viên mở tài khoản tiết kiệm 0 ₫ (lãi suất đọc từ `config/rates`, không lưu trong tài khoản). */
    suspend fun openSaving(ownerUid: String): Result<BankAccount>

    /** Nhân viên mở khoản vay: dư nợ = [loanAmount], lãi suất [annualRate] cố định từ lúc mở. */
    suspend fun openMortgage(ownerUid: String, loanAmount: Long, termMonths: Int, annualRate: Double): Result<BankAccount>

    /**
     * Nạp/rút: một Firestore transaction ghi cùng lúc số dư mới và bản ghi `transactions`.
     * Rút quá số dư trả về [InsufficientFundsException]; tài khoản MORTGAGE trả về [IllegalStateException].
     */
    suspend fun deposit(accountId: String, amount: Long): Result<Transaction>
    suspend fun withdraw(accountId: String, amount: Long): Result<Transaction>
}

class FirestoreAccountRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AccountRepository {

    private val accounts get() = db.collection(COLLECTION)
    private val transactions get() = db.collection(FirestoreTransactionRepository.COLLECTION)
    private val random = SecureRandom()

    override suspend fun createChecking(ownerUid: String): Result<BankAccount> = runCatching {
        create(BankAccount(ownerUid = ownerUid, type = AccountType.CHECKING))
    }

    override suspend fun listByOwner(ownerUid: String): Result<List<BankAccount>> = runCatching {
        accounts.whereEqualTo("ownerUid", ownerUid).get().await().toObjects(BankAccount::class.java)
            .sortedBy { it.type.ordinal }
    }

    override suspend fun getAccount(accountId: String): Result<BankAccount> = runCatching {
        accounts.document(accountId).get().await().toObject(BankAccount::class.java)
            ?: error("Không tìm thấy tài khoản")
    }

    override suspend fun openSaving(ownerUid: String): Result<BankAccount> = runCatching {
        create(BankAccount(ownerUid = ownerUid, type = AccountType.SAVING))
    }

    override suspend fun openMortgage(ownerUid: String, loanAmount: Long, termMonths: Int, annualRate: Double) = runCatching {
        val now = System.currentTimeMillis()
        create(
            BankAccount(
                ownerUid = ownerUid, type = AccountType.MORTGAGE, balance = loanAmount, createdAt = now,
                loanAmount = loanAmount, annualRate = annualRate, termMonths = termMonths, startDate = now
            )
        )
    }

    override suspend fun deposit(accountId: String, amount: Long) = move(accountId, amount, TransactionType.DEPOSIT)

    override suspend fun withdraw(accountId: String, amount: Long) = move(accountId, amount, TransactionType.WITHDRAW)

    private suspend fun move(accountId: String, amount: Long, type: TransactionType): Result<Transaction> = runCatching {
        val accountRef = accounts.document(accountId)
        val txRef = transactions.document()
        try {
            db.runTransaction { t ->
                val account = t.get(accountRef).toObject(BankAccount::class.java) ?: error("Không tìm thấy tài khoản")
                check(account.type != AccountType.MORTGAGE) { "Không nạp/rút trên khoản vay" }
                val newBalance = if (type.isIncoming) account.balance + amount else account.balance - amount
                if (newBalance < 0) throw InsufficientFundsException()
                val tx = Transaction(
                    id = txRef.id, accountId = accountId, ownerUid = account.ownerUid,
                    accountNumber = account.accountNumber, type = type, amount = amount, balanceAfter = newBalance
                )
                t.update(accountRef, mapOf("balance" to newBalance, "lastTxId" to txRef.id))
                t.set(txRef, tx)
                tx
            }.await()
        } catch (e: Exception) {
            // Firestore có thể bọc ngoại lệ ném trong transaction: gỡ ra để caller nhận đúng loại
            throw generateSequence<Throwable>(e) { it.cause }.firstOrNull { it is InsufficientFundsException } ?: e
        }
    }

    /** Gán id và số tài khoản rồi ghi [template]. */
    private suspend fun create(template: BankAccount): BankAccount {
        val doc = accounts.document()
        val account = template.copy(accountId = doc.id, accountNumber = uniqueAccountNumber())
        doc.set(account).await()
        return account
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

    companion object {
        const val COLLECTION = "accounts"
        private const val PREFIX = "8868"
    }
}
