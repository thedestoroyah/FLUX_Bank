package com.flux.bank.ui.customer.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.flux.bank.R
import com.flux.bank.data.model.Transaction
import com.flux.bank.databinding.ItemHistoryHeaderBinding
import com.flux.bank.databinding.ItemTransactionBinding
import com.flux.bank.ui.customer.account.bind
import java.time.format.DateTimeFormatter

/** Danh sách lịch sử: tiêu đề ngày xen giữa các dòng giao dịch. */
class HistoryAdapter(
    private val subtitle: (Transaction) -> String,
    private val onClick: (Transaction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rows: List<HistoryRow> = emptyList()

    private class HeaderHolder(val binding: ItemHistoryHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    private class ItemHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<HistoryRow>) {
        rows = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = rows.size

    override fun getItemViewType(position: Int) = if (rows[position] is HistoryRow.Header) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) HeaderHolder(ItemHistoryHeaderBinding.inflate(inflater, parent, false))
        else ItemHolder(ItemTransactionBinding.inflate(inflater, parent, false).apply {
            // Dòng giao dịch nằm trên nền trắng như các tile, tiêu đề ngày nằm trên nền xám
            root.setBackgroundColor(ContextCompat.getColor(parent.context, R.color.flux_surface))
            val pad = parent.resources.getDimensionPixelSize(R.dimen.space_12)
            root.setPaddingRelative(pad, root.paddingTop, pad, root.paddingBottom)
        })
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is HistoryRow.Header -> (holder as HeaderHolder).binding.tvDay.text = when (val day = row.day) {
                DayLabel.Today -> holder.itemView.context.getString(R.string.history_today)
                DayLabel.Yesterday -> holder.itemView.context.getString(R.string.history_yesterday)
                is DayLabel.On -> day.date.format(DATE_FORMAT)
            }
            is HistoryRow.Item -> with((holder as ItemHolder).binding) {
                bind(row.tx, subtitle(row.tx))
                root.setOnClickListener { onClick(row.tx) }
            }
        }
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
