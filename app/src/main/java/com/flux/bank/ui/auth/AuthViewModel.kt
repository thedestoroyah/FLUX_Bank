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
import com.flux.bank.data.model.UserRole
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.UserRepository
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Error(@StringRes val messageRes: Int) : AuthUiState()
    data class Info(@StringRes val messageRes: Int) : AuthUiState()
    data class NavigateToChangePassword(val user: User) : AuthUiState()
    data class NavigateToCustomer(val user: User) : AuthUiState()
    data class NavigateToOfficer(val user: User) : AuthUiState()
}

/** Đăng nhập bằng mật khẩu, khôi phục phiên có sẵn, quên mật khẩu; quyết định màn hình tiếp theo theo hồ sơ. */
class AuthViewModel(
    private val authRepository: AuthRepository = FluxApp.instance.authRepository,
    private val userRepository: UserRepository = FluxApp.instance.userRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    fun login(email: String, password: String) {
        val trimmed = email.trim()
        when {
            trimmed.isEmpty() -> return fail(R.string.err_email_required)
            password.isEmpty() -> return fail(R.string.err_password_required)
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.login(trimmed, password)
                .onSuccess { uid -> loadAndRoute(uid) }
                .onFailure { fail(R.string.err_login_failed) }
        }
    }

    /** Đã có phiên Firebase (mở app hoặc vừa mở khoá): đọc hồ sơ rồi điều hướng. */
    fun loadSession(uid: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch { loadAndRoute(uid) }
    }

    fun forgotPassword(email: String) {
        if (!Validators.isValidEmail(email)) return fail(R.string.err_email_invalid)
        viewModelScope.launch {
            authRepository.sendPasswordReset(email.trim())
                .onSuccess { _uiState.value = AuthUiState.Info(R.string.msg_reset_email_sent) }
                .onFailure { fail(R.string.err_generic) }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private suspend fun loadAndRoute(uid: String) {
        userRepository.getUser(uid)
            .onSuccess { user -> _uiState.value = routeFor(user) }
            .onFailure {
                authRepository.logout()
                fail(R.string.err_profile_missing)
            }
    }

    private fun routeFor(user: User): AuthUiState = when {
        user.mustChangePassword -> AuthUiState.NavigateToChangePassword(user)
        user.role == UserRole.BANK_OFFICER -> AuthUiState.NavigateToOfficer(user)
        else -> AuthUiState.NavigateToCustomer(user)
    }

    private fun fail(@StringRes res: Int) {
        _uiState.value = AuthUiState.Error(res)
    }
}
