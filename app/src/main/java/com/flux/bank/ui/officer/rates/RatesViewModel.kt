package com.flux.bank.ui.officer.rates

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.core.validation.Validators
import com.flux.bank.data.model.Rates
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.RatesRepository
import com.flux.bank.data.repository.UserRepository
import com.flux.bank.domain.InterestCalculator
import com.flux.bank.domain.PaymentFrequency
import com.flux.bank.ui.common.Event
import kotlinx.coroutines.launch

enum class RateKind { SAVING, MORTGAGE }

data class RatesState(
    val rates: Rates = Rates(),
    val loading: Boolean = true,
    @StringRes val errorRes: Int? = null,
    val saving: Boolean = false
)

/**
 * Tab "Lãi suất" của nhân viên: xem và sửa lãi suất tiết kiệm / vay thế chấp trong `config/rates`.
 *
 * @param clock nguồn thời gian (ms) cho `updatedAt`, tách ra để test.
 */
class RatesViewModel(
    private val repo: RatesRepository = FluxApp.instance.ratesRepository,
    private val auth: AuthRepository = FluxApp.instance.authRepository,
    private val users: UserRepository = FluxApp.instance.userRepository,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _state = MutableLiveData(RatesState())
    val state: LiveData<RatesState> = _state

    private val _saved = MutableLiveData<Event<Unit>>()
    val saved: LiveData<Event<Unit>> = _saved

    fun load() {
        viewModelScope.launch {
            repo.get()
                .onSuccess { rates -> update { it.copy(rates = rates, loading = false) } }
                .onFailure { update { it.copy(loading = false, errorRes = R.string.err_generic) } }
        }
    }

    /**
     * Dòng ví dụ trong sheet sửa: lãi/tháng của 100.000.000 ₫ (tiết kiệm) hoặc tiền trả hàng tháng
     * của khoản vay 1 tỷ / 240 tháng (thế chấp). Null nếu [input] không hợp lệ.
     */
    fun examplePreview(kind: RateKind, input: String): Long? {
        val rate = Validators.parseRate(input) ?: return null
        return when (kind) {
            RateKind.SAVING -> InterestCalculator.monthlySavingInterest(EXAMPLE_SAVING, rate)
            RateKind.MORTGAGE -> InterestCalculator.installment(EXAMPLE_LOAN, rate, EXAMPLE_TERM, PaymentFrequency.MONTHLY)
        }
    }

    /** Lưu mức mới cho [kind]; mức kia giữ nguyên theo giá trị mới nhất trên Firestore. Ghi kèm ai sửa và lúc nào. */
    fun save(kind: RateKind, input: String) {
        val current = _state.value ?: return
        if (current.saving) return
        val rate = Validators.parseRate(input) ?: return update { it.copy(errorRes = R.string.err_rate_range) }
        val uid = auth.currentUid() ?: return
        update { it.copy(saving = true, errorRes = null) }
        viewModelScope.launch {
            // Đọc lại ngay trước khi ghi: tránh ghi đè mức còn lại bằng số cũ (nhân viên khác vừa sửa) hoặc số mặc định (tải lỗi)
            val fresh = repo.get().getOrElse {
                return@launch update { it.copy(saving = false, errorRes = R.string.err_generic) }
            }
            val name = users.getUser(uid).getOrNull()?.fullName.orEmpty()
            val base = fresh.copy(updatedAt = clock(), updatedBy = uid, updatedByName = name)
            val next = if (kind == RateKind.SAVING) base.copy(savingRate = rate) else base.copy(mortgageRate = rate)
            repo.save(next)
                .onSuccess {
                    update { it.copy(rates = next, saving = false) }
                    _saved.value = Event(Unit)
                }
                .onFailure { update { it.copy(saving = false, errorRes = R.string.err_generic) } }
        }
    }

    private fun update(block: (RatesState) -> RatesState) {
        _state.value = block(_state.value ?: RatesState())
    }

    companion object {
        const val EXAMPLE_SAVING = 100_000_000L
        const val EXAMPLE_LOAN = 1_000_000_000L
        const val EXAMPLE_TERM = 240
    }
}
