package com.flux.bank.ui.customer.profile

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.core.validation.Validators
import com.flux.bank.data.model.User
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.UserRepository
import com.flux.bank.domain.CustomerOnboardingService
import com.flux.bank.domain.OnboardingError
import com.flux.bank.ui.common.Event
import kotlinx.coroutines.launch

data class EditProfileState(
    val user: User? = null,
    @StringRes val phoneError: Int? = null,
    @StringRes val errorRes: Int? = null,
    val saving: Boolean = false,
    val saved: Boolean = false
)

/**
 * Khách tự sửa hồ sơ: chỉ SĐT, địa chỉ, ảnh đại diện. Đổi SĐT phải qua OTP: ViewModel phát [authRequest]
 * (SĐT mới), UI mở bottom sheet xác thực rồi gọi [onAuthResult]; chỉ khi xác thực xong mới ghi dữ liệu.
 */
class EditProfileViewModel(
    private val auth: AuthRepository = FluxApp.instance.authRepository,
    private val users: UserRepository = FluxApp.instance.userRepository,
    private val onboarding: CustomerOnboardingService = FluxApp.instance.onboardingService
) : ViewModel() {

    private val _state = MutableLiveData(EditProfileState())
    val state: LiveData<EditProfileState> = _state

    private val _authRequest = MutableLiveData<Event<String>>()
    val authRequest: LiveData<Event<String>> = _authRequest

    private var pendingPhone: String? = null
    private var pendingFields: Map<String, Any?> = emptyMap()

    fun load() {
        val uid = auth.currentUid() ?: return
        viewModelScope.launch {
            users.getUser(uid).onSuccess { user -> update { it.copy(user = user) } }
        }
    }

    /** [avatarBase64] null = không đổi ảnh. */
    fun save(phone: String, address: String, avatarBase64: String? = null) {
        val user = _state.value?.user ?: return
        val newPhone = Validators.normalizePhone(phone)
        val fields = buildMap<String, Any?> {
            if (address.trim() != user.address) put("address", address.trim())
            if (avatarBase64 != null) put("avatarBase64", avatarBase64)
        }
        update { it.copy(phoneError = null, errorRes = null) }
        if (newPhone == user.phoneNumber) return writeFields(user.uid, fields)

        viewModelScope.launch {
            when (val error = onboarding.validatePhoneChange(user.uid, newPhone)) {
                null -> {
                    pendingPhone = newPhone
                    pendingFields = fields
                    _authRequest.value = Event(newPhone)
                }
                OnboardingError.PhoneTaken -> update { it.copy(phoneError = R.string.err_phone_taken) }
                OnboardingError.InvalidPhone -> update { it.copy(phoneError = R.string.err_phone_invalid) }
                else -> update { it.copy(errorRes = R.string.err_generic) }
            }
        }
    }

    /** Kết quả từ bottom sheet OTP. */
    fun onAuthResult(verified: Boolean) {
        val user = _state.value?.user ?: return
        val phone = pendingPhone ?: return
        pendingPhone = null
        if (!verified) return
        update { it.copy(saving = true) }
        viewModelScope.launch {
            users.changePhone(user.uid, user.phoneNumber, phone)
                .onSuccess { writeFields(user.uid, pendingFields) }
                .onFailure { update { it.copy(saving = false, errorRes = R.string.err_generic) } }
        }
    }

    private fun writeFields(uid: String, fields: Map<String, Any?>) {
        if (fields.isEmpty()) return update { it.copy(saving = false, saved = true) }
        update { it.copy(saving = true) }
        viewModelScope.launch {
            users.updateFields(uid, fields)
                .onSuccess { update { it.copy(saving = false, saved = true) } }
                .onFailure { update { it.copy(saving = false, errorRes = R.string.err_generic) } }
        }
    }

    private fun update(change: (EditProfileState) -> EditProfileState) {
        _state.value = change(_state.value ?: EditProfileState())
    }
}
