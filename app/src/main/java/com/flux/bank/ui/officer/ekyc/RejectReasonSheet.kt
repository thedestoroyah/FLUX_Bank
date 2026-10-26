package com.flux.bank.ui.officer.ekyc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.activityViewModels
import com.flux.bank.R
import com.flux.bank.databinding.SheetRejectReasonBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.radiobutton.MaterialRadioButton

/** Bottom sheet chọn lý do từ chối eKYC (bắt buộc); "Khác…" mở ô nhập tự do. */
class RejectReasonSheet : BottomSheetDialogFragment() {

    private var _binding: SheetRejectReasonBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EkycReviewViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetRejectReasonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        RejectReason.entries.forEach { reason ->
            binding.groupReasons.addView(MaterialRadioButton(requireContext()).apply {
                id = View.generateViewId()
                tag = reason
                setText(reason.label)
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Flux_Body)
                minHeight = resources.getDimensionPixelSize(R.dimen.touch_min)
                buttonTintList = requireContext().getColorStateList(R.color.selector_radio)
            })
        }
        binding.groupReasons.setOnCheckedChangeListener { group, id ->
            binding.tilOther.isVisible = group.findViewById<View>(id)?.tag == RejectReason.OTHER
            binding.tvError.isVisible = false
        }
        binding.btnConfirm.setOnClickListener {
            val checked = binding.groupReasons.findViewById<View>(binding.groupReasons.checkedRadioButtonId)
            val error = viewModel.reject(checked?.tag as? RejectReason, binding.etOther.text?.toString().orEmpty())
            if (error == null) dismiss() else {
                binding.tvError.setText(error)
                binding.tvError.isVisible = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
