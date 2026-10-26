package com.flux.bank.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.data.model.User
import com.flux.bank.data.model.UserRole
import com.flux.bank.data.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data class NavigateToCustomer(val user: User) : AuthUiState()
    data class NavigateToOfficer(val user: User) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    fun login(email: String, pass: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            _uiState.value = AuthUiState.Error("Please enter your email.")
            return
        }

        if (pass.isEmpty()) {
            _uiState.value = AuthUiState.Error("Please enter your password.")
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            val loginResult = authRepository.login(trimmedEmail, pass)
            loginResult.onSuccess { firebaseUser ->
                val profileResult = authRepository.getUserProfile(firebaseUser.uid)
                profileResult.onSuccess { userProfile ->
                    when (userProfile.role) {
                        UserRole.CUSTOMER -> {
                            _uiState.value = AuthUiState.NavigateToCustomer(userProfile)
                        }
                        UserRole.BANK_OFFICER -> {
                            _uiState.value = AuthUiState.NavigateToOfficer(userProfile)
                        }
                    }
                }.onFailure { profileError ->
                    _uiState.value = AuthUiState.Error(
                        profileError.localizedMessage ?: "Failed to retrieve user profile."
                    )
                }
            }.onFailure { loginError ->
                _uiState.value = AuthUiState.Error(
                    loginError.localizedMessage ?: "Invalid email or password."
                )
            }
        }
    }

    fun loginWithUid(uid: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val profileResult = authRepository.getUserProfile(uid)
            profileResult.onSuccess { userProfile ->
                when (userProfile.role) {
                    UserRole.CUSTOMER -> {
                        _uiState.value = AuthUiState.NavigateToCustomer(userProfile)
                    }
                    UserRole.BANK_OFFICER -> {
                        _uiState.value = AuthUiState.NavigateToOfficer(userProfile)
                    }
                }
            }.onFailure { profileError ->
                _uiState.value = AuthUiState.Error(
                    profileError.localizedMessage ?: "Failed to authenticate biometric user."
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

