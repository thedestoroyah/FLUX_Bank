package com.flux.bank.ui.customer.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.core.format.TextNormalizer
import com.flux.bank.data.model.BankAccount
import com.flux.bank.domain.TransferError
import com.flux.bank.databinding.FragmentTransferBinding
import com.flux.bank.databinding.ItemFilterChipBinding
import com.flux.bank.ui.auth.sheet.AuthSummary
import com.flux.bank.ui.auth.sheet.TransactionAuthSheet
import com.flux.bank.ui.common.AmountTextWatcher
import com.flux.bank.ui.common.screenHost
import com.flux.bank.ui.customer.account.ReceiptFragment
import com.flux.bank.ui.customer.home.HomeViewModel
import com.flux.bank.ui.ekyc.EkycCaptureFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/** Form chuyển tiền cho ba chế độ [TransferMode] (mockup F4 màn 2–3), xác thực bằng `TransactionAuthSheet`. */
class TransferFragment : Fragment() {

    private var _binding: FragmentTransferBinding? = null
    private val binding get() = _binding!!
    private val mode by lazy { TransferMode.valueOf(requireArguments().getString(ARG_MODE)!!) }
    private val viewModel: TransferViewModel by viewModels {
        viewModelFactory { initializer { TransferViewModel(TransferMode.valueOf(requireArguments().getString(ARG_MODE)!!)) } }
    }
    // Chip được tạo lại khi danh sách tài khoản đổi; reset theo view để quay lại màn này vẫn có chip
    private var sourceChipIds: List<String> = emptyList()
    private var targetChipIds: List<String> = emptyList()
    private var descriptionFilled = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.tvHeaderTitle.text = title()
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.sectionOwn.isVisible = mode == TransferMode.OWN
        binding.tilAccount.isVisible = mode != TransferMode.OWN
        binding.tilBank.isVisible = mode == TransferMode.EXTERNAL
        binding.tilName.isVisible = mode == TransferMode.EXTERNAL
        binding.tvExternalNote.isVisible = mode == TransferMode.EXTERNAL
        if (mode == TransferMode.EXTERNAL) binding.tilAccount.setHint(R.string.transfer_account_number_external)

        prefillFields()
        binding.etAccount.doAfterTextChanged { viewModel.setAccountNumber(it.toString()) }
        binding.etName.doAfterTextChanged { viewModel.setExternalName(it.toString()) }
        binding.etDescription.doAfterTextChanged { if (descriptionFilled) viewModel.setDescription(it.toString()) }
        binding.etAmount.addTextChangedListener(AmountTextWatcher(binding.etAmount) { viewModel.setAmount(it.toString()) })
        QUICK_AMOUNTS.forEach { amount ->
            val chip = ItemFilterChipBinding.inflate(layoutInflater, binding.groupQuick, false).root
            chip.isCheckable = false
            chip.text = MoneyFormatter.formatVnd(amount).removeSuffix(" ₫")
            chip.setOnClickListener { binding.etAmount.setText(amount.toString()) }
            binding.groupQuick.addView(chip)
        }
        binding.groupSources.onChipSelected(viewModel::selectSource)
        binding.groupTargets.onChipSelected(viewModel::selectTarget)
        val pickBank = View.OnClickListener { BankPickerSheet.show(childFragmentManager, BANK_KEY) }
        binding.etBank.setOnClickListener(pickBank)
        binding.tilBank.setEndIconOnClickListener(pickBank)
        binding.btnKyc.setOnClickListener {
            val uid = FluxApp.instance.authRepository.currentUid() ?: return@setOnClickListener
            screenHost.openScreen(EkycCaptureFragment.newInstance(uid, submittedBy = uid))
        }
        binding.btnContinue.setOnClickListener { viewModel.confirm() }

