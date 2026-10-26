package com.flux.bank.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.flux.bank.data.model.UserRole
import com.flux.bank.databinding.ActivityLoginBinding
import com.flux.bank.ui.customer.CustomerMainActivity
import com.flux.bank.ui.officer.OfficerMainActivity
import com.flux.bank.utils.BiometricHelper
import com.flux.bank.utils.SessionManager
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Pre-fill last known email if available
        sessionManager.getSavedEmail()?.let { savedEmail ->
            if (savedEmail.isNotEmpty()) {
                binding.etEmail.setText(savedEmail)
            }
        }

        // Adjust biometric button visibility based on hardware support
        if (!BiometricHelper.canAuthenticate(this)) {
            binding.btnBiometricLogin.visibility = View.GONE
        }

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()
            viewModel.login(email, password)
        }

        binding.btnBiometricLogin.setOnClickListener {
            val savedUid = sessionManager.getSavedUid()
            if (savedUid.isNullOrEmpty()) {
                Snackbar.make(
                    binding.root,
                    "Please sign in with password first to enable Biometric Quick Sign-In.",
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                BiometricHelper.showBiometricPrompt(
                    activity = this,
                    title = "FLUX Biometric Authentication",
                    subtitle = "Verify with Fingerprint or Face ID",
                    onSuccess = {
                        viewModel.loginWithUid(savedUid)
                    },
                    onError = { errorMessage ->
                        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is AuthUiState.Idle -> {
                    setLoading(false)
                }
                is AuthUiState.Loading -> {
                    setLoading(true)
                }
                is AuthUiState.Success -> {
                    setLoading(false)
                }
                is AuthUiState.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                        .setAction("Dismiss") { }
                        .show()
                }
                is AuthUiState.NavigateToCustomer -> {
                    setLoading(false)
                    sessionManager.saveUserSession(
                        uid = state.user.uid,
                        email = state.user.email,
                        role = state.user.role.name,
                        fullName = state.user.fullName
                    )
                    Toast.makeText(this, "Logged in as Customer", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, CustomerMainActivity::class.java).apply {
                        putExtra(CustomerMainActivity.EXTRA_USER_UID, state.user.uid)
                        putExtra(CustomerMainActivity.EXTRA_USER_NAME, state.user.fullName.ifEmpty { "Customer" })
                    }
                    startActivity(intent)
                    finish()
                }
                is AuthUiState.NavigateToOfficer -> {
                    setLoading(false)
                    sessionManager.saveUserSession(
                        uid = state.user.uid,
                        email = state.user.email,
                        role = state.user.role.name,
                        fullName = state.user.fullName
                    )
                    Toast.makeText(this, "Logged in as Bank Officer", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, OfficerMainActivity::class.java).apply {
                        putExtra(OfficerMainActivity.EXTRA_USER_UID, state.user.uid)
                        putExtra(OfficerMainActivity.EXTRA_USER_NAME, state.user.fullName.ifEmpty { "Bank Officer" })
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnBiometricLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }
}

