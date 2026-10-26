package com.flux.bank.ui.officer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.flux.bank.data.model.User
import com.flux.bank.FluxApp
import com.flux.bank.data.model.KycRecord
import com.flux.bank.databinding.ActivityOfficerEkycReviewBinding
import com.flux.bank.utils.ImageUtils
import kotlinx.coroutines.launch

class OfficerEkycReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOfficerEkycReviewBinding
    private val kycRepository = FluxApp.instance.kycRepository
    private val pendingUsers = mutableListOf<Pair<User, KycRecord>>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfficerEkycReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        loadPendingQueue()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnApprove.setOnClickListener {
            submitDecision(isApproved = true)
        }

        binding.btnReject.setOnClickListener {
            submitDecision(isApproved = false)
        }
    }

    private fun loadPendingQueue() {
        setLoading(true)
        lifecycleScope.launch {
            val result = kycRepository.pendingQueue()
            setLoading(false)
            result.onSuccess { users ->
                pendingUsers.clear()
                pendingUsers.addAll(users)
                currentIndex = 0
                displayCurrentApplicant()
            }.onFailure { err ->
                Toast.makeText(this@OfficerEkycReviewActivity, "Failed to load queue: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                displayEmptyState()
            }
        }
    }

    private fun displayCurrentApplicant() {
        binding.tvQueueCount.text = "Queue: ${pendingUsers.size}"

        if (pendingUsers.isEmpty() || currentIndex >= pendingUsers.size) {
            displayEmptyState()
            return
        }

        binding.tvEmptyState.visibility = View.GONE
        binding.cardReviewItem.visibility = View.VISIBLE

        val (user, record) = pendingUsers[currentIndex]
        binding.tvCustomerName.text = user.fullName.ifEmpty { "Anonymous Customer" }
        binding.tvCustomerEmail.text = user.email.ifEmpty { "No email provided" }
        binding.tvCustomerUid.text = "UID: ${user.uid}"

        // Decode ID Card photo
        val idBitmap = ImageUtils.base64ToBitmap(record.idCardBase64)
        if (idBitmap != null) {
            binding.ivReviewIdCard.setImageBitmap(idBitmap)
        } else {
            binding.ivReviewIdCard.setImageResource(com.flux.bank.R.drawable.ic_id_card)
        }

        // Decode Face Selfie photo
        val faceBitmap = ImageUtils.base64ToBitmap(record.faceImageBase64)
        if (faceBitmap != null) {
            binding.ivReviewFace.setImageBitmap(faceBitmap)
        } else {
            binding.ivReviewFace.setImageResource(com.flux.bank.R.drawable.ic_face_scan)
        }
    }

    private fun displayEmptyState() {
        binding.tvQueueCount.text = "Queue: 0"
        binding.cardReviewItem.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
    }

    private fun submitDecision(isApproved: Boolean) {
        if (pendingUsers.isEmpty() || currentIndex >= pendingUsers.size) return
        val currentApplicant = pendingUsers[currentIndex].first

        setLoading(true)
        lifecycleScope.launch {
            val result = kycRepository.review(currentApplicant.uid, isApproved, null, FluxApp.instance.authRepository.currentUid().orEmpty())
            setLoading(false)
            result.onSuccess {
                val actionName = if (isApproved) "Approved" else "Declined"
                Toast.makeText(this@OfficerEkycReviewActivity, "Application $actionName for ${currentApplicant.fullName}", Toast.LENGTH_SHORT).show()
                pendingUsers.removeAt(currentIndex)
                displayCurrentApplicant()
            }.onFailure { err ->
                Toast.makeText(this@OfficerEkycReviewActivity, "Action failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnApprove.isEnabled = !isLoading
        binding.btnReject.isEnabled = !isLoading
    }
}

