package com.flux.bank.ui.customer.utility

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.databinding.ItemCatalogOfferBinding
import com.flux.bank.databinding.ItemCatalogProductBinding
import com.flux.bank.domain.utility.CatalogItem
import com.flux.bank.domain.utility.CatalogKind
import com.flux.bank.ui.customer.Utility
import com.google.android.material.card.MaterialCardView

/** Danh sách mục của một danh mục: dòng (chuyến bay, phim, khách sạn) hoặc ô lưới 2 cột (sản phẩm). Chọn một mục. */
class CatalogItemAdapter(
    private val kind: CatalogKind,
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val utility = Utility.fromService(kind.service)!!
    private var items: List<CatalogItem> = emptyList()
    private var selectedId: String? = null

    class OfferHolder(val binding: ItemCatalogOfferBinding) : RecyclerView.ViewHolder(binding.root)
    class ProductHolder(val binding: ItemCatalogProductBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<CatalogItem>, selected: String?) {
        if (list == items && selected == selectedId) return
        items = list
        selectedId = selected
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (kind == CatalogKind.SHOP) ProductHolder(ItemCatalogProductBinding.inflate(inflater, parent, false))
        else OfferHolder(ItemCatalogOfferBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        val price = MoneyFormatter.formatVnd(item.unitPrice)
        when (holder) {
            is OfferHolder -> with(holder.binding) {
                ivIcon.setImageResource(utility.icon)
                ivIcon.backgroundTintList = ContextCompat.getColorStateList(context, utility.tileBg)
                ImageViewCompat.setImageTintList(ivIcon, ContextCompat.getColorStateList(context, utility.tint))
                tvTitle.text = item.title
                tvSubtitle.text = if (item.stars > 0) {
                    context.getString(R.string.catalog_stars, STAR.repeat(item.stars), item.subtitle)
                } else item.subtitle
                tvPrice.text = price
                tvPer.isVisible = perRes() != null
                perRes()?.let(tvPer::setText)
            }
            is ProductHolder -> with(holder.binding) {
                ivArt.setImageResource(utility.icon)
                ivArt.backgroundTintList = ContextCompat.getColorStateList(context, utility.tileBg)
                ImageViewCompat.setImageTintList(ivArt, ContextCompat.getColorStateList(context, utility.tint))
                tvTitle.text = item.title
                tvPrice.text = price
            }
        }
        markSelected(holder.itemView as MaterialCardView, item.id == selectedId)
        holder.itemView.setOnClickListener { onSelect(item.id) }
    }

    private fun markSelected(card: MaterialCardView, selected: Boolean) {
        card.isChecked = selected
        val stroke = card.resources.getDimensionPixelSize(R.dimen.stroke_hairline)
        // Mục đang chọn viền emerald dày hơn (1.5dp như ô nhập đang focus)
        card.strokeWidth = if (selected) stroke * 3 / 2 else stroke
    }

    private fun perRes(): Int? = when (kind) {
        CatalogKind.FLIGHT -> R.string.catalog_per_flight
        CatalogKind.MOVIE -> R.string.catalog_per_movie
        CatalogKind.HOTEL -> R.string.catalog_per_hotel
        CatalogKind.SHOP -> null
    }

    private companion object {
        const val STAR = "★"
    }
}
