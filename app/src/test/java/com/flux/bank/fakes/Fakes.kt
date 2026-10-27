package com.flux.bank.fakes

import com.flux.bank.core.format.TextNormalizer
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.Bank
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Banks
import com.flux.bank.data.model.Payee
import com.flux.bank.data.model.KycRecord
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.model.Rates
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import com.flux.bank.data.model.User
import com.flux.bank.data.model.UserRole
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.InsufficientFundsException
import com.flux.bank.data.repository.KycRepository
import com.flux.bank.data.repository.PayeeRepository
import com.flux.bank.data.repository.RatesRepository
import com.flux.bank.data.repository.TransactionRepository
import com.flux.bank.data.repository.UserRepository
import kotlinx.coroutines.CompletableDeferred

/** Repository giả trong bộ nhớ, dùng chung cho unit test. */
class FakeAuthRepository : AuthRepository {
    var uid: String? = null
    var email: String? = null
    var loginResult: Result<String> = Result.success("u1")
    val created = mutableListOf<String>()
    var deleted = false
    var released = false
    var failCreate = false
    var changedPassword: String? = null
    var resetEmail: String? = null
    private var counter = 0

    override suspend fun login(email: String, password: String) = loginResult.onSuccess { uid = it; this.email = email }
    override fun currentUid() = uid
    override fun currentEmail() = email
    override fun logout() { uid = null }
    override suspend fun changePassword(newPassword: String): Result<Unit> { changedPassword = newPassword; return Result.success(Unit) }
    override suspend fun sendPasswordReset(email: String): Result<Unit> { resetEmail = email; return Result.success(Unit) }
    override suspend fun createAuthUser(email: String, tempPassword: String): Result<String> {
        if (failCreate) return Result.failure(IllegalStateException("email exists"))
        val id = "new${++counter}"
        created += id
        return Result.success(id)
    }
    override suspend fun deleteCreatedAuthUser(): Result<Unit> { deleted = true; return Result.success(Unit) }
    override fun releaseCreatedAuthUser() { released = true }
}

class FakeUserRepository(initial: List<User> = emptyList()) : UserRepository {
    val users = initial.associateBy { it.uid }.toMutableMap()
    var failNextCreate = false
    var failUpdate = false
    val updates = mutableListOf<Pair<String, Map<String, Any?>>>()

    override suspend fun getUser(uid: String) = users[uid]?.let { Result.success(it) } ?: Result.failure(NoSuchElementException(uid))
    override suspend fun createProfile(user: User): Result<Unit> {
        if (failNextCreate) { failNextCreate = false; return Result.failure(IllegalStateException("write failed")) }
        users[user.uid] = user
        return Result.success(Unit)
    }
    override suspend fun updateFields(uid: String, fields: Map<String, Any?>): Result<Unit> {
        if (failUpdate) return Result.failure(IllegalStateException("update failed"))
        updates += uid to fields
        return Result.success(Unit)
    }
    override suspend fun deleteProfile(uid: String): Result<Unit> { users.remove(uid); return Result.success(Unit) }
    val phoneChanges = mutableListOf<Triple<String, String, String>>()
    override suspend fun changePhone(uid: String, oldPhone: String, newPhone: String): Result<Unit> {
        if (failUpdate) return Result.failure(IllegalStateException("update failed"))
        phoneChanges += Triple(uid, oldPhone, newPhone)
        users[uid]?.let { users[uid] = it.copy(phoneNumber = newPhone) }
        return Result.success(Unit)
    }
    override suspend fun listCustomers() = Result.success(users.values.filter { it.role == UserRole.CUSTOMER })
    override suspend fun findUidByPhone(phone: String) = Result.success(users.values.firstOrNull { it.phoneNumber == phone }?.uid)
    var nationalIdQueries = 0
    override suspend fun findByNationalId(nationalId: String): Result<User?> {
        nationalIdQueries++
        return Result.success(users.values.firstOrNull { it.nationalId == nationalId })
    }
    override suspend fun countCustomers() = Result.success(users.values.count { it.role == UserRole.CUSTOMER })
}