        childFragmentManager.setFragmentResultListener(BANK_KEY, viewLifecycleOwner) { _, result ->
            result.getString(BankPickerSheet.KEY_CODE)?.let(viewModel::selectBank)
        }
        childFragmentManager.setFragmentResultListener(AUTH_KEY, viewLifecycleOwner) { _, result ->
            viewModel.onAuthResult(result.getBoolean(TransactionAuthSheet.KEY_VERIFIED))
        }
        viewModel.askAuth.observe(viewLifecycleOwner) { e -> e.consume()?.let(::showAuth) }
        viewModel.done.observe(viewLifecycleOwner) { e ->
            e.consume()?.let { tx ->
                val label = viewModel.state.value?.source?.let(::accountLabel).orEmpty()
                parentFragmentManager.popBackStack()
                screenHost.openScreen(ReceiptFragment.newInstance(tx, label, viewOnly = false))
            }
        }
        viewModel.state.observe(viewLifecycleOwner, ::render)
        if (savedInstanceState == null) viewModel.load(prefill())
    }

    /** Điền sẵn ô nhập từ prefill (không gọi ViewModel: `load` tự áp dụng prefill sau khi tải xong). */
    private fun prefillFields() {
        val prefill = prefill() ?: return
        binding.etAccount.setText(prefill.accountNumber)
        if (mode == TransferMode.EXTERNAL) binding.etName.setText(prefill.name)
    }

    private fun prefill(): TransferPrefill? {
        val args = requireArguments()
        val number = args.getString(ARG_ACCOUNT_NUMBER) ?: return null
        return TransferPrefill(number, args.getString(ARG_BANK_CODE)!!, args.getString(ARG_NAME).orEmpty())
    }

    private fun title() = getString(
        when (mode) {
            TransferMode.INTERNAL -> R.string.transfer_title_internal
            TransferMode.OWN -> R.string.transfer_own
            TransferMode.EXTERNAL -> R.string.transfer_external
        }
    )

    private fun showAuth(request: AuthRequest) {
        TransactionAuthSheet.show(
            childFragmentManager,
            AuthSummary(
                title(),
                listOf(
                    getString(R.string.transfer_sheet_recipient) to request.recipientName,
                    getString(R.string.transfer_sheet_account) to request.recipientDetail,
                    getString(R.string.move_sheet_amount) to MoneyFormatter.formatVnd(request.amount)
                ),
                request.amount
            ),
            AUTH_KEY
        )
    }

    private fun render(s: TransferState) {
        binding.cardKyc.isVisible = s.kycBlocked
        sourceChipIds = renderChips(binding.groupSources, s.sources, s.sourceId, sourceChipIds)
        targetChipIds = renderChips(binding.groupTargets, s.ownTargets, s.targetId, targetChipIds)
        if (!descriptionFilled && s.description.isNotEmpty()) {
            descriptionFilled = true
            binding.etDescription.setText(s.description)
        }
        binding.etBank.setText(s.bank?.let { "${it.code} · ${it.shortName}" }.orEmpty())

        binding.cardPayee.isVisible = s.payee != null
        s.payee?.let { payee ->
            val name = TextNormalizer.holderName(payee.holderName)
            binding.tvPayeeInitials.text = HomeViewModel.initials(name)
            binding.tvPayeeName.text = name
            binding.tvPayeeDetail.text = getString(R.string.receipt_bank_account, getString(R.string.bank_flux), getString(HomeViewModel.labelFor(payee.type)))
        }
        val recipientText = s.recipientError?.let { errorText(it, s.remainingToday) }
        binding.tilBank.error = recipientText.takeIf { s.recipientError == TransferError.NO_BANK }
        binding.tilName.error = recipientText.takeIf { s.recipientError == TransferError.NO_NAME }
        binding.tilAccount.error = recipientText.takeIf { s.recipientError !in setOf(TransferError.NO_BANK, TransferError.NO_NAME) && mode != TransferMode.OWN }
        binding.tilAccount.helperText = if (s.lookingUp) getString(R.string.transfer_looking_up) else null
        binding.tvTargetError.isVisible = mode == TransferMode.OWN && recipientText != null
        binding.tvTargetError.text = recipientText

        binding.tilAmount.helperText = getString(R.string.transfer_remaining, MoneyFormatter.formatVnd(s.remainingToday))
        binding.tilAmount.error = s.amountError?.let { errorText(it, s.remainingToday) }
        binding.tvError.isVisible = s.errorRes != null
        s.errorRes?.let(binding.tvError::setText)
        binding.btnContinue.isEnabled = !s.submitting && !s.kycBlocked && s.source != null
    }

    private fun errorText(error: TransferError, remainingToday: Long): String = when (error) {
        TransferError.DAILY_LIMIT -> getString(R.string.err_transfer_daily, MoneyFormatter.formatVnd(remainingToday))
        else -> getString(errorRes(error))
    }

    private fun errorRes(error: TransferError): Int = when (error) {
            TransferError.BELOW_MIN -> R.string.err_amount_min
            TransferError.ABOVE_MAX -> R.string.err_transfer_max
            TransferError.INSUFFICIENT_FUNDS -> R.string.err_amount_balance
            TransferError.DAILY_LIMIT -> R.string.err_transfer_daily
            TransferError.SAME_ACCOUNT -> R.string.err_transfer_same
            TransferError.INVALID_ACCOUNT_NUMBER ->
                if (mode == TransferMode.EXTERNAL) R.string.err_transfer_external_number else R.string.err_transfer_flux_number
            TransferError.PAYEE_NOT_FOUND -> if (mode == TransferMode.OWN) R.string.err_transfer_no_target else R.string.err_transfer_not_found
            TransferError.NO_BANK -> R.string.err_transfer_no_bank
            TransferError.NO_NAME -> R.string.err_transfer_no_name
        }

    /** Tạo lại chip khi danh sách đổi, cập nhật nhãn số dư và chip đang chọn; trả về danh sách id hiện tại. */
    private fun renderChips(group: ChipGroup, accounts: List<BankAccount>, selectedId: String?, current: List<String>): List<String> {
        val ids = accounts.map { it.accountId }
        if (ids != current) {
            group.removeAllViews()
            accounts.forEach { account ->
                val chip = ItemFilterChipBinding.inflate(layoutInflater, group, false).root
                chip.id = View.generateViewId()
                chip.tag = account.accountId
                group.addView(chip)
            }
        }
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            val account = accounts.firstOrNull { it.accountId == chip.tag } ?: continue
            chip.text = getString(R.string.move_account_chip, getString(HomeViewModel.labelFor(account.type)), MoneyFormatter.formatVnd(account.balance))
            if (account.accountId == selectedId && !chip.isChecked) chip.isChecked = true
        }
        return ids
    }

    private fun ChipGroup.onChipSelected(select: (String) -> Unit) = setOnCheckedStateChangeListener { group, ids ->
        (group.findViewById<View>(ids.firstOrNull() ?: return@setOnCheckedStateChangeListener)?.tag as? String)?.let(select)
    }

    private fun accountLabel(account: BankAccount) =
        getString(HomeViewModel.labelFor(account.type)) + " · " + account.accountNumber

    override fun onDestroyView() {
        super.onDestroyView()
        sourceChipIds = emptyList()
        targetChipIds = emptyList()
        descriptionFilled = false
        _binding = null
    }

    companion object {
        private const val ARG_MODE = "mode"
        private const val ARG_ACCOUNT_NUMBER = "account_number"
        private const val ARG_BANK_CODE = "bank_code"
        private const val ARG_NAME = "name"
        private const val AUTH_KEY = "transfer_auth"
        private const val BANK_KEY = "transfer_bank"
        private val QUICK_AMOUNTS = listOf(100_000L, 500_000L, 1_000_000L, 5_000_000L)

        fun newInstance(mode: TransferMode, prefill: TransferPrefill? = null) = TransferFragment().apply {
            arguments = bundleOf(
                ARG_MODE to mode.name, ARG_ACCOUNT_NUMBER to prefill?.accountNumber,
                ARG_BANK_CODE to prefill?.bankCode, ARG_NAME to prefill?.name
            )
        }
    }
}
