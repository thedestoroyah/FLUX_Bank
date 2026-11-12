package com.flux.bank.ui.customer.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.core.format.TextNormalizer
import com.flux.bank.data.model.Banks
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import com.flux.bank.databinding.FragmentReceiptBinding
import com.flux.bank.ui.common.setKeyValueRows
import com.flux.bank.ui.customer.CustomerMainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Biên lai một giao dịch: ngay sau khi nạp/rút xong, hoặc chỉ xem khi chạm một dòng trong lịch sử. */
class ReceiptFragment : Fragment() {

    private var _binding: FragmentReceiptBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tx = transactionFrom(requireArguments())
        val type = tx.type
        val transfer = type == TransactionType.TRANSFER_OUT || type == TransactionType.TRANSFER_IN
        binding.header.tvHeaderTitle.setText(R.string.receipt_title)
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.actions.isVisible = !requireArguments().getBoolean(ARG_VIEW_ONLY)
        binding.tvStatus.setText(
            when (type) {
                TransactionType.DEPOSIT -> R.string.receipt_deposit_ok
                TransactionType.WITHDRAW -> R.string.receipt_withdraw_ok
                TransactionType.TRANSFER_OUT -> R.string.receipt_transfer_ok
                TransactionType.TRANSFER_IN -> R.string.receipt_transfer_in_ok
                TransactionType.PAYMENT -> R.string.receipt_payment_ok
            }
        )
        binding.tvAmount.text = TransactionRowAdapter.signedAmount(tx)
        binding.tvAmount.setTextColor(
            ContextCompat.getColor(requireContext(), if (type.isIncoming) R.color.flux_emerald_dark else R.color.flux_text_primary)
        )
        binding.rows.setKeyValueRows(rowsFor(tx, requireArguments().getString(ARG_ACCOUNT_LABEL).orEmpty()))
        binding.btnHome.setOnClickListener { leaveTo(CustomerMainActivity.CustomerTab.HOME) }
        if (transfer) binding.btnHistory.setText(R.string.receipt_transfer_again)
        binding.btnHistory.setOnClickListener {
            leaveTo(if (transfer) CustomerMainActivity.CustomerTab.TRANSFER else CustomerMainActivity.CustomerTab.HISTORY)
        }
    }

    /** Các dòng biên lai theo loại giao dịch (mockup F3 màn 4, F4 màn 5). */
    private fun rowsFor(tx: Transaction, accountLabel: String): List<Pair<String, String>> {
        val counterparty = getString(R.string.receipt_bank_account, bankName(tx.counterpartyBank), tx.counterpartyAccount)
        val head = when (tx.type) {
            TransactionType.TRANSFER_OUT -> listOf(
                getString(R.string.receipt_from) to accountLabel,
                getString(R.string.receipt_to) to tx.counterpartyName,
                "" to counterparty,
                getString(R.string.receipt_description) to tx.description
            )
            TransactionType.TRANSFER_IN -> listOf(
                getString(R.string.receipt_to_account) to accountLabel,
                getString(R.string.receipt_from) to TextNormalizer.holderName(tx.counterpartyName),
                "" to counterparty,
                getString(R.string.receipt_description) to tx.description
            )
            TransactionType.DEPOSIT -> listOfNotNull(
                getString(R.string.receipt_account) to accountLabel,
                getString(R.string.move_source) to (if (tx.paymentRef != null) getString(R.string.vnpay) else getString(R.string.move_source_value)),
                tx.paymentRef?.let { getString(R.string.receipt_vnpay_ref) to it }
            )
            TransactionType.WITHDRAW -> listOf(
                getString(R.string.receipt_account) to accountLabel,
                getString(R.string.move_destination) to getString(R.string.move_destination_value)
            )
            TransactionType.PAYMENT -> listOf(getString(R.string.receipt_account) to accountLabel)
        }
        return head + listOfNotNull(
            getString(R.string.receipt_time) to TIME_FORMAT.format(Date(tx.createdAt)),
            getString(R.string.receipt_id) to displayId(tx.id),
            tx.balanceAfter?.let { getString(R.string.receipt_balance_after) to MoneyFormatter.formatVnd(it) }
        )
    }

    private fun bankName(code: String) = Banks.byCode(code)?.shortName ?: getString(R.string.bank_flux)

    /** Đóng mọi màn phụ rồi chuyển tab. */
    private fun leaveTo(tab: CustomerMainActivity.CustomerTab) {
        val activity = requireActivity() as CustomerMainActivity
        parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        activity.showTab(tab)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_TYPE = "type"
        private const val ARG_AMOUNT = "amount"
        private const val ARG_BALANCE_AFTER = "balance_after"
        private const val ARG_CREATED_AT = "created_at"
        private const val ARG_ACCOUNT_LABEL = "account_label"
        private const val ARG_VIEW_ONLY = "view_only"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_CP_NAME = "cp_name"
        private const val ARG_CP_ACCOUNT = "cp_account"
        private const val ARG_CP_BANK = "cp_bank"
        private const val ARG_PAYMENT_REF = "payment_ref"
        private const val ID_LENGTH = 10
        private val TIME_FORMAT = SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale("vi"))

        /** Mã giao dịch hiển thị: 10 ký tự đầu của document id, viết hoa. */
        fun displayId(id: String) = id.take(ID_LENGTH).uppercase()

        fun newInstance(tx: Transaction, accountLabel: String, viewOnly: Boolean) = ReceiptFragment().apply {
            arguments = bundleOf(
                ARG_ID to tx.id, ARG_TYPE to tx.type.name, ARG_AMOUNT to tx.amount, ARG_CREATED_AT to tx.createdAt,
                ARG_ACCOUNT_LABEL to accountLabel, ARG_VIEW_ONLY to viewOnly, ARG_DESCRIPTION to tx.description,
                ARG_CP_NAME to tx.counterpartyName, ARG_CP_ACCOUNT to tx.counterpartyAccount, ARG_CP_BANK to tx.counterpartyBank,
                ARG_PAYMENT_REF to tx.paymentRef
            ).apply { tx.balanceAfter?.let { putLong(ARG_BALANCE_AFTER, it) } }
        }

        private fun transactionFrom(args: Bundle) = Transaction(
            id = args.getString(ARG_ID)!!, type = TransactionType.valueOf(args.getString(ARG_TYPE)!!),
            amount = args.getLong(ARG_AMOUNT), createdAt = args.getLong(ARG_CREATED_AT),
            balanceAfter = if (args.containsKey(ARG_BALANCE_AFTER)) args.getLong(ARG_BALANCE_AFTER) else null,
            description = args.getString(ARG_DESCRIPTION).orEmpty(), counterpartyName = args.getString(ARG_CP_NAME).orEmpty(),
            counterpartyAccount = args.getString(ARG_CP_ACCOUNT).orEmpty(), counterpartyBank = args.getString(ARG_CP_BANK).orEmpty(),
            paymentRef = args.getString(ARG_PAYMENT_REF)
        )
    }
}
