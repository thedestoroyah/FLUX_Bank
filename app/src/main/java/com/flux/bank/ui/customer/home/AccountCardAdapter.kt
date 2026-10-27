package com.flux.bank.ui.customer.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flux.bank.R
import com.flux.bank.databinding.ItemAccountCardBinding

/** Trang của ViewPager2: mỗi trang là một thẻ Titanium. */
class AccountCardAdapter(
    private val onToggleBalance: () -> Unit,
    private val onCardClick: (String) -> Unit
) :
    RecyclerView.Adapter<AccountCardAdapter.Holder>() {

    private var items: List<AccountCardUi> = emptyList()
    private var hidden = false

    class Holder(val binding: ItemAccountCardBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(cards: List<AccountCardUi>, balanceHidden: Boolean) {
        items = cards
        hidden = balanceHidden
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAccountCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val card = items[position]
        with(holder.binding) {
            tvType.setText(card.typeLabel)
            tvBalance.text = card.balanceText
            tvNumber.text = card.maskedNumber
            btnEye.setImageResource(if (hidden) R.drawable.ic_eye_slash else R.drawable.ic_eye)
            btnEye.setOnClickListener { onToggleBalance() }
            tvBalanceLabel.setText(card.balanceLabel)
            this.card.setOnClickListener { onCardClick(card.accountId) }
        }
    }
}
