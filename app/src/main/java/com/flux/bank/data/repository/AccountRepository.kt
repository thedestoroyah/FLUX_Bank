package com.flux.bank.data.repository

import com.flux.bank.core.format.TextNormalizer
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.Bank
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Banks
import com.flux.bank.data.model.Payee
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
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

    /** Id cho một document `transactions` sắp tạo (VNPay dùng làm `vnp_TxnRef`). */
    fun newTransactionId(): String

    /**
     * Nạp/rút: một Firestore transaction ghi cùng lúc số dư mới và bản ghi `transactions`.
     * Rút quá số dư trả về [InsufficientFundsException]; tài khoản MORTGAGE trả về [IllegalStateException].
     * Nạp qua VNPay truyền [txId] (= `vnp_TxnRef`, rules chặn ghi lại id đã có) và [paymentRef] (= `vnp_TransactionNo`).
     */
    suspend fun deposit(accountId: String, amount: Long, txId: String? = null, paymentRef: String? = null): Result<Transaction>
    suspend fun withdraw(accountId: String, amount: Long): Result<Transaction>

    /**
     * Chuyển trong FLUX: một Firestore transaction trừ nguồn, tạo TRANSFER_OUT, cộng đích bằng `increment`
     * (người gửi không đọc được tài khoản đích) và tạo TRANSFER_IN trỏ cặp với nhau.
     * [senderName] phải đúng `users.fullName` của người gửi (rules đối chiếu). Trả về giao dịch TRANSFER_OUT;
     * thiếu tiền trả về [InsufficientFundsException].
     */
    suspend fun transfer(fromId: String, payee: Payee, amount: Long, description: String, senderName: String): Result<Transaction>

    /** Chuyển liên ngân hàng (giả lập): chỉ trừ nguồn và tạo TRANSFER_OUT, không ai được cộng tiền. */
    suspend fun transferExternal(
        fromId: String, bank: Bank, accountNumber: String, holderName: String, amount: Long, description: String
    ): Result<Transaction>
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

    override fun newTransactionId(): String = transactions.document().id

    override suspend fun deposit(accountId: String, amount: Long, txId: String?, paymentRef: String?) =
        move(accountId, amount, TransactionType.DEPOSIT, txId, paymentRef)

    override suspend fun withdraw(accountId: String, amount: Long) = move(accountId, amount, TransactionType.WITHDRAW)

    private suspend fun move(
        accountId: String, amount: Long, type: TransactionType, txId: String? = null, paymentRef: String? = null
    ): Result<Transaction> = runCatching {
        val accountRef = accounts.document(accountId)
        val txRef = txId?.let(transactions::document) ?: transactions.document()
        unwrapInsufficientFunds {
            db.runTransaction { t ->
                val account = t.get(accountRef).toObject(BankAccount::class.java) ?: error("Không tìm thấy tài khoản")
                check(account.type != AccountType.MORTGAGE) { "Không nạp/rút trên khoản vay" }
                val newBalance = if (type.isIncoming) account.balance + amount else account.balance - amount
                if (newBalance < 0) throw InsufficientFundsException()
                val tx = Transaction(
                    id = txRef.id, accountId = accountId, ownerUid = account.ownerUid,
                    accountNumber = account.accountNumber, type = type, amount = amount, balanceAfter = newBalance,
                    paymentRef = paymentRef
                )
                t.update(accountRef, mapOf("balance" to newBalance, "lastTxId" to txRef.id))
                t.set(txRef, tx)
                tx
            }.await()
        }
    }

    override suspend fun transfer(fromId: String, payee: Payee, amount: Long, description: String, senderName: String) = runCatching {
        check(payee.accountId != fromId) { "Không chuyển cho chính tài khoản nguồn" }
        val toRef = accounts.document(payee.accountId)
        val outRef = transactions.document()
        val inRef = transactions.document()
        unwrapInsufficientFunds {
            db.runTransaction { t ->
                val (from, out) = debit(t, fromId, outRef, amount) { account, balance ->
                    Transaction(
                        id = outRef.id, accountId = fromId, ownerUid = account.ownerUid, accountNumber = account.accountNumber,
                        type = TransactionType.TRANSFER_OUT, amount = amount, balanceAfter = balance, description = description,
                        counterpartyName = TextNormalizer.holderName(payee.holderName), counterpartyAccount = payee.accountNumber,
                        counterpartyBank = Banks.FLUX, pairTxId = inRef.id
                    )
                }
                val incoming = Transaction(
                    id = inRef.id, accountId = payee.accountId, ownerUid = payee.ownerUid, accountNumber = payee.accountNumber,
                    type = TransactionType.TRANSFER_IN, amount = amount, balanceAfter = null, description = description,
                    counterpartyName = senderName, counterpartyAccount = from.accountNumber, counterpartyBank = Banks.FLUX,
                    pairTxId = outRef.id, createdAt = out.createdAt
                )
                t.update(toRef, mapOf("balance" to FieldValue.increment(amount), "lastTxId" to inRef.id))
                t.set(inRef, incoming)
                out
            }.await()
        }
    }

    override suspend fun transferExternal(
        fromId: String, bank: Bank, accountNumber: String, holderName: String, amount: Long, description: String
    ) = runCatching {
        val outRef = transactions.document()
        unwrapInsufficientFunds {
            db.runTransaction { t ->
                debit(t, fromId, outRef, amount) { account, balance ->
                    Transaction(
                        id = outRef.id, accountId = fromId, ownerUid = account.ownerUid, accountNumber = account.accountNumber,
                        type = TransactionType.TRANSFER_OUT, amount = amount, balanceAfter = balance, description = description,
                        counterpartyName = TextNormalizer.holderName(holderName), counterpartyAccount = accountNumber,
                        counterpartyBank = bank.code
                    )
                }.second
            }.await()
        }
    }

    /** Trong transaction [t]: đọc nguồn, trừ [amount], ghi số dư + giao dịch do [build] dựng. Trả về (tài khoản nguồn, giao dịch). */
    private fun debit(
        t: com.google.firebase.firestore.Transaction, fromId: String, txRef: DocumentReference, amount: Long,
        build: (BankAccount, Long) -> Transaction
    ): Pair<BankAccount, Transaction> {
        val fromRef = accounts.document(fromId)
        val account = t.get(fromRef).toObject(BankAccount::class.java) ?: error("Không tìm thấy tài khoản")
        check(account.type != AccountType.MORTGAGE) { "Không chuyển tiền từ khoản vay" }
        val newBalance = account.balance - amount
        if (newBalance < 0) throw InsufficientFundsException()
        val tx = build(account, newBalance)
        t.update(fromRef, mapOf("balance" to newBalance, "lastTxId" to txRef.id))
        t.set(txRef, tx)
        return account to tx
    }

    // Firestore có thể bọc ngoại lệ ném trong transaction: gỡ ra để caller nhận đúng loại
    private inline fun <T> unwrapInsufficientFunds(block: () -> T): T = try {
        block()
    } catch (e: Exception) {
        throw generateSequence<Throwable>(e) { it.cause }.firstOrNull { it is InsufficientFundsException } ?: e
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