class FakeAccountRepository : AccountRepository {
    val accounts = mutableListOf<BankAccount>()
    var failNextCreate = false
    /** Lỗi trả về cho lần nạp/rút kế tiếp (ví dụ [InsufficientFundsException]). */
    var failNextMove: Throwable? = null
    /** Nếu khác null, nạp/rút sẽ treo tới khi gate được complete (để test bấm 2 lần). */
    var moveGate: CompletableDeferred<Unit>? = null
    val deposits = mutableListOf<Pair<String, Long>>()
    val withdrawals = mutableListOf<Pair<String, Long>>()
    val opened = mutableListOf<BankAccount>()
    /** Nếu khác null, mở tài khoản sẽ treo tới khi gate được complete. */
    var openGate: CompletableDeferred<Unit>? = null

    override suspend fun createChecking(ownerUid: String): Result<BankAccount> {
        if (failNextCreate) { failNextCreate = false; return Result.failure(IllegalStateException("account failed")) }
        val a = BankAccount(accountId = "a${accounts.size}", ownerUid = ownerUid, type = AccountType.CHECKING, accountNumber = "886800000${accounts.size}")
        accounts += a
        return Result.success(a)
    }
    override suspend fun listByOwner(ownerUid: String) = Result.success(accounts.filter { it.ownerUid == ownerUid })
    override suspend fun getAccount(accountId: String) =
        accounts.firstOrNull { it.accountId == accountId }?.let { Result.success(it) } ?: Result.failure(NoSuchElementException(accountId))

    override suspend fun openSaving(ownerUid: String) = open(BankAccount(ownerUid = ownerUid, type = AccountType.SAVING))
    override suspend fun openMortgage(ownerUid: String, loanAmount: Long, termMonths: Int, annualRate: Double) =
        open(BankAccount(ownerUid = ownerUid, type = AccountType.MORTGAGE, balance = loanAmount, loanAmount = loanAmount, termMonths = termMonths, annualRate = annualRate))

    private suspend fun open(template: BankAccount): Result<BankAccount> {
        openGate?.await()
        val a = template.copy(accountId = "a${accounts.size}", accountNumber = "886800000${accounts.size}")
        accounts += a; opened += a
        return Result.success(a)
    }

    /** Các giao dịch TRANSFER_OUT đã ghi (cả trong FLUX và liên ngân hàng). */
    val transfers = mutableListOf<Transaction>()
    private var nextTxId = 0

    override fun newTransactionId() = "vnp${nextTxId++}"

    override suspend fun deposit(accountId: String, amount: Long, txId: String?, paymentRef: String?): Result<Transaction> {
        deposits += accountId to amount
        return move(accountId, amount, TransactionType.DEPOSIT)
            .map { tx -> tx.copy(id = txId ?: tx.id, paymentRef = paymentRef) }
    }
    override suspend fun withdraw(accountId: String, amount: Long): Result<Transaction> { withdrawals += accountId to amount; return move(accountId, amount, TransactionType.WITHDRAW) }

    override suspend fun transfer(fromId: String, payee: Payee, amount: Long, description: String, senderName: String): Result<Transaction> =
        move(fromId, amount, TransactionType.TRANSFER_OUT).map { out ->
            accounts.indexOfFirst { it.accountId == payee.accountId }.takeIf { it >= 0 }?.let { i ->
                accounts[i] = accounts[i].copy(balance = accounts[i].balance + amount)
            }
            out.copy(description = description, counterpartyName = TextNormalizer.holderName(payee.holderName),
                counterpartyAccount = payee.accountNumber, counterpartyBank = Banks.FLUX, pairTxId = "in-${out.id}")
                .also { transfers += it }
        }

