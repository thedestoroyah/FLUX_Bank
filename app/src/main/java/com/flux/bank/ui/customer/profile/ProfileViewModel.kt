package com.flux.bank.ui.customer.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.data.model.User
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.UserRepository
import kotlinx.coroutines.launch

data class ProfileState(val user: User? = null, val biometricEnabled: Boolean = false)

/**
 * Tab Cá nhân: hiển thị hồ sơ (che bớt email/CCCD) và bật/tắt đăng nhập sinh trắc học.
 *
 * @param setLocalBiometric lưu cờ sinh trắc học cục bộ (SessionManager) — cần cho quyết định khoá app lúc mở.
 */
class ProfileViewModel(
    private val auth: AuthRepository = FluxApp.instance.authRepository,
    private val users: UserRepository = FluxApp.instance.userRepository,
    private val setLocalBiometric: (String, Boolean) -> Unit = { uid, on ->
        FluxApp.instance.sessionManager.setBiometricEnabled(uid, on)
    }
) : ViewModel() {

    private val _state = MutableLiveData(ProfileState())
    val state: LiveData<ProfileState> = _state

    fun load() {
        val uid = auth.currentUid() ?: return
        viewModelScope.launch {
            users.getUser(uid).onSuccess { user ->
                _state.value = ProfileState(user, user.biometricEnabled)
            }
        }
    }

    /** Gọi sau khi BiometricPrompt thành công (khi bật) hoặc ngay khi tắt. */
    fun setBiometric(enabled: Boolean) {
        val uid = auth.currentUid() ?: return
        setLocalBiometric(uid, enabled)
        _state.value = (_state.value ?: ProfileState()).copy(biometricEnabled = enabled)
        viewModelScope.launch { users.updateFields(uid, mapOf("biometricEnabled" to enabled)) }
    }

    companion object {
        /** "minhanh@gmail.com" -> "min•••@gmail.com". */
        fun maskEmail(email: String): String {
            val at = email.indexOf('@')
            if (at <= 0) return email
            return email.take(minOf(3, at)) + "•••" + email.substring(at)
        }

        /** "079203001234" -> "•••• •••• 1234". */
        fun maskNationalId(id: String): String = "•••• •••• " + id.takeLast(4)
    }
}
