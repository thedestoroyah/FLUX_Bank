package com.flux.bank.ui.officer.customers

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.core.validation.Validators
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.User
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.UserRepository
import com.flux.bank.domain.CustomerOnboardingService
import com.flux.bank.ui.common.Event
import kotlinx.coroutines.launch

data class CustomerDetailState(
    val user: User? = null,
    val accounts: List<BankAccount> = emptyList(),
    val fieldErrors: Map<CustomerField, Int> = emptyMap(),
    @StringRes val errorRes: Int? = null,
    val saving: Boolean = false,
    val saved: Boolean = false
)

/** Nhân viên xem và sửa hồ sơ khách (trừ email), gửi email đặt lại mật khẩu. */
class CustomerDetailViewModel(
    private val users: UserRepository = FluxApp.instance.userRepository,
    private val auth: AuthRepository = FluxApp.instance.authRepository,
    private val service: CustomerOnboardingService = FluxApp.instance.onboardingService,
    private val accounts: AccountRepository? = null
) : ViewModel() {

    private val _state = MutableLiveData(CustomerDetailState())
    val state: LiveData<CustomerDetailState> = _state

    private val _message = MutableLiveData<Event<Int>>()
    val message: LiveData<Event<Int>> = _message

    fun load(uid: String) {
        viewModelScope.launch {
            val user = users.getUser(uid).getOrNull()
            val list = accounts?.listByOwner(uid)?.getOrNull().orEmpty()
            update { it.copy(user = user, accounts = list) }
        }
    }

    fun save(fullName: String, phone: String, nationalId: String, address: String) {
        val user = _state.value?.user ?: return
        if (fullName.isBlank()) return update { it.copy(fieldErrors = mapOf(CustomerField.NAME to R.string.err_name_required)) }
        val newPhone = Validators.normalizePhone(phone)
        val newId = nationalId.filterNot(Char::isWhitespace)
        update { it.copy(fieldErrors = emptyMap(), errorRes = null, saving = true) }
        viewModelScope.launch {
            service.validateEdit(user.uid, newPhone, newId)?.let { error ->
                val (field, res) = error.toFieldError()
                return@launch update {
                    it.copy(saving = false, fieldErrors = field?.let { f -> mapOf(f to res) }.orEmpty(), errorRes = if (field == null) res else null)
                }
            }
            if (newPhone != user.phoneNumber) {
                users.changePhone(user.uid, user.phoneNumber, newPhone).onFailure {
                    return@launch update { s -> s.copy(saving = false, errorRes = R.string.err_generic) }
                }
            }
            val fields = buildMap<String, Any?> {
                if (fullName.trim() != user.fullName) put("fullName", fullName.trim())
                if (newId != user.nationalId) put("nationalId", newId)
                if (address.trim() != user.address) put("address", address.trim())
            }
            if (fields.isNotEmpty()) {
                users.updateFields(user.uid, fields).onFailure {
                    return@launch update { s -> s.copy(saving = false, errorRes = R.string.err_generic) }
                }
            }
            val updated = user.copy(fullName = fullName.trim(), phoneNumber = newPhone, nationalId = newId, address = address.trim())
            update { it.copy(user = updated, saving = false, saved = true) }
        }
    }

    fun resetPassword() {
        val email = _state.value?.user?.email ?: return
        viewModelScope.launch {
            auth.sendPasswordReset(email)
                .onSuccess { _message.value = Event(R.string.detail_reset_sent) }
                .onFailure { _message.value = Event(R.string.err_generic) }
        }
    }

    private fun update(change: (CustomerDetailState) -> CustomerDetailState) {
        _state.value = change(_state.value ?: CustomerDetailState())
    }
}
