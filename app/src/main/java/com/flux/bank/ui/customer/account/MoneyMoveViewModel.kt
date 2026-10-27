package com.flux.bank.ui.customer.account

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.core.validation.AmountError
import com.flux.bank.core.validation.Validators
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.InsufficientFundsException
import com.flux.bank.data.repository.UserRepository
import com.flux.bank.ui.common.Event
import kotlinx.coroutines.launch

data class MoneyMoveState(
    val deposit: Boolean,
    val accounts: List<BankAccount> = emptyList(),
    val selectedId: String? = null,
    val amount: Long = 0,
    val balanceAfter: Long? = null,
    val amountError: AmountError? = null,
    val kycBlocked: Boolean = false,
    val submitting: Boolean = false,
    @StringRes val errorRes: Int? = null
) {
    val selected: BankAccount? get() = accounts.firstOrNull { it.accountId == selectedId }
}

/**
 * Nạp ([deposit] = true) hoặc rút tiền trên Checking/Saving (mockup màn 3).
 * Luồng: [confirm] kiểm tra rồi phát [askAuth] → màn hình mở `TransactionAuthSheet` → [onAuthResult] → ghi giao dịch → [done].
 */
class MoneyMoveViewModel(
    private val deposit: Boolean,
    private val accounts: AccountRepository = FluxApp.instance.accountRepository,
    private val users: UserRepository = FluxApp.instance.userRepository,
    private val auth: AuthRepository = FluxApp.instance.authRepository
) : ViewModel() {

    private val _state = MutableLiveData(MoneyMoveState(deposit))
    val state: LiveData<MoneyMoveState> = _state

    private val _askAuth = MutableLiveData<Event<Long>>()
    val askAuth: LiveData<Event<Long>> = _askAuth

    private val _done = MutableLiveData<Event<Transaction>>()
    val done: LiveData<Event<Transaction>> = _done

    // Đang chờ kết quả sheet xác thực: chặn bấm "Tiếp tục" lần 2 mở thêm sheet
    private var awaitingAuth = false
    private var kycStatus: String? = null

    /** [preselectedAccountId] là tài khoản đang xem (mở từ chi tiết); null thì chọn Checking đầu tiên. */
    fun load(preselectedAccountId: String?) {
        val uid = auth.currentUid() ?: return
        viewModelScope.launch {
            // Lỗi đọc hồ sơ (mạng) không có nghĩa là chưa eKYC: báo lỗi chung thay vì bảo khách đi xác minh lại
            val user = users.getUser(uid)
            kycStatus = user.getOrNull()?.kycStatus
            val list = movable(uid)
            update { s ->
                s.copy(
                    accounts = list,
                    selectedId = s.selectedId ?: (list.firstOrNull { it.accountId == preselectedAccountId } ?: list.firstOrNull())?.accountId,
                    kycBlocked = user.isSuccess && kycStatus != KycStatus.VERIFIED,
                    errorRes = if (user.isFailure) R.string.err_generic else s.errorRes
                )
            }
        }
    }

    fun select(accountId: String) = update { it.copy(selectedId = accountId, amountError = null, errorRes = null) }

    fun setAmount(input: String) = update { it.copy(amount = Validators.parseAmount(input), amountError = null, errorRes = null) }

    fun confirm() {
        val s = _state.value ?: return
        if (kycStatus != KycStatus.VERIFIED || s.submitting || awaitingAuth) return
        val account = s.selected ?: return
        Validators.amountError(deposit, s.amount, account.balance)?.let { error ->
            return update { it.copy(amountError = error) }
        }
        awaitingAuth = true
        _askAuth.value = Event(s.amount)
    }

    fun onAuthResult(verified: Boolean) {
        if (!awaitingAuth) return
        awaitingAuth = false
        if (!verified) return
        val s = _state.value ?: return
        val account = s.selected ?: return
        update { it.copy(submitting = true, errorRes = null) }
        viewModelScope.launch {
            val result = if (deposit) accounts.deposit(account.accountId, s.amount) else accounts.withdraw(account.accountId, s.amount)
            result
                .onSuccess { tx ->
                    update { it.copy(submitting = false) }
                    _done.value = Event(tx)
                }
                .onFailure { e ->
                    val insufficient = e is InsufficientFundsException
                    // Số dư đã đổi từ nơi khác: tải lại để phần xem trước đúng với số dư thật
                    val fresh = if (insufficient) auth.currentUid()?.let { movable(it) } else null
                    update {
                        it.copy(
                            submitting = false,
                            accounts = fresh ?: it.accounts,
                            errorRes = if (insufficient) R.string.err_insufficient_funds else R.string.err_generic
                        )
                    }
                }
        }
    }

    private suspend fun movable(uid: String) =
        accounts.listByOwner(uid).getOrNull().orEmpty().filter { it.type != AccountType.MORTGAGE }

    /** Mỗi lần đổi tài khoản/số tiền thì tính lại "Số dư sau giao dịch". */
    private fun update(block: (MoneyMoveState) -> MoneyMoveState) {
        val next = block(_state.value ?: MoneyMoveState(deposit))
        val balance = next.selected?.balance
        val after = when {
            balance == null || next.amount <= 0 -> null
            deposit -> balance + next.amount
            else -> (balance - next.amount).takeIf { it >= 0 }
        }
        _state.value = next.copy(balanceAfter = after)
    }
}
