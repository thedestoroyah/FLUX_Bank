package com.flux.bank.ui.customer.branch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flux.bank.R
import com.flux.bank.core.format.DistanceFormatter
import com.flux.bank.databinding.ItemBranchBinding
import com.flux.bank.domain.branch.RankedBranch

/** Một dòng danh sách: [showDistance] = đã có vị trí người dùng; [isPick] = chi nhánh được gợi ý. */
data class BranchRow(val item: RankedBranch, val showDistance: Boolean, val isPick: Boolean)

/** Danh sách chi nhánh trong sheet mở rộng (mockup F6 màn 2). */
class BranchAdapter(private val onClick: (String) -> Unit) :
    ListAdapter<BranchRow, BranchAdapter.Holder>(Diff) {

    class Holder(val binding: ItemBranchBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemBranchBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
        val row = getItem(position)
        val ctx = root.context
        ivIcon.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.cat_blue_bg)
        ivIcon.imageTintList = ContextCompat.getColorStateList(ctx, R.color.cat_blue)
        tvName.text = row.item.branch.name
        tvAddress.text = row.item.branch.address
        tvBadge.isVisible = row.isPick
        distanceBox.isVisible = row.showDistance
        val walk = row.item.walk
        tvDistance.text = DistanceFormatter.distance(walk?.distanceMeters ?: row.item.straightMeters)
        tvDistanceNote.text = walk?.let { ctx.getString(R.string.branch_minutes, DistanceFormatter.minutes(it.durationSeconds)) }
            ?: ctx.getString(R.string.branch_straight_line)
        root.setOnClickListener { onClick(row.item.branch.id) }
    }

    private object Diff : DiffUtil.ItemCallback<BranchRow>() {
        override fun areItemsTheSame(a: BranchRow, b: BranchRow) = a.item.branch.id == b.item.branch.id
        override fun areContentsTheSame(a: BranchRow, b: BranchRow) = a == b
    }
}
