package com.flux.bank.ui.officer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.databinding.ActivityOfficerMainBinding
import com.flux.bank.ui.auth.LoginActivity

class OfficerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOfficerMainBinding
    private val authRepository = AuthRepository()

    companion object {
        const val EXTRA_USER_UID = "extra_user_uid"
        const val EXTRA_USER_NAME = "extra_user_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfficerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: "Officer"
        val userUid = intent.getStringExtra(EXTRA_USER_UID) ?: "-"

        binding.tvOfficerGreeting.text = "Welcome, $userName"
        binding.tvOfficerUid.text = "UID: $userUid"

        val currentUser = authRepository.getCurrentUser()
        if (currentUser?.email != null) {
            binding.tvOfficerEmail.text = currentUser.email
        }

        binding.cardTaskEkyc.setOnClickListener {
            val intent = Intent(this, OfficerEkycReviewActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            authRepository.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}

