package com.flux.bank.ui.customer.account

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.Transaction
import com.flux.bank.databinding.FragmentAccountDetailBinding
import com.flux.bank.domain.PaymentFrequency
import com.flux.bank.ui.common.screenHost
import com.flux.bank.ui.common.setKeyValueRows
import com.flux.bank.ui.customer.home.HomeViewModel
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/** Chi tiết một tài khoản của khách: Checking/Saving (số dư, lãi, nạp/rút) hoặc Mortgage (trả nợ theo kỳ). */
class AccountDetailFragment : Fragment() {

    private var _binding: FragmentAccountDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AccountDetailViewModel by viewModels()
    private val accountId by lazy { requireArguments().getString(ARG_ID)!! }
    private val recentAdapter = TransactionRowAdapter(
        subtitle = { TIME_FORMAT.format(Date(it.createdAt)) },
        onClick = ::openReceipt
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.rvRecent.adapter = recentAdapter
        binding.accountCard.btnEye.isVisible = false
        binding.groupFrequency.setOnCheckedStateChangeListener { _, ids ->
            viewModel.setFrequency(if (ids.firstOrNull() == R.id.chipBiweekly) PaymentFrequency.BIWEEKLY else PaymentFrequency.MONTHLY)
        }
        binding.btnCopy.setOnClickListener {
            val number = viewModel.state.value?.account?.accountNumber ?: return@setOnClickListener
            requireContext().getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText(number, number))
            Snackbar.make(binding.root, R.string.detail_copied, Snackbar.LENGTH_SHORT).show()
        }
        binding.btnDeposit.setOnClickListener { screenHost.openScreen(MoneyMoveFragment.newInstance(deposit = true, accountId = accountId)) }
        binding.btnWithdraw.setOnClickListener { screenHost.openScreen(MoneyMoveFragment.newInstance(deposit = false, accountId = accountId)) }
        viewModel.state.observe(viewLifecycleOwner, ::render)
    }

    override fun onResume() {
        super.onResume()
        // Tải lại để số dư và giao dịch mới hiện ngay sau khi quay về từ màn nạp/rút
        viewModel.load(accountId)
    }

    private fun render(s: AccountDetailState) {
        if (s.failed) Snackbar.make(binding.root, R.string.detail_load_failed, Snackbar.LENGTH_LONG).show()
        val account = s.account ?: return
        val mortgage = account.type == AccountType.MORTGAGE
        binding.header.tvHeaderTitle.setText(
            when (account.type) {
                AccountType.CHECKING -> R.string.detail_title_checking
                AccountType.SAVING -> R.string.detail_title_saving
                AccountType.MORTGAGE -> R.string.detail_title_mortgage
            }
        )
        binding.groupBalance.isVisible = !mortgage
        binding.groupMortgage.isVisible = mortgage
        if (mortgage) renderMortgage(s) else renderBalance(s)
    }

    private fun renderBalance(s: AccountDetailState) {
        val account = s.account ?: return
        with(binding.accountCard) {
            tvType.setText(HomeViewModel.labelFor(account.type))
            tvBalanceLabel.setText(R.string.card_available_balance)
            tvBalance.text = MoneyFormatter.formatVnd(account.balance)
            tvNumber.text = "•••• " + account.accountNumber.takeLast(4)
        }
        binding.btnCopy.text = getString(R.string.detail_copy_number, account.accountNumber)
        binding.cardInterest.isVisible = s.saving != null
        s.saving?.let { info ->
            binding.rowsInterest.setKeyValueRows(
                listOf(
                    getString(R.string.saving_rate_row) to getString(R.string.rate_per_year, MoneyFormatter.formatRate(info.ratePercent)),
                    getString(R.string.saving_monthly_row) to "+" + MoneyFormatter.formatVnd(info.monthlyInterest),
                    getString(R.string.saving_yearly_row) to MoneyFormatter.formatVnd(info.yearlyInterest)
                )
            )
        }
        recentAdapter.submit(s.recent)
        binding.tvNoTx.isVisible = s.recent.isEmpty()
    }

    private fun renderMortgage(s: AccountDetailState) {
        val account = s.account ?: return
        val info = s.mortgage ?: return
        val rate = getString(R.string.rate_per_year, MoneyFormatter.formatRate(account.annualRate))
        binding.tvOutstanding.text = MoneyFormatter.formatVnd(account.balance)
        binding.tvLoanMeta.text = getString(R.string.mortgage_meta, account.accountNumber, rate)
        binding.tvInstallment.text = MoneyFormatter.formatVnd(info.installment)
        binding.tvNextDue.text = getString(R.string.mortgage_next_due, info.nextDue.format(DATE_FORMAT))
        val start = Instant.ofEpochMilli(account.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
        binding.rowsLoan.setKeyValueRows(
            listOf(
                getString(R.string.mortgage_loan_row) to MoneyFormatter.formatVnd(account.loanAmount),
                getString(R.string.mortgage_term_row) to getString(R.string.mortgage_term_value, account.termMonths, info.periods),
                getString(R.string.mortgage_start_row) to start.format(DATE_FORMAT),
                getString(R.string.mortgage_interest_row) to MoneyFormatter.formatVnd(info.totalInterest),
                getString(R.string.mortgage_total_row) to MoneyFormatter.formatVnd(info.totalPayable)
            )
        )
    }

    private fun openReceipt(tx: Transaction) {
        val account = viewModel.state.value?.account ?: return
        val label = getString(HomeViewModel.labelFor(account.type)) + " · " + account.accountNumber
        screenHost.openScreen(ReceiptFragment.newInstance(tx, label, viewOnly = true))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "account_id"
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val TIME_FORMAT = SimpleDateFormat("dd/MM · HH:mm", Locale("vi"))

        fun newInstance(accountId: String) = AccountDetailFragment().apply { arguments = bundleOf(ARG_ID to accountId) }
    }
}
