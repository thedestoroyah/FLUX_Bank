package com.flux.bank.ui.customer.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.RecyclerView
import com.flux.bank.core.format.TextNormalizer
import com.flux.bank.data.model.Bank
import com.flux.bank.data.model.Banks
import com.flux.bank.databinding.ItemRecentPayeeBinding
import com.flux.bank.databinding.SheetBankPickerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Bottom sheet chọn ngân hàng nhận (mockup F4 màn 4); kết quả là mã ngân hàng trong [KEY_CODE]. */
class BankPickerSheet : BottomSheetDialogFragment() {

    private var _binding: SheetBankPickerBinding? = null
    private val binding get() = _binding!!
    private val adapter = BankAdapter { bank ->
        setFragmentResult(requireArguments().getString(ARG_REQUEST_KEY)!!, bundleOf(KEY_CODE to bank.code))
        dismiss()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetBankPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvBanks.adapter = adapter
        adapter.submit(Banks.ALL)
        binding.etSearch.doAfterTextChanged { text ->
            val query = fold(text?.toString().orEmpty().trim())
            adapter.submit(Banks.ALL.filter { query.isEmpty() || fold("${it.code} ${it.shortName} ${it.fullName}").contains(query) })
        }
    }

    private fun fold(text: String) = TextNormalizer.stripDiacritics(text).lowercase()

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvBanks.adapter = null
        _binding = null
    }

    private class BankAdapter(private val onClick: (Bank) -> Unit) : RecyclerView.Adapter<BankAdapter.Holder>() {
        private var items: List<Bank> = emptyList()

        class Holder(val binding: ItemRecentPayeeBinding) : RecyclerView.ViewHolder(binding.root)

        fun submit(list: List<Bank>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemRecentPayeeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val bank = items[position]
            holder.binding.tvInitials.text = bank.code
            holder.binding.tvName.text = bank.shortName
            holder.binding.tvDetail.text = bank.fullName
            holder.binding.root.setOnClickListener { onClick(bank) }
        }
    }

    companion object {
        const val KEY_CODE = "bank_code"
        private const val ARG_REQUEST_KEY = "request_key"

        fun show(fm: FragmentManager, requestKey: String) =
            BankPickerSheet().apply { arguments = bundleOf(ARG_REQUEST_KEY to requestKey) }.show(fm, "bank_picker")
    }
}
