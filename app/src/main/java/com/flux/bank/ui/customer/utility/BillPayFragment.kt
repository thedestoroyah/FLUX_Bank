package com.flux.bank.ui.customer.utility

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.databinding.FragmentBillPayBinding
import com.flux.bank.databinding.ItemFilterChipBinding
import com.flux.bank.domain.utility.BillKind
import com.flux.bank.domain.utility.BillProvider
import com.flux.bank.ui.common.screenHost
import com.flux.bank.ui.common.setKeyValueRows
import com.flux.bank.ui.customer.Utility
import com.flux.bank.ui.customer.account.ReceiptFragment
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/** Tiền điện / tiền nước (mockup F5 màn 1–2): chọn nhà cung cấp, tra mã khách hàng, trả hoá đơn kỳ trước. */
class BillPayFragment : Fragment() {

    private var _binding: FragmentBillPayBinding? = null
    private val binding get() = _binding!!
    private val kind by lazy { BillKind.valueOf(requireArguments().getString(ARG_KIND)!!) }
    private val utility get() = if (kind == BillKind.ELECTRICITY) Utility.TIEN_DIEN else Utility.TIEN_NUOC
    private val viewModel: BillPayViewModel by viewModels {
        viewModelFactory { initializer { BillPayViewModel(BillKind.valueOf(requireArguments().getString(ARG_KIND)!!)) } }
    }
    private var ui: PaymentUi? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBillPayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.tvHeaderTitle.setText(utility.label)
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.tilCode.helperText = getString(if (kind == BillKind.ELECTRICITY) R.string.bill_code_hint_power else R.string.bill_code_hint_water)
        binding.ivBillIcon.setImageResource(utility.icon)
        binding.ivBillIcon.backgroundTintList = ContextCompat.getColorStateList(requireContext(), utility.tileBg)
        ImageViewCompat.setImageTintList(binding.ivBillIcon, ContextCompat.getColorStateList(requireContext(), utility.tint))

        BillProvider.forKind(kind).forEach { provider ->
            val chip = ItemFilterChipBinding.inflate(layoutInflater, binding.groupProviders, false).root
            chip.id = View.generateViewId()
            chip.tag = provider.name
            chip.text = provider.displayName
            binding.groupProviders.addView(chip)
        }
        binding.groupProviders.setOnCheckedStateChangeListener { group, ids ->
            val tag = group.findViewById<View>(ids.firstOrNull() ?: return@setOnCheckedStateChangeListener)?.tag as? String
            tag?.let { viewModel.selectProvider(BillProvider.valueOf(it)) }
        }
        binding.etCode.doAfterTextChanged { viewModel.setCode(it.toString()) }
        binding.etCode.setOnEditorActionListener { _, action, _ ->
            (action == EditorInfo.IME_ACTION_SEARCH).also { if (it) viewModel.lookup() }
        }
        binding.btnLookup.setOnClickListener { viewModel.lookup() }
        binding.btnPay.setOnClickListener { viewModel.confirm() }
        binding.btnReceipt.setOnClickListener {
            val tx = viewModel.bill.value?.paidTx ?: return@setOnClickListener
            val account = viewModel.checkout.value?.sources?.firstOrNull { it.accountId == tx.accountId }
            screenHost.openScreen(ReceiptFragment.newInstance(tx, ui?.accountLabel(account).orEmpty(), viewOnly = true))
        }

        ui = PaymentUi(this, viewModel, utility, binding.kyc, binding.source).also { it.attach() }
        viewModel.bill.observe(viewLifecycleOwner) { render() }
        viewModel.checkout.observe(viewLifecycleOwner) { render() }
        if (savedInstanceState == null) viewModel.load()
    }

    private fun render() {
        val b = viewModel.bill.value ?: return
        val c = viewModel.checkout.value ?: return
        ui?.render(c)
        for (i in 0 until binding.groupProviders.childCount) {
            val chip = binding.groupProviders.getChildAt(i) as com.google.android.material.chip.Chip
            if (chip.tag == b.provider.name && !chip.isChecked) chip.isChecked = true
        }
        binding.tilCode.error = when {
            b.codeError -> getString(R.string.bill_code_error)
            b.notFound -> getString(R.string.bill_not_found)
            else -> null
        }
        val bill = b.bill
        binding.cardBill.isVisible = bill != null
        if (bill != null) {
            binding.tvHolder.text = bill.holderName
            binding.tvAddress.text = bill.address
            val usage = getString(if (kind == BillKind.ELECTRICITY) R.string.bill_usage_power else R.string.bill_usage_water, bill.usage)
            binding.billRows.setKeyValueRows(
                listOf(
                    getString(R.string.bill_period) to bill.period.format(PERIOD),
                    getString(R.string.bill_usage) to usage,
                    getString(R.string.bill_due) to bill.dueDate.format(DATE)
                )
            )
            binding.tvBillAmount.text = MoneyFormatter.formatVnd(bill.amount)
            binding.btnPay.text = getString(R.string.pay_action_amount, MoneyFormatter.formatVnd(bill.amount))
        } else {
            binding.btnPay.setText(R.string.pay_action)
        }
        val paid = b.paidTx
        binding.cardPaid.isVisible = paid != null
        paid?.let { binding.tvPaid.text = getString(R.string.bill_paid, PAID_TIME.format(Date(it.createdAt))) }
        binding.btnPay.isVisible = paid == null
        binding.btnReceipt.isVisible = paid != null
        binding.btnPay.isEnabled = ui?.canPay(c) == true && bill != null && b.paidChecked && !b.looking
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ui = null
        _binding = null
    }

    companion object {
        private const val ARG_KIND = "kind"
        private val PERIOD = DateTimeFormatter.ofPattern("MM/yyyy")
        private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val PAID_TIME = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))

        fun newInstance(kind: BillKind) = BillPayFragment().apply { arguments = bundleOf(ARG_KIND to kind.name) }
    }
}
