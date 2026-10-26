package com.flux.bank.ui.customer.home

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.flux.bank.databinding.ItemUtilityBinding
import com.flux.bank.ui.customer.Utility

class UtilityGridAdapter(private val onClick: (Utility) -> Unit) :
    RecyclerView.Adapter<UtilityGridAdapter.Holder>() {

    private val items = Utility.entries

    class Holder(val binding: ItemUtilityBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemUtilityBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        with(holder.binding) {
            ivIcon.setImageResource(item.icon)
            ImageViewCompat.setImageTintList(ivIcon, ColorStateList.valueOf(ContextCompat.getColor(context, item.tint)))
            ivIcon.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, item.tileBg))
            tvLabel.setText(item.label)
            root.contentDescription = context.getString(item.label)
            root.setOnClickListener { onClick(item) }
        }
    }
}