    override suspend fun transferExternal(fromId: String, bank: Bank, accountNumber: String, holderName: String, amount: Long, description: String) =
        move(fromId, amount, TransactionType.TRANSFER_OUT).map { out ->
            out.copy(description = description, counterpartyName = TextNormalizer.holderName(holderName),
                counterpartyAccount = accountNumber, counterpartyBank = bank.code).also { transfers += it }
        }

    private suspend fun move(accountId: String, amount: Long, type: TransactionType): Result<Transaction> {
        moveGate?.await()
        failNextMove?.let { failNextMove = null; return Result.failure(it) }
        val i = accounts.indexOfFirst { it.accountId == accountId }
        if (i < 0) return Result.failure(NoSuchElementException(accountId))
        val account = accounts[i]
        val newBalance = if (type.isIncoming) account.balance + amount else account.balance - amount
        if (newBalance < 0) return Result.failure(InsufficientFundsException())
        val tx = Transaction(id = "tx${deposits.size + withdrawals.size + transfers.size}", accountId = accountId, ownerUid = account.ownerUid,
            accountNumber = account.accountNumber, type = type, amount = amount, balanceAfter = newBalance)
        accounts[i] = account.copy(balance = newBalance, lastTxId = tx.id)
        return Result.success(tx)
    }
}

class FakePayeeRepository(val index: MutableMap<String, Payee> = mutableMapOf()) : PayeeRepository {
    /** Nếu khác null, tra cứu sẽ treo tới khi gate được complete (để test kết quả về muộn). */
    var findGate: CompletableDeferred<Unit>? = null
    val ensured = mutableListOf<Pair<List<BankAccount>, String>>()

    override suspend fun find(accountNumber: String): Result<Payee?> {
        findGate?.await()
        return Result.success(index[accountNumber])
    }

    override suspend fun ensureIndexed(accounts: List<BankAccount>, fullName: String): Result<Unit> {
        ensured += accounts to fullName
        return Result.success(Unit)
    }
}

class FakeTransactionRepository(val items: MutableList<Transaction> = mutableListOf()) : TransactionRepository {
    override suspend fun listRecent(ownerUid: String, limit: Long) =
        Result.success(items.filter { it.ownerUid == ownerUid }.sortedByDescending { it.createdAt }.take(limit.toInt()))
}

class FakeRatesRepository(var rates: Rates = Rates()) : RatesRepository {
    val saved = mutableListOf<Rates>()
    var failGet = false
    override suspend fun get(): Result<Rates> = if (failGet) Result.failure(IllegalStateException("offline")) else Result.success(rates)
    override suspend fun save(rates: Rates): Result<Unit> { saved += rates; this.rates = rates; return Result.success(Unit) }
}

class FakeKycRepository : KycRepository {
    val records = mutableMapOf<String, KycRecord>()
    val submitted = mutableListOf<KycRecord>()
    val reviews = mutableListOf<List<Any?>>()
    var queue = mutableListOf<Pair<User, KycRecord>>()

    override suspend fun submit(record: KycRecord): Result<Unit> { submitted += record; records[record.uid] = record; return Result.success(Unit) }
    override suspend fun get(uid: String) = Result.success(records[uid])
    override suspend fun pendingQueue() = Result.success(queue.toList())
    override suspend fun review(uid: String, approve: Boolean, reason: String?, officerUid: String): Result<Unit> {
        reviews += listOf(uid, approve, reason, officerUid)
        queue.removeAll { it.first.uid == uid }
        return Result.success(Unit)
    }
}

fun customer(uid: String, phone: String = "09000000${uid.takeLast(2).padStart(2, '0')}", nationalId: String = "0792030012${uid.takeLast(2).padStart(2, '0')}", kyc: String = KycStatus.NOT_SUBMITTED, name: String = "Khách $uid") =
    User(uid = uid, email = "$uid@mail.com", fullName = name, phoneNumber = phone, nationalId = nationalId, kycStatus = kyc)
