package com.flux.bank.ui.officer.customers

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.core.validation.Validators
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Rates
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.RatesRepository
import com.flux.bank.domain.InterestCalculator
import com.flux.bank.domain.PaymentFrequency
import com.flux.bank.ui.common.Event
import kotlinx.coroutines.launch

data class OpenAccountState(
    val type: AccountType = AccountType.SAVING,
    val mortgageRate: Double = Rates().mortgageRate,
    val loanAmount: Long = 0,
    val termMonths: Int = DEFAULT_TERM,
    val monthly: Long? = null,
    val biweekly: Long? = null,
    @StringRes val errorRes: Int? = null,
    val submitting: Boolean = false
) {
    companion object {
        const val DEFAULT_TERM = 240
    }
}

/** Nhân viên mở tài khoản Tiết kiệm (0 ₫) hoặc khoản vay thế chấp cho khách (mockup màn 7). */
class OpenAccountViewModel(
    private val accounts: AccountRepository = FluxApp.instance.accountRepository,
    private val rates: RatesRepository = FluxApp.instance.ratesRepository
) : ViewModel() {

    private val _state = MutableLiveData(OpenAccountState())
    val state: LiveData<OpenAccountState> = _state

    private val _opened = MutableLiveData<Event<BankAccount>>()
    val opened: LiveData<Event<BankAccount>> = _opened

    /** Lãi suất vay để xem trước; lỗi đọc thì tạm hiện mức mặc định (lúc mở khoản vay sẽ đọc lại, xem [submit]). */
    fun load() {
        viewModelScope.launch {
            rates.get().onSuccess { r -> update { it.copy(mortgageRate = r.mortgageRate) } }
        }
    }

    fun selectType(type: AccountType) = update { it.copy(type = type, errorRes = null) }

    fun setLoanAmount(input: String) = update { it.copy(loanAmount = Validators.parseAmount(input), errorRes = null) }

    fun setTerm(months: Int) = update { it.copy(termMonths = months, errorRes = null) }

    fun submit(ownerUid: String) {
        val s = _state.value ?: return
        if (s.submitting) return
        if (s.type == AccountType.MORTGAGE) {
            if (s.loanAmount <= 0) return update { it.copy(errorRes = R.string.err_loan_amount) }
            if (s.termMonths !in MIN_TERM..MAX_TERM) return update { it.copy(errorRes = R.string.err_loan_term) }
        }
        update { it.copy(submitting = true, errorRes = null) }
        viewModelScope.launch {
            val result = if (s.type == AccountType.MORTGAGE) {
                // Lãi vay được cố định vào khoản vay: đọc lại mức hiện hành, không dùng số mặc định khi tải lỗi
                val rate = rates.get().getOrElse {
                    return@launch update { it.copy(submitting = false, errorRes = R.string.err_generic) }
                }.mortgageRate
                accounts.openMortgage(ownerUid, s.loanAmount, s.termMonths, rate)
            } else {
                accounts.openSaving(ownerUid)
            }
            result
                .onSuccess { account ->
                    update { it.copy(submitting = false) }
                    _opened.value = Event(account)
                }
                .onFailure { update { it.copy(submitting = false, errorRes = R.string.err_generic) } }
        }
    }

    /** Mỗi lần đổi số tiền/thời hạn thì tính lại phần xem trước tiền trả mỗi kỳ. */
    private fun update(block: (OpenAccountState) -> OpenAccountState) {
        val next = block(_state.value ?: OpenAccountState())
        val valid = next.loanAmount > 0 && next.termMonths in MIN_TERM..MAX_TERM
        _state.value = next.copy(
            monthly = if (valid) InterestCalculator.installment(next.loanAmount, next.mortgageRate, next.termMonths, PaymentFrequency.MONTHLY) else null,
            biweekly = if (valid) InterestCalculator.installment(next.loanAmount, next.mortgageRate, next.termMonths, PaymentFrequency.BIWEEKLY) else null
        )
    }

    companion object {
        const val MIN_TERM = 1
        const val MAX_TERM = 360
    }
}
