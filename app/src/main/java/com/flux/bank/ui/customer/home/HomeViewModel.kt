package com.flux.bank.ui.customer.home

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.util.Calendar

/** Một thẻ tài khoản trên trang chủ, đã định dạng sẵn để hiển thị. */
data class AccountCardUi(
    val accountId: String,
    @StringRes val typeLabel: Int,
    @StringRes val balanceLabel: Int,
    val maskedNumber: String,
    val balanceText: String
)

data class HomeUiState(
    val loading: Boolean = true,
    @StringRes val greeting: Int = R.string.greeting_morning,
    val firstName: String = "",
    val initials: String = "",
    val accounts: List<AccountCardUi> = emptyList(),
    val balanceHidden: Boolean = false,
    val kycStatus: String = KycStatus.VERIFIED
)

/**
 * Dữ liệu trang chủ khách hàng: lời chào theo giờ, tên gọi, các thẻ tài khoản và trạng thái eKYC.
 *
 * @param hourOfDay nguồn giờ hiện tại (0-23), tách ra để test lời chào.
 */
class HomeViewModel(
    private val auth: AuthRepository = FluxApp.instance.authRepository,
    private val users: UserRepository = FluxApp.instance.userRepository,
    private val accountsRepo: AccountRepository = FluxApp.instance.accountRepository,
    private val hourOfDay: () -> Int = { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
) : ViewModel() {

    private val _state = MutableLiveData(HomeUiState())
    val state: LiveData<HomeUiState> = _state
    private var rawAccounts: List<BankAccount> = emptyList()

    fun load() {
        val uid = auth.currentUid() ?: return
        viewModelScope.launch {
            val user = users.getUser(uid).getOrNull()
            rawAccounts = accountsRepo.listByOwner(uid).getOrDefault(emptyList())
            val current = _state.value ?: HomeUiState()
            _state.value = current.copy(
                loading = false,
                greeting = greetingFor(hourOfDay()),
                firstName = shortName(user?.fullName.orEmpty()),
                initials = initials(user?.fullName.orEmpty()),
                kycStatus = user?.kycStatus ?: KycStatus.NOT_SUBMITTED,
                accounts = cards(current.balanceHidden)
            )
        }
    }

    fun toggleBalance() {
        val current = _state.value ?: return
        val hidden = !current.balanceHidden
        _state.value = current.copy(balanceHidden = hidden, accounts = cards(hidden))
    }

    private fun cards(hidden: Boolean) = rawAccounts.map { account ->
        AccountCardUi(
            accountId = account.accountId,
            typeLabel = labelFor(account.type),
            balanceLabel = if (account.type == AccountType.MORTGAGE) R.string.card_outstanding else R.string.card_available_balance,
            maskedNumber = "•••• " + account.accountNumber.takeLast(4),
            balanceText = if (hidden) HIDDEN_BALANCE else MoneyFormatter.formatVnd(account.balance)
        )
    }

    companion object {
        private const val HIDDEN_BALANCE = "••••••• ₫"

        @StringRes
        fun greetingFor(hour: Int): Int = when {
            hour < 12 -> R.string.greeting_morning
            hour < 18 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }

        /** Tên gọi kiểu Việt: tên có từ 3 chữ trở lên lấy 2 chữ cuối ("Nguyễn Minh Anh" -> "Minh Anh"), còn lại lấy chữ cuối. */
        fun shortName(fullName: String): String {
            val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            return if (parts.size >= 3) parts.takeLast(2).joinToString(" ") else parts.lastOrNull().orEmpty()
        }

        fun initials(fullName: String): String =
            shortName(fullName).split(" ").filter { it.isNotEmpty() }.joinToString("") { it.take(1) }.uppercase()

        @StringRes
        fun labelFor(type: AccountType): Int = when (type) {
            AccountType.CHECKING -> R.string.account_checking
            AccountType.SAVING -> R.string.account_saving
            AccountType.MORTGAGE -> R.string.account_mortgage
        }
    }
}
