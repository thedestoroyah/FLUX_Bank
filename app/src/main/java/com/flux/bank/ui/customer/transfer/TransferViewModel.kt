package com.flux.bank.ui.customer.transfer

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.core.format.TextNormalizer
import com.flux.bank.core.validation.Validators
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.Bank
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Banks
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.model.Payee
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.InsufficientFundsException
import com.flux.bank.data.repository.PayeeRepository
import com.flux.bank.data.repository.TransactionRepository
import com.flux.bank.data.repository.UserRepository
import com.flux.bank.domain.TransferError
import com.flux.bank.domain.TransferPolicy
import com.flux.bank.ui.common.Event
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class TransferState(
    val mode: TransferMode,
    val sources: List<BankAccount> = emptyList(),
    val sourceId: String? = null,
    val ownTargets: List<BankAccount> = emptyList(),
    val targetId: String? = null,
    val accountNumber: String = "",
    val payee: Payee? = null,
    val lookingUp: Boolean = false,
    val bank: Bank? = null,
    val externalName: String = "",
    val amount: Long = 0,
    val description: String = "",
    val remainingToday: Long = TransferPolicy.DAILY_LIMIT,
    val recipientError: TransferError? = null,
    val amountError: TransferError? = null,
    val kycBlocked: Boolean = false,
    val submitting: Boolean = false,
    @StringRes val errorRes: Int? = null
) {
    val source: BankAccount? get() = sources.firstOrNull { it.accountId == sourceId }
    val target: BankAccount? get() = ownTargets.firstOrNull { it.accountId == targetId }
}

/** Nội dung hiện trên sheet xác thực: người nhận (in hoa không dấu) và "ngân hàng · số tài khoản". */
data class AuthRequest(val amount: Long, val recipientName: String, val recipientDetail: String)

/**
 * Form chuyển tiền cho cả ba chế độ (mockup F4 màn 2–3). Kiểm tra hợp lệ trước khi ghi (spec §4):
 * người nhận → số tiền/số dư/hạn mức → sheet xác thực → ghi giao dịch → [done].
 */
