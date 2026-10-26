package com.flux.bank.ui.ekyc

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.flux.bank.R
import com.flux.bank.data.model.KycStatus
import com.flux.bank.databinding.FragmentEkycCaptureBinding
import com.flux.bank.utils.ImageUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Chụp CCCD + chân dung (độ phân giải đầy đủ qua app camera) và nộp eKYC cho [ARG_TARGET].
 * Dùng chung cho khách tự nộp và nhân viên chụp hộ ([ARG_SUBMITTED_BY] khác [ARG_TARGET]).
 * Nộp xong: phát fragment result [RESULT_SUBMITTED] và tự đóng.
 */
class EkycCaptureFragment : Fragment() {

    private enum class Target { ID, SELFIE }

    private var _binding: FragmentEkycCaptureBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EkycViewModel by viewModels()
    private val targetUid by lazy { requireArguments().getString(ARG_TARGET)!! }
    private val submittedBy by lazy { requireArguments().getString(ARG_SUBMITTED_BY)!! }
    private var pendingTarget: Target? = null
    private var pendingUri: Uri? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val target = pendingTarget
        val uri = pendingUri
        if (saved && target != null && uri != null) onPhotoTaken(target, uri)
    }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingTarget?.let(::launchCamera)
        else Snackbar.make(binding.root, R.string.ekyc_camera_denied, Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEkycCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.tvHeaderTitle.setText(R.string.ekyc_title)
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnCaptureId.setOnClickListener { capture(Target.ID) }
        binding.btnCaptureSelfie.setOnClickListener { capture(Target.SELFIE) }
        binding.btnSubmit.setOnClickListener { viewModel.submit(targetUid, submittedBy) }
        viewModel.state.observe(viewLifecycleOwner, ::render)
        if (savedInstanceState == null) viewModel.load(targetUid)
    }

    private fun render(state: EkycUiState) {
        renderBanner(state)
        binding.btnSubmit.isEnabled = state.canSubmit
        binding.btnSubmit.text = if (state.loading) "" else getString(R.string.ekyc_submit)
        binding.progress.isVisible = state.loading
        binding.btnCaptureId.setText(if (state.hasIdPhoto) R.string.ekyc_recapture else R.string.ekyc_capture)
        binding.btnCaptureSelfie.setText(if (state.hasSelfie) R.string.ekyc_recapture else R.string.ekyc_capture)
        state.errorRes?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        if (state.submitted) {
            Snackbar.make(binding.root, R.string.ekyc_submitted, Snackbar.LENGTH_SHORT).show()
            setFragmentResult(RESULT_SUBMITTED, bundleOf())
            parentFragmentManager.popBackStack()
        }
    }

    private fun renderBanner(state: EkycUiState) {
        val banner = binding.banner
        banner.isVisible = state.status != KycStatus.NOT_SUBMITTED
        val (title, body, color) = when (state.status) {
            KycStatus.REJECTED -> Triple(
                getString(R.string.ekyc_rejected_title),
                getString(R.string.ekyc_rejected_reason, state.rejectReason.orEmpty()),
                R.color.flux_error_dark
            )
            KycStatus.PENDING -> Triple(getString(R.string.ekyc_pending_banner), "", R.color.flux_warning_dark)
            else -> Triple(getString(R.string.ekyc_verified_banner), "", R.color.flux_emerald_dark)
        }
        val bg = when (state.status) {
            KycStatus.REJECTED -> R.color.flux_error_bg
            KycStatus.PENDING -> R.color.flux_warning_bg
            else -> R.color.flux_emerald_light
        }
        val tint = ContextCompat.getColor(requireContext(), color)
        banner.backgroundTintList = ContextCompat.getColorStateList(requireContext(), bg)
        binding.tvBannerTitle.text = title
        binding.tvBannerTitle.setTextColor(tint)
        binding.tvBannerBody.text = body
        binding.tvBannerBody.isVisible = body.isNotEmpty()
        binding.tvBannerBody.setTextColor(tint)
        binding.ivBanner.setImageResource(if (state.status == KycStatus.VERIFIED) R.drawable.ic_shield_check else R.drawable.ic_warning)
        ImageViewCompat.setImageTintList(binding.ivBanner, ColorStateList.valueOf(tint))
    }

    private fun capture(target: Target) {
        pendingTarget = target
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera(target)
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera(target: Target) {
        val dir = File(requireContext().cacheDir, "ekyc").apply { mkdirs() }
        val file = File(dir, "${target.name.lowercase()}.jpg")
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
        pendingUri = uri
        takePicture.launch(uri)
    }

    private fun onPhotoTaken(target: Target, uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                ImageUtils.decodeSampled(requireContext(), uri, BitmapPhoto.MAX_DIM)
            } ?: return@launch
            val photo = BitmapPhoto(bitmap, requireContext())
            when (target) {
                Target.ID -> {
                    binding.ivId.imageTintList = null
                    binding.ivId.setPadding(0, 0, 0, 0)
                    binding.ivId.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    binding.ivId.setImageBitmap(bitmap)
                    viewModel.setIdPhoto(photo)
                }
                Target.SELFIE -> {
                    binding.ivSelfie.imageTintList = null
                    binding.ivSelfie.setPadding(0, 0, 0, 0)
                    binding.ivSelfie.setImageBitmap(bitmap)
                    viewModel.setSelfie(photo)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val RESULT_SUBMITTED = "ekyc_submitted"
        private const val ARG_TARGET = "target_uid"
        private const val ARG_SUBMITTED_BY = "submitted_by"

        fun newInstance(targetUid: String, submittedBy: String) = EkycCaptureFragment().apply {
            arguments = bundleOf(ARG_TARGET to targetUid, ARG_SUBMITTED_BY to submittedBy)
        }
    }
}
