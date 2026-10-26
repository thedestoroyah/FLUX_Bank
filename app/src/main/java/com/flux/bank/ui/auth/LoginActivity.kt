package com.flux.bank.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.flux.bank.FluxApp
import com.flux.bank.databinding.ActivityLoginBinding
import com.flux.bank.utils.BiometricHelper
import com.google.android.material.snackbar.Snackbar

/** Màn đăng nhập; cũng là điểm vào app: nếu đã có phiên thì khoá (sinh trắc học) hoặc vào thẳng. */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()
    private val app get() = FluxApp.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Màn đăng nhập tự quyết định khoá lúc mở app, AppLockObserver không cần khoá thêm
        app.appLockObserver.markColdStartHandled()

        if (savedInstanceState == null && routeExistingSession()) return

        app.sessionManager.lastEmail()?.let { binding.etEmail.setText(it) }
        binding.btnLogin.setOnClickListener { submit() }
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_DONE).also { if (it) submit() }
        }
        binding.btnForgot.setOnClickListener { viewModel.forgotPassword(binding.etEmail.text.toString()) }
        observe()
    }

    /** Có phiên Firebase: khoá nếu cần, không thì đọc hồ sơ và điều hướng. Trả về true nếu đã chuyển màn. */
    private fun routeExistingSession(): Boolean {
        val uid = app.authRepository.currentUid() ?: return false
        val mustLock = app.lockPolicy.onColdStart(
            hasSession = true,
            biometricEnabled = app.sessionManager.isBiometricEnabled(uid),
            biometricAvailable = BiometricHelper.canAuthenticate(this)
        )
        if (mustLock) {
            startActivity(UnlockActivity.intent(this, UnlockActivity.Mode.COLD_START))
            finish()
            return true
        }
        observe()
        viewModel.loadSession(uid)
        return false
    }

    private fun submit() = viewModel.login(binding.etEmail.text.toString(), binding.etPassword.text.toString())

    private fun observe() {
        if (viewModel.uiState.hasObservers()) return
        viewModel.uiState.observe(this) { state ->
            setLoading(state is AuthUiState.Loading)
            when (state) {
                is AuthUiState.Error -> snack(getString(state.messageRes))
                is AuthUiState.Info -> snack(getString(state.messageRes))
                is AuthUiState.NavigateToChangePassword -> {
                    rememberEmail()
                    startActivity(ChangePasswordActivity.intent(this, forced = true))
                    finish()
                }
                is AuthUiState.NavigateToCustomer -> { rememberEmail(); RoleRouter.start(this, state.user) }
                is AuthUiState.NavigateToOfficer -> { rememberEmail(); RoleRouter.start(this, state.user) }
                else -> Unit
            }
        }
    }

    private fun rememberEmail() {
        app.authRepository.currentEmail()?.let { app.sessionManager.saveLastEmail(it) }
    }

    private fun snack(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        viewModel.resetState()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.text = if (loading) "" else getString(com.flux.bank.R.string.login_submit)
        binding.btnLogin.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
    }

    companion object {
        fun intent(activity: android.content.Context) =
            Intent(activity, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
