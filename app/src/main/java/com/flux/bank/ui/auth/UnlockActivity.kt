package com.flux.bank.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.flux.bank.FluxApp
import com.flux.bank.databinding.ActivityUnlockBinding
import com.flux.bank.utils.BiometricHelper
import com.google.android.material.snackbar.Snackbar

/**
 * Màn mở khoá bằng sinh trắc học.
 * - COLD_START: mở khoá xong thì đọc hồ sơ và vào màn chính.
 * - RESUME: phủ lên màn đang dùng sau khi app ở nền ≥ 60 giây; mở khoá xong chỉ cần đóng lại.
 */
class UnlockActivity : AppCompatActivity() {

    enum class Mode { COLD_START, RESUME }

    private lateinit var binding: ActivityUnlockBinding
    private val viewModel: AuthViewModel by viewModels()
    private val app get() = FluxApp.instance
    private val mode by lazy { Mode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: Mode.COLD_START.name) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnlockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        app.appLockObserver.markColdStartHandled()

        val uid = app.authRepository.currentUid()
        if (uid == null) {
            startActivity(LoginActivity.intent(this))
            finish()
            return
        }
        val name = app.sessionManager.displayName(uid).orEmpty()
        binding.tvName.text = name
        binding.tvAvatar.text = initials(name)

        binding.btnFingerprint.setOnClickListener { promptBiometric(uid) }
        binding.btnOtherAccount.setOnClickListener { RoleRouter.signOut(this) }
        // Không cho bấm back để vượt qua màn khoá
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })
        viewModel.uiState.observe(this) { state ->
            binding.progress.visibility = if (state is AuthUiState.Loading) View.VISIBLE else View.GONE
            when (state) {
                is AuthUiState.NavigateToChangePassword -> {
                    startActivity(ChangePasswordActivity.intent(this, forced = true)); finish()
                }
                is AuthUiState.NavigateToCustomer -> RoleRouter.start(this, state.user)
                is AuthUiState.NavigateToOfficer -> RoleRouter.start(this, state.user)
                is AuthUiState.Error -> Snackbar.make(binding.root, state.messageRes, Snackbar.LENGTH_LONG).show()
                else -> Unit
            }
        }
        if (savedInstanceState == null) promptBiometric(uid)
    }

    override fun onResume() {
        super.onResume()
        app.appLockObserver.lockScreenVisible = true
    }

    override fun onPause() {
        super.onPause()
        app.appLockObserver.lockScreenVisible = false
    }

    private fun promptBiometric(uid: String) {
        BiometricHelper.prompt(
            this,
            onSuccess = {
                app.appLockObserver.onUnlocked()
                if (mode == Mode.RESUME) finish() else viewModel.loadSession(uid)
            },
            onError = { message -> Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show() }
        )
    }

    private fun initials(name: String): String =
        name.split(" ").filter { it.isNotBlank() }.let { parts ->
            (parts.dropLast(1).lastOrNull()?.take(1).orEmpty() + parts.lastOrNull()?.take(1).orEmpty()).uppercase()
        }

    companion object {
        private const val EXTRA_MODE = "extra_mode"

        fun intent(context: Context, mode: Mode): Intent =
            Intent(context, UnlockActivity::class.java).putExtra(EXTRA_MODE, mode.name)
    }
}
