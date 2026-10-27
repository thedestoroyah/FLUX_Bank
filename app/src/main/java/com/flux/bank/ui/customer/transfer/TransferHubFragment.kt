package com.flux.bank.ui.customer.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.flux.bank.R
import com.flux.bank.data.model.Banks
import com.flux.bank.databinding.FragmentTransferHubBinding
import com.flux.bank.databinding.ItemRecentPayeeBinding
import com.flux.bank.databinding.ItemTransferOptionBinding
import com.flux.bank.ui.common.ComingSoonFragment
import com.flux.bank.ui.common.screenHost
import com.flux.bank.ui.customer.home.HomeViewModel

/** Tab Chuyển tiền: ba cách chuyển và người nhận gần đây (mockup F4 màn 1). */
class TransferHubFragment : Fragment() {

    private var _binding: FragmentTransferHubBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransferHubViewModel by viewModels()
    private val adapter = RecentAdapter { open(if (it.bankCode == Banks.FLUX) TransferMode.INTERNAL else TransferMode.EXTERNAL, it) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransferHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.optInternal.bind(R.string.transfer_internal, R.string.transfer_internal_hint, R.drawable.ic_arrows_swap, R.color.cat_emerald, R.color.cat_emerald_bg)
        binding.optExternal.bind(R.string.transfer_external, R.string.transfer_external_hint, R.drawable.ic_bank, R.color.cat_blue, R.color.cat_blue_bg)
        binding.optOwn.bind(R.string.transfer_own, R.string.transfer_own_hint, R.drawable.ic_piggy, R.color.cat_gold, R.color.cat_gold_bg)
        binding.optInternal.root.setOnClickListener { open(TransferMode.INTERNAL) }
        binding.optExternal.root.setOnClickListener { open(TransferMode.EXTERNAL) }
        binding.optOwn.root.setOnClickListener { open(TransferMode.OWN) }
        binding.rvRecent.adapter = adapter
        viewModel.recent.observe(viewLifecycleOwner) { list ->
            adapter.submit(list)
            binding.tvRecentLabel.isVisible = list.isNotEmpty()
            binding.cardRecent.isVisible = list.isNotEmpty()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun open(mode: TransferMode, recent: RecentPayee? = null) {
        // Task 8 thay bằng TransferFragment.newInstance(mode, prefill từ recent)
        screenHost.openScreen(ComingSoonFragment.newInstance(R.string.tab_transfer, R.drawable.ic_arrows_swap, R.color.cat_blue))
    }

    private fun ItemTransferOptionBinding.bind(
        @StringRes title: Int, @StringRes subtitle: Int, @DrawableRes icon: Int, @ColorRes tint: Int, @ColorRes bg: Int
    ) {
        tvTitle.setText(title)
        tvSubtitle.setText(subtitle)
        ivIcon.setImageResource(icon)
        ivIcon.backgroundTintList = ContextCompat.getColorStateList(requireContext(), bg)
        ImageViewCompat.setImageTintList(ivIcon, ContextCompat.getColorStateList(requireContext(), tint))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvRecent.adapter = null
        _binding = null
    }

    private class RecentAdapter(private val onClick: (RecentPayee) -> Unit) : RecyclerView.Adapter<RecentAdapter.Holder>() {
        private var items: List<RecentPayee> = emptyList()

        class Holder(val binding: ItemRecentPayeeBinding) : RecyclerView.ViewHolder(binding.root)

        fun submit(list: List<RecentPayee>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemRecentPayeeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val payee = items[position]
            val context = holder.binding.root.context
            holder.binding.tvInitials.text = HomeViewModel.initials(payee.name)
            holder.binding.tvName.text = payee.name
            holder.binding.tvDetail.text = context.getString(
                R.string.receipt_bank_account,
                Banks.byCode(payee.bankCode)?.shortName ?: context.getString(R.string.bank_flux), payee.accountNumber
            )
            holder.binding.root.setOnClickListener { onClick(payee) }
        }
    }
}
