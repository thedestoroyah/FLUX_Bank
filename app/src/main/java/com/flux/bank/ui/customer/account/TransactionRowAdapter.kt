package com.flux.bank.ui.customer.account

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.core.format.TextNormalizer
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import com.flux.bank.databinding.ItemTransactionBinding

/**
 * Danh sách dòng giao dịch dùng chung (chi tiết tài khoản, trang chủ).
 * [subtitle] do màn hình gọi quyết định (giờ, ngày hay tên tài khoản).
 */
class TransactionRowAdapter(
    private val subtitle: (Transaction) -> String,
    private val onClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionRowAdapter.Holder>() {

    private var items: List<Transaction> = emptyList()

    /** Nhãn một dòng giao dịch: chuỗi [res], có thể kèm [name] (tên bên kia, in hoa không dấu) làm tham số. */
    data class TxLabel(@StringRes val res: Int, val name: String? = null)

    class Holder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<Transaction>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val tx = items[position]
        holder.binding.bind(tx, subtitle(tx))
        holder.binding.root.setOnClickListener { onClick(tx) }
    }

    companion object {
        fun labelFor(tx: Transaction): TxLabel = when (tx.type) {
            TransactionType.DEPOSIT -> TxLabel(if (tx.paymentRef != null) R.string.tx_deposit_vnpay else R.string.util_deposit)
            TransactionType.WITHDRAW -> TxLabel(R.string.util_withdraw)
            TransactionType.TRANSFER_OUT -> TxLabel(R.string.tx_transfer_out, TextNormalizer.holderName(tx.counterpartyName))
            TransactionType.TRANSFER_IN -> TxLabel(R.string.tx_transfer_in, TextNormalizer.holderName(tx.counterpartyName))
            TransactionType.PAYMENT -> TxLabel(R.string.tx_payment_title, tx.description)
        }

        fun title(context: Context, tx: Transaction): String =
            labelFor(tx).let { label -> label.name?.let { context.getString(label.res, it) } ?: context.getString(label.res) }

        /** "+2.000.000 ₫" cho tiền vào, "−500.000 ₫" cho tiền ra. */
        fun signedAmount(tx: Transaction): String =
            MoneyFormatter.formatSignedVnd(if (tx.type.isIncoming) tx.amount else -tx.amount) + " ₫"
    }
}

/** Gán dữ liệu cho một dòng giao dịch; tách ra để lịch sử (adapter khác) dùng lại. */
fun ItemTransactionBinding.bind(tx: Transaction, subtitle: String) {
    val context = root.context
    val incoming = tx.type.isIncoming
    ivIcon.setImageResource(if (incoming) R.drawable.ic_plus else R.drawable.ic_arrow_down)
    ivIcon.backgroundTintList = ContextCompat.getColorStateList(context, if (incoming) R.color.cat_emerald_bg else R.color.flux_border_subtle)
    ImageViewCompat.setImageTintList(
        ivIcon, ColorStateList.valueOf(ContextCompat.getColor(context, if (incoming) R.color.flux_emerald_dark else R.color.flux_text_secondary))
    )
    tvTitle.text = TransactionRowAdapter.title(context, tx)
    tvSubtitle.text = subtitle
    tvAmount.text = TransactionRowAdapter.signedAmount(tx)
    tvAmount.setTextColor(ContextCompat.getColor(context, if (incoming) R.color.flux_emerald_dark else R.color.flux_text_primary))
}
