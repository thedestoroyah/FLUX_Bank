package com.flux.bank.ui.customer

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.flux.bank.data.model.KycStatus
import com.flux.bank.FluxApp
import com.flux.bank.databinding.ActivityEkycBinding
import com.flux.bank.utils.ImageUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class EkycActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEkycBinding
    private val viewModel: EkycViewModel by viewModels()
    private val authRepository = FluxApp.instance.authRepository

    private var idCardBitmap: Bitmap? = null
    private var faceBitmap: Bitmap? = null
    private var currentCaptureTarget: CaptureTarget? = null

    private enum class CaptureTarget {
        ID_CARD,
        FACE_SELFIE
    }

    // Camera launcher returning a thumbnail Bitmap directly
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            when (currentCaptureTarget) {
                CaptureTarget.ID_CARD -> {
                    idCardBitmap = bitmap
                    binding.ivIdPreview.setImageBitmap(bitmap)
                }
                CaptureTarget.FACE_SELFIE -> {
                    faceBitmap = bitmap
                    binding.ivFacePreview.setImageBitmap(bitmap)
                }
                null -> {}
            }
        }
    }

    // Permission launcher for Camera
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takePictureLauncher.launch(null)
        } else {
            Toast.makeText(this, "Camera permission is required to capture eKYC photos.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEkycBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
        loadCurrentKycStatus()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCaptureId.setOnClickListener {
            currentCaptureTarget = CaptureTarget.ID_CARD
            launchCamera()
        }

        binding.btnCaptureFace.setOnClickListener {
            currentCaptureTarget = CaptureTarget.FACE_SELFIE
            launchCamera()
        }

        binding.btnSubmitEkyc.setOnClickListener {
            val uid = authRepository.currentUid().orEmpty()
            viewModel.submitEkyc(uid, idCardBitmap, faceBitmap)
        }
    }

    private fun launchCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePictureLauncher.launch(null)
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is EkycUiState.Idle -> {
                    setLoading(false)
                }
                is EkycUiState.Loading -> {
                    setLoading(true)
                }
                is EkycUiState.Success -> {
                    setLoading(false)
                    binding.tvKycStatusBadge.text = "Status: Pending Officer Review"
                    binding.tvKycStatusDesc.text = "Your documents have been encrypted and submitted. A bank officer will review them shortly."
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                is EkycUiState.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                        .setAction("OK") { }
                        .show()
                }
            }
        }
    }

    private fun loadCurrentKycStatus() {
        val uid = authRepository.currentUid()
        if (uid != null) {
            // Load existing KYC status and photos if any
            lifecycleScopeLaunch {
                val profileResult = FluxApp.instance.userRepository.getUser(uid)
                profileResult.onSuccess { user ->
                    when (user.kycStatus) {
                        KycStatus.VERIFIED -> {
                            binding.tvKycStatusBadge.text = "Status: Identity Verified"
                            binding.tvKycStatusDesc.text = "Your account is officially verified. 2FA biometrics and high-value limits are unlocked."
                            binding.btnSubmitEkyc.isEnabled = false
                            binding.btnSubmitEkyc.text = "Verification Complete"
                        }
                        KycStatus.PENDING -> {
                            binding.tvKycStatusBadge.text = "Status: Pending Officer Review"
                            binding.tvKycStatusDesc.text = "Your documents are currently queued for inspection by an authorized bank officer."
                        }
                        KycStatus.REJECTED -> {
                            binding.tvKycStatusBadge.text = "Status: Verification Declined"
                            binding.tvKycStatusDesc.text = "Your submitted documents were not clear. Please recapture your ID and face selfie."
                        }
                        else -> {
                            binding.tvKycStatusBadge.text = "Status: Not Submitted"
                            binding.tvKycStatusDesc.text = "Please capture your National ID and Face selfie to complete verification."
                        }
                    }

                    // Display previous images if available
                    val record = FluxApp.instance.kycRepository.get(uid).getOrNull()
                    record?.idCardBase64?.let { base64 ->
                        ImageUtils.base64ToBitmap(base64)?.let { bmp ->
                            idCardBitmap = bmp
                            binding.ivIdPreview.setImageBitmap(bmp)
                        }
                    }
                    record?.faceImageBase64?.let { base64 ->
                        ImageUtils.base64ToBitmap(base64)?.let { bmp ->
                            faceBitmap = bmp
                            binding.ivFacePreview.setImageBitmap(bmp)
                        }
                    }
                }
            }
        }
    }

    private fun lifecycleScopeLaunch(block: suspend () -> Unit) {
        lifecycleScope.launch {
            block()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSubmitEkyc.isEnabled = !isLoading
        binding.btnCaptureId.isEnabled = !isLoading
        binding.btnCaptureFace.isEnabled = !isLoading
    }
}
