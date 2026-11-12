package com.flux.bank.ui.customer.assistant

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.flux.bank.databinding.ItemChatActionBinding
import com.flux.bank.databinding.ItemChatBotBinding
import com.flux.bank.databinding.ItemChatTypingBinding
import com.flux.bank.databinding.ItemChatUserBinding
import com.flux.bank.domain.assistant.AssistantAction

/**
 * Danh sách tin của màn Trợ lý: tin khách, tin bot (kèm nút hành động), bong bóng đang trả lời.
 *
 * @param month số tháng hiện tại cho nhãn "Xem giao dịch tháng N".
 * @param onAction gọi khi khách chạm một nút hành động.
 */
class ChatAdapter(
    private val month: () -> Int,
    private val onAction: (AssistantAction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ChatItem> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<ChatItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ChatItem.User -> TYPE_USER
        is ChatItem.Bot -> TYPE_BOT
        ChatItem.Typing -> TYPE_TYPING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> Holder(ItemChatUserBinding.inflate(inflater, parent, false).root)
            TYPE_BOT -> BotHolder(ItemChatBotBinding.inflate(inflater, parent, false))
            else -> TypingHolder(ItemChatTypingBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ChatItem.User -> ItemChatUserBinding.bind(holder.itemView).tvText.text = item.text
            is ChatItem.Bot -> (holder as BotHolder).bind(item)
            ChatItem.Typing -> (holder as TypingHolder).start()
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder as? TypingHolder)?.stop()
    }

    private class Holder(view: View) : RecyclerView.ViewHolder(view)

    private inner class BotHolder(private val binding: ItemChatBotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatItem.Bot) {
            binding.tvText.text = item.reply.text
            binding.tvBasic.isVisible = item.reply.basicMode
            binding.actions.removeAllViews()
            binding.actions.isVisible = item.reply.actions.isNotEmpty()
            item.reply.actions.forEach { binding.actions.addView(actionRow(it)) }
        }

        private fun actionRow(action: AssistantAction): View {
            val context = binding.root.context
            val row = ItemChatActionBinding.inflate(LayoutInflater.from(context), binding.actions, false)
            val visual = AssistantActionUi.visualFor(action)
            row.ivIcon.setImageResource(visual.icon)
            ImageViewCompat.setImageTintList(row.ivIcon, ColorStateList.valueOf(ContextCompat.getColor(context, visual.tint)))
            row.ivIcon.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, visual.tileBg))
            row.tvLabel.text = if (action == AssistantAction.MONTH_HISTORY) context.getString(visual.label, month()) else context.getString(visual.label)
            row.root.setOnClickListener { onAction(action) }
            return row.root
        }
    }

    private class TypingHolder(private val binding: ItemChatTypingBinding) : RecyclerView.ViewHolder(binding.root) {
        private var pulse: ObjectAnimator? = null

        // Không chạy hiệu ứng khi người dùng tắt animation trong cài đặt (giảm chuyển động)
        fun start() {
            if (!ValueAnimator.areAnimatorsEnabled() || pulse != null) return
            pulse = ObjectAnimator.ofFloat(binding.tvDots, View.ALPHA, 1f, PULSE_MIN_ALPHA).apply {
                duration = PULSE_MS
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        }

        fun stop() {
            pulse?.cancel()
            pulse = null
            binding.tvDots.alpha = 1f
        }
    }

    private companion object {
        const val TYPE_USER = 0
        const val TYPE_BOT = 1
        const val TYPE_TYPING = 2
        const val PULSE_MS = 600L
        const val PULSE_MIN_ALPHA = 0.35f
    }
}
