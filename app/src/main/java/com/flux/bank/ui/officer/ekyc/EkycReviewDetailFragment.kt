package com.flux.bank.ui.officer.ekyc

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.flux.bank.R
import com.flux.bank.databinding.FragmentEkycReviewDetailBinding
import com.flux.bank.utils.ImageUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Chi tiết một hồ sơ eKYC: ảnh CCCD + chân dung cạnh thông tin khách, nút Duyệt / Từ chối. */
class EkycReviewDetailFragment : Fragment() {

    private var _binding: FragmentEkycReviewDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EkycReviewViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEkycReviewDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.tvHeaderTitle.setText(R.string.review_title)
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnApprove.setOnClickListener { viewModel.approve() }
        binding.btnReject.setOnClickListener { RejectReasonSheet().show(childFragmentManager, "reject") }
        viewModel.message.observe(viewLifecycleOwner) { event ->
            event.consume()?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
        viewModel.selected.observe(viewLifecycleOwner) { selected ->
            // Đã duyệt/từ chối xong (hoặc mất lựa chọn): quay lại hàng đợi
            val (user, record) = selected ?: return@observe parentFragmentManager.popBackStack()
            binding.tvName.text = user.fullName
            val time = SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale.forLanguageTag("vi")).format(Date(record.submittedAt))
            binding.tvSubmitted.text = getString(R.string.review_submitted_at, time)
            binding.tvAssisted.isVisible = EkycReviewViewModel.isAssisted(record)
            binding.tvInfo.text = getString(R.string.review_info, user.fullName, user.nationalId, user.phoneNumber)
            show(binding.ivId, ImageUtils.base64ToBitmap(record.idCardBase64))
            show(binding.ivSelfie, ImageUtils.base64ToBitmap(record.faceImageBase64))
        }
    }

    private fun show(view: ImageView, bitmap: Bitmap?) {
        bitmap ?: return
        view.setImageBitmap(bitmap)
        view.setOnClickListener {
            val full = ImageView(requireContext()).apply {
                setImageBitmap(bitmap)
                adjustViewBounds = true
            }
            MaterialAlertDialogBuilder(requireContext()).setView(full).setPositiveButton(R.string.action_ok, null).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
