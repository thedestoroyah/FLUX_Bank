package com.flux.bank.ui.customer.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Rates
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.RatesRepository
import com.flux.bank.data.repository.TransactionRepository
import com.flux.bank.domain.InterestCalculator
import com.flux.bank.domain.PaymentFrequency
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Khối lãi của tài khoản tiết kiệm: lãi suất chung hiện tại và lãi dự kiến. */
data class SavingInfo(val ratePercent: Double, val monthlyInterest: Long, val yearlyInterest: Long)

/** Thông tin trả nợ của khoản vay theo kỳ đang chọn. */
data class MortgageInfo(
    val frequency: PaymentFrequency,
    val installment: Long,
    val periods: Int,
    val nextDue: LocalDate,
    val totalInterest: Long,
    val totalPayable: Long
)

data class AccountDetailState(
    val account: BankAccount? = null,
    val saving: SavingInfo? = null,
    val mortgage: MortgageInfo? = null,
    val recent: List<Transaction> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false
)

/**
 * Chi tiết một tài khoản (mockup màn 1–2). Saving đọc lãi suất chung mỗi lần tải nên nhân viên đổi là thấy ngay;
 * Mortgage dùng lãi suất cố định lưu trong tài khoản.
 *
 * @param zone múi giờ để đổi `startDate` (ms) sang ngày; @param today ngày hiện tại, tách ra để test.
 */
class AccountDetailViewModel(
    private val accounts: AccountRepository = FluxApp.instance.accountRepository,
    private val rates: RatesRepository = FluxApp.instance.ratesRepository,
    private val transactions: TransactionRepository = FluxApp.instance.transactionRepository,
    private val auth: AuthRepository = FluxApp.instance.authRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _state = MutableLiveData(AccountDetailState())
    val state: LiveData<AccountDetailState> = _state
    private var frequency = PaymentFrequency.MONTHLY

    fun load(accountId: String) {
        viewModelScope.launch {
            val account = accounts.getAccount(accountId).getOrNull()
                ?: return@launch update { it.copy(loading = false, failed = true) }
            val saving = if (account.type == AccountType.SAVING) {
                val rate = rates.get().getOrNull()?.savingRate ?: Rates().savingRate
                val monthly = InterestCalculator.monthlySavingInterest(account.balance, rate)
                SavingInfo(rate, monthly, monthly * MONTHS_PER_YEAR)
            } else null
            // shortcut: lọc ở client trên 100 giao dịch mới nhất của khách (spec §5); cần index (accountId, createdAt) nếu muốn xem xa hơn
            val recent = auth.currentUid()?.let { uid ->
                transactions.listRecent(uid).getOrNull().orEmpty().filter { it.accountId == accountId }.take(RECENT_COUNT)
            }.orEmpty()
            update { AccountDetailState(account = account, saving = saving, mortgage = mortgageInfo(account), recent = recent, loading = false) }
        }
    }

    fun setFrequency(value: PaymentFrequency) {
        frequency = value
        update { s -> s.copy(mortgage = s.account?.let(::mortgageInfo)) }
    }

    private fun mortgageInfo(account: BankAccount): MortgageInfo? {
        if (account.type != AccountType.MORTGAGE) return null
        val p = account.loanAmount
        val installment = InterestCalculator.installment(p, account.annualRate, account.termMonths, frequency)
        val totalInterest = InterestCalculator.totalInterest(p, account.annualRate, account.termMonths, frequency)
        val start = Instant.ofEpochMilli(account.startDate).atZone(zone).toLocalDate()
        return MortgageInfo(
            frequency = frequency,
            installment = installment,
            periods = InterestCalculator.periods(account.termMonths, frequency),
            nextDue = InterestCalculator.nextDueDate(start, frequency, today()),
            totalInterest = totalInterest,
            totalPayable = p + totalInterest
        )
    }

    private fun update(block: (AccountDetailState) -> AccountDetailState) {
        _state.value = block(_state.value ?: AccountDetailState())
    }

    private companion object {
        const val MONTHS_PER_YEAR = 12
        const val RECENT_COUNT = 5
    }
}
