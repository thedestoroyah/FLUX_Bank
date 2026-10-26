package com.flux.bank.ui.auth

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
import kotlinx.coroutines.launch

/** Trạng thái từng quy tắc mật khẩu, để checklist trên màn hình đổi màu theo thời gian thực. */
data class PasswordRules(val lengthOk: Boolean, val lettersAndDigits: Boolean, val matches: Boolean)

sealed class ChangePasswordState {
    object Idle : ChangePasswordState()
    object Loading : ChangePasswordState()
    data class Error(@StringRes val messageRes: Int) : ChangePasswordState()
    data class Done(val user: User?) : ChangePasswordState()
}

/** Đổi mật khẩu (bắt buộc lần đầu hoặc tự nguyện); xong thì tắt cờ `mustChangePassword`. */
class ChangePasswordViewModel(
    private val auth: AuthRepository = FluxApp.instance.authRepository,
    private val users: UserRepository = FluxApp.instance.userRepository
) : ViewModel() {

    private val _state = MutableLiveData<ChangePasswordState>(ChangePasswordState.Idle)
    val state: LiveData<ChangePasswordState> = _state

    fun rules(newPassword: String, confirm: String) = PasswordRules(
        lengthOk = newPassword.length >= 8,
        lettersAndDigits = newPassword.any { it.isLetter() } && newPassword.any { it.isDigit() },
        matches = newPassword.isNotEmpty() && newPassword == confirm
    )

    fun submit(newPassword: String, confirm: String) {
        if (newPassword != confirm) return error(R.string.err_password_mismatch)
        if (!Validators.isStrongEnoughPassword(newPassword)) return error(R.string.err_password_weak)
        val uid = auth.currentUid() ?: return error(R.string.err_generic)
        _state.value = ChangePasswordState.Loading
        viewModelScope.launch {
            auth.changePassword(newPassword).onFailure { e ->
                // Firebase đòi đăng nhập lại nếu phiên đã cũ
                val res = if (e.javaClass.simpleName.contains("RecentLogin")) R.string.err_recent_login else R.string.err_generic
                return@launch error(res)
            }
            users.updateFields(uid, mapOf("mustChangePassword" to false))
            _state.value = ChangePasswordState.Done(users.getUser(uid).getOrNull())
        }
    }

    private fun error(@StringRes res: Int) {
        _state.value = ChangePasswordState.Error(res)
    }
}
