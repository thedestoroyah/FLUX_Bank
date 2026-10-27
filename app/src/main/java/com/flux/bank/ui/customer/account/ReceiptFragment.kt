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
        val args = requireArguments()
        val type = TransactionType.valueOf(args.getString(ARG_TYPE)!!)
        val tx = Transaction(
            id = args.getString(ARG_ID)!!, type = type, amount = args.getLong(ARG_AMOUNT),
            balanceAfter = args.getLong(ARG_BALANCE_AFTER), createdAt = args.getLong(ARG_CREATED_AT)
        )
        binding.header.tvHeaderTitle.setText(R.string.receipt_title)
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.actions.isVisible = !args.getBoolean(ARG_VIEW_ONLY)
        binding.tvStatus.setText(if (type.isIncoming) R.string.receipt_deposit_ok else R.string.receipt_withdraw_ok)
        binding.tvAmount.text = TransactionRowAdapter.signedAmount(tx)
        binding.tvAmount.setTextColor(
            ContextCompat.getColor(requireContext(), if (type.isIncoming) R.color.flux_emerald_dark else R.color.flux_text_primary)
        )
        binding.rows.setKeyValueRows(
            listOf(
                getString(R.string.receipt_account) to args.getString(ARG_ACCOUNT_LABEL).orEmpty(),
                getString(if (type.isIncoming) R.string.move_source else R.string.move_destination) to
                    getString(if (type.isIncoming) R.string.move_source_value else R.string.move_destination_value),
                getString(R.string.receipt_time) to TIME_FORMAT.format(Date(tx.createdAt)),
                getString(R.string.receipt_id) to displayId(tx.id),
                getString(R.string.receipt_balance_after) to MoneyFormatter.formatVnd(tx.balanceAfter)
            )
        )
        binding.btnHome.setOnClickListener { leaveTo(CustomerMainActivity.CustomerTab.HOME) }
        binding.btnHistory.setOnClickListener { leaveTo(CustomerMainActivity.CustomerTab.HISTORY) }
    }

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
        private const val ID_LENGTH = 10
        private val TIME_FORMAT = SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale("vi"))

        /** Mã giao dịch hiển thị: 10 ký tự đầu của document id, viết hoa. */
        fun displayId(id: String) = id.take(ID_LENGTH).uppercase()

        fun newInstance(tx: Transaction, accountLabel: String, viewOnly: Boolean) = ReceiptFragment().apply {
            arguments = bundleOf(
                ARG_ID to tx.id, ARG_TYPE to tx.type.name, ARG_AMOUNT to tx.amount, ARG_BALANCE_AFTER to tx.balanceAfter,
                ARG_CREATED_AT to tx.createdAt, ARG_ACCOUNT_LABEL to accountLabel, ARG_VIEW_ONLY to viewOnly
            )
        }
    }
}
