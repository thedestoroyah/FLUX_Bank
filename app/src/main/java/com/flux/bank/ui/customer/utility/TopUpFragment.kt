package com.flux.bank.ui.customer.utility

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.databinding.FragmentTopUpBinding
import com.flux.bank.databinding.ItemDenominationBinding
import com.flux.bank.domain.utility.Carriers
import com.flux.bank.ui.customer.Utility
import com.google.android.material.card.MaterialCardView

/** Nạp tiền điện thoại (mockup F5 màn 3): SĐT + nhà mạng, lưới 6 mệnh giá, nguồn tiền. */
class TopUpFragment : Fragment() {

    private var _binding: FragmentTopUpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TopUpViewModel by viewModels()
    private var ui: PaymentUi? = null
    private val cells = mutableMapOf<Long, MaterialCardView>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTopUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.tvHeaderTitle.setText(R.string.util_topup)
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        val gap = resources.getDimensionPixelSize(R.dimen.space_4)
        Carriers.DENOMINATIONS.forEach { amount ->
            val cell = ItemDenominationBinding.inflate(layoutInflater, binding.gridAmounts, false)
            cell.tvValue.text = MoneyFormatter.formatVnd(amount).removeSuffix(" ₫")
            cell.root.contentDescription = MoneyFormatter.formatVnd(amount)
            cell.root.setOnClickListener { viewModel.selectAmount(amount) }
            cell.root.layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
                width = 0
                height = resources.getDimensionPixelSize(R.dimen.button_height)
                setMargins(gap, gap, gap, gap)
            }
            binding.gridAmounts.addView(cell.root)
            cells[amount] = cell.root
        }
        binding.etPhone.doAfterTextChanged { viewModel.setPhone(it.toString()) }
        binding.btnPay.setOnClickListener { viewModel.confirm() }

        ui = PaymentUi(this, viewModel, Utility.NAP_DT, binding.kyc, binding.source).also { it.attach() }
        viewModel.topUp.observe(viewLifecycleOwner) { render() }
        viewModel.checkout.observe(viewLifecycleOwner) { render() }
        if (savedInstanceState == null) viewModel.load()
    }

    private fun render() {
        val t = viewModel.topUp.value ?: return
        val c = viewModel.checkout.value ?: return
        ui?.render(c)
        // SĐT mặc định (ViewModel điền sau khi tải hồ sơ) chỉ đưa vào ô khi ô đang trống
        if (binding.etPhone.text.isNullOrEmpty() && t.phone.isNotEmpty()) binding.etPhone.setText(t.phone)
        binding.tilPhone.suffixText = t.carrier?.displayName
        binding.tilPhone.error = if (t.phoneError) getString(R.string.topup_phone_error) else null
        val stroke = resources.getDimensionPixelSize(R.dimen.stroke_hairline)
        cells.forEach { (amount, cell) ->
            cell.isChecked = amount == t.amount
            // Ô đang chọn viền dày hơn (1.5dp như ô nhập đang focus)
            cell.strokeWidth = if (cell.isChecked) stroke * 3 / 2 else stroke
        }
        binding.btnPay.text = t.amount?.let { getString(R.string.topup_action_amount, MoneyFormatter.formatVnd(it)) }
            ?: getString(R.string.topup_action)
        binding.btnPay.isEnabled = ui?.canPay(c) == true && t.amount != null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ui = null
        cells.clear()
        _binding = null
    }
}
