package com.flux.bank.ui.officer.customers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.data.model.AccountType
import com.flux.bank.databinding.FragmentOpenAccountBinding
import com.flux.bank.databinding.ItemFilterChipBinding
import com.flux.bank.ui.common.AmountTextWatcher
import com.flux.bank.ui.common.setKeyValueRows
import com.flux.bank.ui.customer.home.HomeViewModel

/** Nhân viên mở tài khoản Tiết kiệm hoặc khoản vay cho một khách (mockup màn 7). */
class OpenAccountFragment : Fragment() {

    private var _binding: FragmentOpenAccountBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OpenAccountViewModel by viewModels()
    private val uid by lazy { requireArguments().getString(ARG_UID)!! }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOpenAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.tvHeaderTitle.setText(R.string.open_account_title)
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.tvCustomer.text = getString(
            R.string.open_account_customer, requireArguments().getString(ARG_NAME), requireArguments().getString(ARG_PHONE)
        )
        binding.chipSaving.isChecked = true
        binding.groupType.setOnCheckedStateChangeListener { _, ids ->
            viewModel.selectType(if (ids.firstOrNull() == R.id.chipMortgage) AccountType.MORTGAGE else AccountType.SAVING)
        }
        TERM_PRESETS.forEach { months ->
            val chip = ItemFilterChipBinding.inflate(layoutInflater, binding.groupTerm, false).root
            chip.id = View.generateViewId()
            chip.text = getString(R.string.term_months, months)
            chip.isChecked = months == OpenAccountState.DEFAULT_TERM
            chip.setOnClickListener {
                binding.etTerm.text = null
                viewModel.setTerm(months)
            }
            binding.groupTerm.addView(chip)
        }
        binding.etLoan.addTextChangedListener(AmountTextWatcher(binding.etLoan) { viewModel.setLoanAmount(it.toString()) })
        binding.etTerm.doAfterTextChanged { text ->
            val months = text?.toString()?.toIntOrNull() ?: return@doAfterTextChanged
            binding.groupTerm.clearCheck()
            viewModel.setTerm(months)
        }
        binding.btnSubmit.setOnClickListener { viewModel.submit(uid) }
        viewModel.state.observe(viewLifecycleOwner, ::render)
        viewModel.opened.observe(viewLifecycleOwner) { e ->
            e.consume()?.let { account ->
                Toast.makeText(requireContext(), getString(R.string.open_account_done, getString(HomeViewModel.labelFor(account.type))), Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
        if (savedInstanceState == null) viewModel.load()
    }

    private fun render(s: OpenAccountState) {
        val mortgage = s.type == AccountType.MORTGAGE
        val rate = getString(R.string.rate_per_year, MoneyFormatter.formatRate(s.mortgageRate))
        binding.groupMortgage.isVisible = mortgage
        binding.tvSavingInfo.isVisible = !mortgage
        binding.rows.setKeyValueRows(
            listOf(
                getString(R.string.open_account_rate_row) to rate,
                getString(R.string.open_account_monthly_row) to (s.monthly?.let(MoneyFormatter::formatVnd) ?: DASH),
                getString(R.string.open_account_biweekly_row) to (s.biweekly?.let(MoneyFormatter::formatVnd) ?: DASH)
            )
        )
        binding.btnSubmit.setText(if (mortgage) R.string.open_account_submit_mortgage else R.string.open_account_submit_saving)
        binding.btnSubmit.isEnabled = !s.submitting
        binding.tvError.isVisible = s.errorRes != null
        s.errorRes?.let(binding.tvError::setText)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_UID = "uid"
        private const val ARG_NAME = "name"
        private const val ARG_PHONE = "phone"
        private const val DASH = "—"
        private val TERM_PRESETS = listOf(12, 60, 120, 240)

        fun newInstance(uid: String, name: String, phone: String) =
            OpenAccountFragment().apply { arguments = bundleOf(ARG_UID to uid, ARG_NAME to name, ARG_PHONE to phone) }
    }
}