class TransferViewModel(
    private val mode: TransferMode,
    private val accounts: AccountRepository = FluxApp.instance.accountRepository,
    private val payees: PayeeRepository = FluxApp.instance.payeeRepository,
    private val transactions: TransactionRepository = FluxApp.instance.transactionRepository,
    private val users: UserRepository = FluxApp.instance.userRepository,
    private val auth: AuthRepository = FluxApp.instance.authRepository,
    private val now: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _state = MutableLiveData(TransferState(mode))
    val state: LiveData<TransferState> = _state

    private val _askAuth = MutableLiveData<Event<AuthRequest>>()
    val askAuth: LiveData<Event<AuthRequest>> = _askAuth

    private val _done = MutableLiveData<Event<Transaction>>()
    val done: LiveData<Event<Transaction>> = _done

    private var fullName = ""
    private var kycStatus: String? = null
    private var descriptionEdited = false
    // Yêu cầu đã kiểm tra lúc bấm "Tiếp tục", đang chờ sheet xác thực. Ghi đúng yêu cầu này
    // (form có thể đổi trong lúc sheet mở, ví dụ khi màn hình được tạo lại); khác null thì chặn mở sheet thứ hai.
    private var confirmed: TransferState? = null
    private var lookupJob: Job? = null

    /** Tải hồ sơ, tài khoản nguồn và hạn mức còn lại; [prefill] điền sẵn người nhận (gần đây hoặc QR). */
    fun load(prefill: TransferPrefill?) {
        val uid = auth.currentUid() ?: return
        viewModelScope.launch {
            val user = users.getUser(uid)
            fullName = user.getOrNull()?.fullName.orEmpty()
            kycStatus = user.getOrNull()?.kycStatus
            val sources = accounts.listByOwner(uid).getOrNull().orEmpty().filter { it.type != AccountType.MORTGAGE }
            val history = transactions.listRecent(uid).getOrDefault(emptyList())
            update { s ->
                withSource(
                    s.copy(
                        sources = sources,
                        remainingToday = TransferPolicy.remainingToday(history, now()),
                        description = if (descriptionEdited) s.description else defaultDescription(),
                        kycBlocked = user.isSuccess && kycStatus != KycStatus.VERIFIED,
                        // Lỗi đọc hồ sơ (mạng) không có nghĩa là chưa eKYC
                        errorRes = if (user.isFailure) R.string.err_generic else s.errorRes
                    ),
                    s.sourceId ?: sources.firstOrNull()?.accountId
                )
            }
            prefill?.let(::applyPrefill)
        }
    }

    fun selectSource(accountId: String) = update { s ->
        withSource(s, accountId).copy(
            amountError = null, errorRes = null,
            recipientError = if (s.payee?.accountId == accountId) TransferError.SAME_ACCOUNT else s.recipientError.takeUnless { it == TransferError.SAME_ACCOUNT }
        )
    }

    fun selectTarget(accountId: String) = update { it.copy(targetId = accountId, recipientError = null) }

    /** Chỉ giữ chữ số. Chế độ trong FLUX: đủ 12 số thì tra tên, huỷ lần tra trước để kết quả cũ không ghi đè. */
    fun setAccountNumber(input: String) {
        val digits = input.filter(Char::isDigit).take(MAX_ACCOUNT_DIGITS)
        if (digits == _state.value?.accountNumber) return
        lookupJob?.cancel()
        update { it.copy(accountNumber = digits, payee = null, lookingUp = false, recipientError = null, errorRes = null) }
        if (mode != TransferMode.INTERNAL || digits.length != FLUX_ACCOUNT_DIGITS) return
        if (!TransferPolicy.isFluxAccountNumber(digits)) return update { it.copy(recipientError = TransferError.INVALID_ACCOUNT_NUMBER) }
        update { it.copy(lookingUp = true) }
        lookupJob = viewModelScope.launch {
            val result = payees.find(digits)
            if (_state.value?.accountNumber != digits) return@launch
            val payee = result.getOrNull()
            update { s ->
                s.copy(
                    lookingUp = false,
                    payee = payee,
                    recipientError = when {
                        result.isFailure -> null
                        payee == null -> TransferError.PAYEE_NOT_FOUND
                        payee.accountId == s.sourceId -> TransferError.SAME_ACCOUNT
                        else -> null
                    },
                    errorRes = if (result.isFailure) R.string.err_generic else null
                )
            }
        }
    }

    fun selectBank(code: String) = update { it.copy(bank = Banks.byCode(code), recipientError = null) }

    fun setExternalName(input: String) = update { it.copy(externalName = input, recipientError = null) }

    fun setAmount(input: String) = update { it.copy(amount = Validators.parseAmount(input), amountError = null, errorRes = null) }

    /** Nội dung dài quá [TransferPolicy.MAX_DESCRIPTION] bị cắt (ô nhập cũng giới hạn độ dài). */
    fun setDescription(input: String) {
        descriptionEdited = true
        update { it.copy(description = input.take(TransferPolicy.MAX_DESCRIPTION)) }
    }

    fun confirm() {
        val s = _state.value ?: return
        if (kycStatus != KycStatus.VERIFIED || s.submitting || confirmed != null || s.lookingUp) return
        val source = s.source ?: return
        val recipientError = recipientError(s)
        val amountError = TransferPolicy.checkAmount(s.amount, source.balance, s.remainingToday)
        if (recipientError != null || amountError != null) {
            return update { it.copy(recipientError = recipientError, amountError = amountError) }
        }
        confirmed = s
        _askAuth.value = Event(authRequest(s))
    }

    fun onAuthResult(verified: Boolean) {
        val s = confirmed ?: return
        confirmed = null
        if (!verified) return
        val sourceId = s.sourceId ?: return
        update { it.copy(submitting = true, errorRes = null) }
        viewModelScope.launch {
            val result = when (mode) {
                TransferMode.INTERNAL -> accounts.transfer(sourceId, s.payee!!, s.amount, s.description, fullName)
                TransferMode.OWN -> accounts.transfer(sourceId, ownPayee(s.target!!), s.amount, s.description, fullName)
                TransferMode.EXTERNAL ->
                    accounts.transferExternal(sourceId, s.bank!!, s.accountNumber, s.externalName, s.amount, s.description)
            }
            result
                .onSuccess { tx ->
                    update { it.copy(submitting = false) }
                    _done.value = Event(tx)
                }
                .onFailure { e ->
                    val insufficient = e is InsufficientFundsException
                    // Số dư đã đổi từ nơi khác: tải lại để kiểm tra lần sau dùng số dư thật
                    val fresh = if (insufficient) auth.currentUid()?.let { uid ->
                        accounts.listByOwner(uid).getOrNull()?.filter { it.type != AccountType.MORTGAGE }
                    } else null
                    update {
                        withSource(it.copy(submitting = false, sources = fresh ?: it.sources), it.sourceId).copy(
                            errorRes = if (insufficient) R.string.err_insufficient_funds else R.string.err_generic
                        )
                    }
                }
        }
    }

    private fun applyPrefill(prefill: TransferPrefill) {
        if (mode == TransferMode.EXTERNAL) {
            selectBank(prefill.bankCode)
            setExternalName(prefill.name)
        }
        setAccountNumber(prefill.accountNumber)
    }

    private fun recipientError(s: TransferState): TransferError? = when (mode) {
        TransferMode.INTERNAL -> when {
            !TransferPolicy.isFluxAccountNumber(s.accountNumber) -> TransferError.INVALID_ACCOUNT_NUMBER
            s.payee == null -> s.recipientError ?: TransferError.PAYEE_NOT_FOUND
            s.payee.accountId == s.sourceId -> TransferError.SAME_ACCOUNT
            else -> null
        }
        TransferMode.OWN -> if (s.target == null) TransferError.PAYEE_NOT_FOUND else null
        TransferMode.EXTERNAL -> when {
            !TransferPolicy.isExternalAccountNumber(s.accountNumber) -> TransferError.INVALID_ACCOUNT_NUMBER
            s.bank == null -> TransferError.NO_BANK
            s.externalName.isBlank() -> TransferError.NO_NAME
            else -> null
        }
    }

    private fun authRequest(s: TransferState): AuthRequest = when (mode) {
        TransferMode.INTERNAL ->
            AuthRequest(s.amount, TextNormalizer.holderName(s.payee!!.holderName), "${Banks.FLUX} · ${s.accountNumber}")
        TransferMode.OWN -> AuthRequest(s.amount, TextNormalizer.holderName(fullName), "${Banks.FLUX} · ${s.target!!.accountNumber}")
        TransferMode.EXTERNAL ->
            AuthRequest(s.amount, TextNormalizer.holderName(s.externalName), "${s.bank!!.shortName} · ${s.accountNumber}")
    }

    private fun ownPayee(target: BankAccount) =
        Payee(target.accountId, target.accountNumber, target.ownerUid, fullName, target.type)

    /** Đặt nguồn và tính lại danh sách đích "của tôi" (mọi nguồn trừ nguồn đang chọn). */
    private fun withSource(s: TransferState, sourceId: String?): TransferState {
        val targets = s.sources.filter { it.accountId != sourceId }
        return s.copy(
            sourceId = sourceId,
            ownTargets = targets,
            targetId = s.targetId?.takeIf { id -> targets.any { it.accountId == id } } ?: targets.firstOrNull()?.accountId
        )
    }

    private fun defaultDescription() = "${TextNormalizer.holderName(fullName)} chuyen tien"

    private fun update(block: (TransferState) -> TransferState) {
        _state.value = block(_state.value ?: TransferState(mode))
    }

    private companion object {
        const val FLUX_ACCOUNT_DIGITS = 12
        const val MAX_ACCOUNT_DIGITS = 19
    }
}
