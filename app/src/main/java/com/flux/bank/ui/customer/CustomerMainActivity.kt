package com.flux.bank.ui.customer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.flux.bank.FluxApp
import com.flux.bank.databinding.ActivityCustomerMainBinding
import com.flux.bank.ui.auth.LoginActivity

import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.flux.bank.R
import com.flux.bank.data.model.KycStatus
import kotlinx.coroutines.launch

class CustomerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerMainBinding
    private val authRepository = FluxApp.instance.authRepository
    private var currentUid: String = ""

    companion object {
        const val EXTRA_USER_UID = "extra_user_uid"
        const val EXTRA_USER_NAME = "extra_user_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: "Customer"
        currentUid = intent.getStringExtra(EXTRA_USER_UID) ?: authRepository.currentUid().orEmpty()

        binding.tvCustomerGreeting.text = "Welcome, $userName"
        binding.tvUserUid.text = "UID: $currentUid"

        authRepository.currentEmail()?.let { binding.tvCustomerEmail.text = it }

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        if (currentUid.isNotEmpty()) {
            refreshKycStatus(currentUid)
        }
    }

    private fun setupListeners() {
        binding.btnStartEkyc.setOnClickListener {
            launchEkycScreen()
        }

        binding.cardEkycBanner.setOnClickListener {
            launchEkycScreen()
        }

        binding.btnLogout.setOnClickListener {
            authRepository.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun launchEkycScreen() {
        val intent = Intent(this, EkycActivity::class.java)
        startActivity(intent)
    }

    private fun refreshKycStatus(uid: String) {
        lifecycleScope.launch {
            val result = FluxApp.instance.userRepository.getUser(uid)
            result.onSuccess { user ->
                when (user.kycStatus) {
                    KycStatus.NOT_SUBMITTED -> {
                        binding.tvEkycTitle.text = "Identity Verification (eKYC)"
                        binding.tvEkycSubtitle.text = "Status: Not Verified • Tap to apply"
                        binding.ivEkycStatusIcon.setColorFilter(ContextCompat.getColor(this@CustomerMainActivity, R.color.flux_accent_blue))
                        binding.btnStartEkyc.text = "Verify →"
                        binding.btnStartEkyc.isEnabled = true
                        binding.cardEkycBanner.isEnabled = true
                    }
                    KycStatus.PENDING -> {
                        binding.tvEkycTitle.text = "Identity Verification Under Review"
                        binding.tvEkycSubtitle.text = "Status: Pending Review • Verification in progress"
                        binding.ivEkycStatusIcon.setColorFilter(ContextCompat.getColor(this@CustomerMainActivity, R.color.flux_warning))
                        binding.btnStartEkyc.text = "Pending"
                        binding.btnStartEkyc.isEnabled = false
                        binding.cardEkycBanner.isEnabled = true
                    }
                    KycStatus.VERIFIED -> {
                        binding.tvEkycTitle.text = "Identity Verified (eKYC)"
                        binding.tvEkycSubtitle.text = "Status: Verified ✓ • Full banking access granted"
                        binding.ivEkycStatusIcon.setColorFilter(ContextCompat.getColor(this@CustomerMainActivity, R.color.flux_emerald))
                        binding.btnStartEkyc.text = "Verified"
                        binding.btnStartEkyc.isEnabled = false
                        binding.cardEkycBanner.isEnabled = false
                    }
                    KycStatus.REJECTED -> {
                        binding.tvEkycTitle.text = "Verification Rejected"
                        binding.tvEkycSubtitle.text = "Status: Rejected ✕ • Tap to re-submit identity documents"
                        binding.ivEkycStatusIcon.setColorFilter(ContextCompat.getColor(this@CustomerMainActivity, R.color.flux_error))
                        binding.btnStartEkyc.text = "Retry →"
                        binding.btnStartEkyc.isEnabled = true
                        binding.cardEkycBanner.isEnabled = true
                    }
                }
            }
        }
    }
}

