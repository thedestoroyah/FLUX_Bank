package com.flux.bank.ui.customer.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.core.validation.AmountError
import com.flux.bank.data.model.BankAccount
import com.flux.bank.databinding.FragmentMoneyMoveBinding
import com.flux.bank.databinding.ItemFilterChipBinding
import com.flux.bank.ui.auth.sheet.AuthSummary
import com.flux.bank.ui.auth.sheet.TransactionAuthSheet
import com.flux.bank.ui.common.AmountTextWatcher
import com.flux.bank.ui.common.screenHost
import com.flux.bank.ui.common.setKeyValueRows
import com.flux.bank.ui.customer.home.HomeViewModel
import com.flux.bank.ui.ekyc.EkycCaptureFragment

/** Nạp hoặc rút tiền trên Checking/Saving (mockup màn 3), xác thực bằng `TransactionAuthSheet`. */
class MoneyMoveFragment : Fragment() {

    private var _binding: FragmentMoneyMoveBinding? = null
    private val binding get() = _binding!!
    private val deposit by lazy { requireArguments().getBoolean(ARG_DEPOSIT) }
    private val viewModel: MoneyMoveViewModel by viewModels {
        viewModelFactory { initializer { MoneyMoveViewModel(requireArguments().getBoolean(ARG_DEPOSIT)) } }
    }
    private var chipIds: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoneyMoveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.tvHeaderTitle.setText(if (deposit) R.string.util_deposit else R.string.util_withdraw)
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.tvAccountLabel.setText(if (deposit) R.string.move_account_to else R.string.move_account_from)
        binding.tilAmount.helperText = getString(if (deposit) R.string.move_amount_helper_deposit else R.string.move_amount_helper_withdraw)
        if (deposit) binding.tvNote.setText(R.string.move_vnpay_note)
        binding.etAmount.addTextChangedListener(AmountTextWatcher(binding.etAmount) { viewModel.setAmount(it.toString()) })
        QUICK_AMOUNTS.forEach { amount ->
            val chip = ItemFilterChipBinding.inflate(layoutInflater, binding.groupQuick, false).root
            chip.isCheckable = false
            chip.text = MoneyFormatter.formatVnd(amount).removeSuffix(" ₫")
            chip.setOnClickListener { binding.etAmount.setText(amount.toString()) }
            binding.groupQuick.addView(chip)
        }
        binding.groupAccounts.setOnCheckedStateChangeListener { group, ids ->
            (group.findViewById<View>(ids.firstOrNull() ?: return@setOnCheckedStateChangeListener)?.tag as? String)?.let(viewModel::select)
        }
        binding.btnKyc.setOnClickListener {
            val uid = FluxApp.instance.authRepository.currentUid() ?: return@setOnClickListener
            screenHost.openScreen(EkycCaptureFragment.newInstance(uid, submittedBy = uid))
        }
        binding.btnContinue.setOnClickListener { viewModel.confirm() }
        childFragmentManager.setFragmentResultListener(AUTH_KEY, viewLifecycleOwner) { _, result ->
            viewModel.onAuthResult(result.getBoolean(TransactionAuthSheet.KEY_VERIFIED))
        }
        parentFragmentManager.setFragmentResultListener(VnPayFragment.REQUEST_KEY, viewLifecycleOwner) { _, result ->
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            val query = result.getSerializable(VnPayFragment.KEY_QUERY) as? HashMap<String, String>
            if (query != null) viewModel.onPaymentResult(query) else viewModel.onPaymentClosed()
        }
        viewModel.openPayment.observe(viewLifecycleOwner) { e -> e.consume()?.let { screenHost.openScreen(VnPayFragment.newInstance(it)) } }
        viewModel.askAuth.observe(viewLifecycleOwner) { e -> e.consume()?.let(::showAuth) }
        viewModel.done.observe(viewLifecycleOwner) { e ->
            e.consume()?.let { tx ->
                val label = viewModel.state.value?.selected?.let(::accountLabel).orEmpty()
                parentFragmentManager.popBackStack()
                screenHost.openScreen(ReceiptFragment.newInstance(tx, label, viewOnly = false))
            }
        }
        viewModel.state.observe(viewLifecycleOwner, ::render)
        if (savedInstanceState == null) viewModel.load(requireArguments().getString(ARG_ACCOUNT_ID))
    }

    private fun showAuth(amount: Long) {
        val account = viewModel.state.value?.selected ?: return
        TransactionAuthSheet.show(
            childFragmentManager,
            AuthSummary(
                getString(if (deposit) R.string.util_deposit else R.string.util_withdraw),
                listOf(
                    getString(R.string.move_sheet_account) to accountLabel(account),
                    getString(R.string.move_sheet_amount) to MoneyFormatter.formatVnd(amount)
                ),
                amount
            ),
            AUTH_KEY
        )
    }

    private fun accountLabel(account: BankAccount) =
        getString(HomeViewModel.labelFor(account.type)) + " · " + account.accountNumber

    private fun render(s: MoneyMoveState) {
        binding.cardKyc.isVisible = s.kycBlocked
        binding.tvNoAccount.isVisible = s.accounts.isEmpty() && !s.kycBlocked
        renderChips(s)
        binding.tilAmount.error = when (s.amountError) {
            AmountError.BELOW_MIN -> getString(R.string.err_amount_min)
            AmountError.ABOVE_DEPOSIT_MAX -> getString(R.string.err_amount_deposit_max)
            AmountError.INSUFFICIENT_FUNDS -> getString(R.string.err_amount_balance)
            null -> null
        }
        binding.rows.setKeyValueRows(
            listOf(
                getString(if (deposit) R.string.move_source else R.string.move_destination) to
                    getString(if (deposit) R.string.move_source_vnpay else R.string.move_destination_value),
                getString(R.string.move_balance_after) to (s.balanceAfter?.let(MoneyFormatter::formatVnd) ?: DASH)
            )
        )
        binding.tvError.isVisible = s.errorRes != null
        s.errorRes?.let(binding.tvError::setText)
        binding.btnContinue.isEnabled = !s.submitting && !s.kycBlocked && s.selected != null
    }

    /** Tạo lại chip tài khoản khi danh sách đổi; cập nhật nhãn số dư mỗi lần. */
    private fun renderChips(s: MoneyMoveState) {
        val ids = s.accounts.map { it.accountId }
        if (ids != chipIds) {
            chipIds = ids
            binding.groupAccounts.removeAllViews()
            s.accounts.forEach { account ->
                val chip = ItemFilterChipBinding.inflate(layoutInflater, binding.groupAccounts, false).root
                chip.id = View.generateViewId()
                chip.tag = account.accountId
                binding.groupAccounts.addView(chip)
            }
        }
        for (i in 0 until binding.groupAccounts.childCount) {
            val chip = binding.groupAccounts.getChildAt(i) as com.google.android.material.chip.Chip
            val account = s.accounts.firstOrNull { it.accountId == chip.tag } ?: continue
            chip.text = getString(R.string.move_account_chip, getString(HomeViewModel.labelFor(account.type)), MoneyFormatter.formatVnd(account.balance))
            if (account.accountId == s.selectedId && !chip.isChecked) chip.isChecked = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // View mới (quay lại từ VNPay/eKYC) chưa có chip nào: buộc renderChips tạo lại
        chipIds = emptyList()
        _binding = null
    }

    companion object {
        private const val ARG_DEPOSIT = "deposit"
        private const val ARG_ACCOUNT_ID = "account_id"
        private const val AUTH_KEY = "money_move_auth"
        private const val DASH = "—"
        private val QUICK_AMOUNTS = listOf(100_000L, 500_000L, 1_000_000L, 5_000_000L)

        fun newInstance(deposit: Boolean, accountId: String? = null) =
            MoneyMoveFragment().apply { arguments = bundleOf(ARG_DEPOSIT to deposit, ARG_ACCOUNT_ID to accountId) }
    }
}
