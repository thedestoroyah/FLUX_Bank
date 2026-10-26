package com.flux.bank.ui.officer.customers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.data.model.User
import com.flux.bank.domain.CustomerOnboardingService
import com.flux.bank.domain.NewCustomerInput
import com.flux.bank.domain.OnboardingException
import com.flux.bank.ui.common.Event
import kotlinx.coroutines.launch
import java.security.SecureRandom

/** Khách vừa tạo kèm mật khẩu tạm (chỉ hiện cho nhân viên một lần). */
data class CreatedCustomer(val user: User, val tempPassword: String)

/** Form nhân viên tạo khách hàng; lỗi được gắn vào đúng ô nhập. */
class CreateCustomerViewModel(
    private val service: CustomerOnboardingService = FluxApp.instance.onboardingService
) : ViewModel() {

    private val _state = MutableLiveData(CustomerFormState())
    val state: LiveData<CustomerFormState> = _state

    private val _created = MutableLiveData<Event<CreatedCustomer>>()
    val created: LiveData<Event<CreatedCustomer>> = _created

    fun submit(input: NewCustomerInput) {
        _state.value = CustomerFormState(saving = true)
        viewModelScope.launch {
            service.create(input)
                .onSuccess { user ->
                    _state.value = CustomerFormState(saved = true)
                    _created.value = Event(CreatedCustomer(user, input.tempPassword))
                }
                .onFailure { e ->
                    val error = (e as? OnboardingException)?.error
                    val (field, res) = error?.toFieldError() ?: (null to com.flux.bank.R.string.err_generic)
                    _state.value = if (field != null) CustomerFormState(fieldErrors = mapOf(field to res))
                    else CustomerFormState(errorRes = res)
                }
        }
    }

    companion object {
        private const val LETTERS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ"
        private const val DIGITS = "23456789"
        private const val LENGTH = 10
        private val random = SecureRandom()

        /** Mật khẩu tạm 10 ký tự, luôn có cả chữ và số (bỏ ký tự dễ nhầm như l/1, O/0). */
        fun generatePassword(): String {
            val pool = LETTERS + DIGITS
            val chars = MutableList(LENGTH - 2) { pool[random.nextInt(pool.length)] }
            chars += LETTERS[random.nextInt(LETTERS.length)]
            chars += DIGITS[random.nextInt(DIGITS.length)]
            chars.shuffle(random)
            return chars.joinToString("")
        }
    }
}
