package com.flux.bank.ui.customer.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.flux.bank.R
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Banks
import com.flux.bank.data.model.Transaction
import com.flux.bank.databinding.FragmentHistoryBinding
import com.flux.bank.databinding.ItemFilterChipBinding
import com.flux.bank.ui.common.screenHost
import com.flux.bank.ui.customer.account.ReceiptFragment
import com.flux.bank.ui.customer.home.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tab Lịch sử: chip lọc theo tài khoản, giao dịch nhóm theo ngày; chạm một dòng để xem biên lai. */
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels()
    private var accounts: Map<String, BankAccount> = emptyMap()
    private val adapter = HistoryAdapter(subtitle = ::subtitleFor, onClick = ::openReceipt)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvHistory.adapter = adapter
        binding.groupFilter.setOnCheckedStateChangeListener { group, ids ->
            val chip = group.findViewById<View>(ids.firstOrNull() ?: return@setOnCheckedStateChangeListener)
            viewModel.filter(chip?.tag as? String)
        }
        viewModel.chips.observe(viewLifecycleOwner, ::renderChips)
        viewModel.rows.observe(viewLifecycleOwner) { rows ->
            adapter.submit(rows)
            binding.tvEmpty.isVisible = rows.isEmpty()
        }
        // Tải lỗi thì nói rõ là lỗi, đừng để khách tưởng mình chưa có giao dịch nào
        viewModel.failed.observe(viewLifecycleOwner) { failed ->
            binding.tvEmpty.setText(if (failed) R.string.history_load_failed else R.string.history_empty)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun renderChips(list: List<BankAccount>) {
        accounts = list.associateBy { it.accountId }
        adapter.notifyDataSetChanged()
        val selected = viewModel.selectedFilter()
        binding.groupFilter.removeAllViews()
        (listOf<BankAccount?>(null) + list).forEach { account ->
            val chip = ItemFilterChipBinding.inflate(layoutInflater, binding.groupFilter, false).root
            chip.id = View.generateViewId()
            chip.tag = account?.accountId
            chip.text = account?.let(::accountLabel) ?: getString(R.string.history_all)
            binding.groupFilter.addView(chip)
            if (account?.accountId == selected) chip.isChecked = true
        }
    }

    private fun accountLabel(account: BankAccount) =
        getString(HomeViewModel.labelFor(account.type)) + " ••" + account.accountNumber.takeLast(4)

    private fun subtitleFor(tx: Transaction): String {
        // Liên ngân hàng: hiện tên ngân hàng nhận thay cho tài khoản nguồn (mockup F4 màn 6)
        val label = Banks.byCode(tx.counterpartyBank)?.shortName
            ?: accounts[tx.accountId]?.let { getString(HomeViewModel.labelFor(it.type)) } ?: tx.accountNumber
        return label + " · " + TIME_FORMAT.format(Date(tx.createdAt))
    }

    private fun openReceipt(tx: Transaction) {
        val label = accounts[tx.accountId]?.let { getString(HomeViewModel.labelFor(it.type)) + " · " + it.accountNumber } ?: tx.accountNumber
        screenHost.openScreen(ReceiptFragment.newInstance(tx, label, viewOnly = true))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale("vi"))
    }
}
