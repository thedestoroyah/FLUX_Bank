package com.flux.bank.ui.officer.rates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.databinding.SheetEditRateBinding
import com.flux.bank.ui.common.padAboveKeyboard
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Bottom sheet sửa một mức lãi suất; dùng chung ViewModel với [RatesFragment] (fragment cha). */
class EditRateSheet : BottomSheetDialogFragment() {

    private var _binding: SheetEditRateBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RatesViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private var submitted = false
    private val kind get() = RateKind.valueOf(requireArguments().getString(ARG_KIND)!!)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetEditRateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.padAboveKeyboard()
        val rates = viewModel.state.value?.rates
        binding.tvTitle.setText(if (kind == RateKind.SAVING) R.string.rates_edit_saving else R.string.rates_edit_mortgage)
        if (savedInstanceState == null && rates != null) {
            binding.etRate.setText(MoneyFormatter.formatRate(if (kind == RateKind.SAVING) rates.savingRate else rates.mortgageRate))
        }
        binding.etRate.doAfterTextChanged { renderPreview() }
        renderPreview()
        binding.btnSave.setOnClickListener {
            submitted = true
            viewModel.save(kind, binding.etRate.text?.toString().orEmpty())
        }
        // Không nghe `saved`: LiveData giữ sự kiện cũ nên lần mở sau sẽ tự đóng ngay
        viewModel.state.observe(viewLifecycleOwner) { s ->
            binding.btnSave.isEnabled = !s.saving
            if (submitted && !s.saving) {
                if (s.errorRes == null) dismiss() else submitted = false
            }
        }
    }

    /** Dòng ví dụ đổi theo số đang nhập; số sai thì hiện lỗi ngay dưới ô. */
    private fun renderPreview() {
        val preview = viewModel.examplePreview(kind, binding.etRate.text?.toString().orEmpty())
        binding.tilRate.error = if (preview == null) getString(R.string.err_rate_range) else null
        binding.tvExample.text = preview?.let {
            val money = MoneyFormatter.formatVnd(it)
            getString(if (kind == RateKind.SAVING) R.string.rates_example_saving else R.string.rates_example_mortgage, money)
        }.orEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "EditRateSheet"
        private const val ARG_KIND = "kind"

        fun show(fm: FragmentManager, kind: RateKind) {
            if (fm.findFragmentByTag(TAG) != null) return
            EditRateSheet().apply { arguments = bundleOf(ARG_KIND to kind.name) }.show(fm, TAG)
        }
    }
}
