package com.flux.bank.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.core.widget.doAfterTextChanged
import com.flux.bank.R
import com.flux.bank.databinding.ActivityChangePasswordBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Đổi mật khẩu. Chế độ bắt buộc (lần đầu đăng nhập bằng mật khẩu tạm): quay lại = đăng xuất,
 * xong thì vào màn chính. Chế độ tự nguyện (từ tab Cá nhân): xong thì đóng.
 */
class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private val viewModel: ChangePasswordViewModel by viewModels()
    private val forced by lazy { intent.getBooleanExtra(EXTRA_FORCED, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.header.tvHeaderTitle.setText(R.string.chpw_title)
        binding.tvCaption.setText(if (forced) R.string.chpw_caption_forced else R.string.chpw_caption_voluntary)
        binding.header.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (forced) RoleRouter.signOut(this@ChangePasswordActivity) else finish()
            }
        })

        binding.etNew.doAfterTextChanged { renderRules() }
        binding.etConfirm.doAfterTextChanged { renderRules() }
        binding.btnSave.setOnClickListener { viewModel.submit(newPassword(), confirm()) }
        renderRules()

        viewModel.state.observe(this) { state ->
            binding.btnSave.isEnabled = state !is ChangePasswordState.Loading
            when (state) {
                is ChangePasswordState.Error -> Snackbar.make(binding.root, state.messageRes, Snackbar.LENGTH_LONG).show()
                is ChangePasswordState.Done -> {
                    Toast.makeText(this, R.string.chpw_done, Toast.LENGTH_SHORT).show()
                    val user = state.user
                    if (forced && user != null) RoleRouter.start(this, user) else finish()
                }
                else -> Unit
            }
        }
    }

    private fun newPassword() = binding.etNew.text?.toString().orEmpty()
    private fun confirm() = binding.etConfirm.text?.toString().orEmpty()

    private fun renderRules() {
        val rules = viewModel.rules(newPassword(), confirm())
        mark(binding.tvRuleLength, rules.lengthOk)
        mark(binding.tvRuleMix, rules.lettersAndDigits)
        mark(binding.tvRuleMatch, rules.matches)
    }

    private fun mark(view: TextView, ok: Boolean) {
        val color = ContextCompat.getColor(this, if (ok) R.color.flux_emerald else R.color.flux_text_tertiary)
        TextViewCompat.setCompoundDrawableTintList(view, android.content.res.ColorStateList.valueOf(color))
        view.setTextColor(ContextCompat.getColor(this, if (ok) R.color.flux_text_primary else R.color.flux_text_tertiary))
    }

    companion object {
        private const val EXTRA_FORCED = "extra_forced"

        fun intent(context: Context, forced: Boolean): Intent =
            Intent(context, ChangePasswordActivity::class.java).putExtra(EXTRA_FORCED, forced)
    }
}
