package com.flux.bank.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.widget.GridLayout
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import com.flux.bank.R
import com.google.android.material.button.MaterialButton

/**
 * Bàn phím số vẽ trong app (4x3: 1-9, trống, 0, xoá). Dùng thay bàn phím hệ thống để bottom sheet
 * không bị đẩy lên và bàn phím bên thứ ba không đọc được mã.
 */
class NumericKeypadView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    GridLayout(context, attrs) {

    var onDigit: (Char) -> Unit = {}
    var onBackspace: () -> Unit = {}

    init {
        columnCount = 3
        val keys = listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', ' ', '0', '<')
        val height = (56 * resources.displayMetrics.density).toInt()
        keys.forEach { key ->
            val view = when (key) {
                '<' -> ImageButton(context).apply {
                    setImageResource(R.drawable.ic_backspace)
                    ImageViewCompat.setImageTintList(this, ContextCompat.getColorStateList(context, R.color.flux_text_primary))
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    contentDescription = context.getString(R.string.cd_backspace)
                    setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onBackspace() }
                }
                ' ' -> android.view.View(context)
                else -> MaterialButton(context, null, androidx.appcompat.R.attr.borderlessButtonStyle).apply {
                    text = key.toString()
                    TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Flux_TitleL)
                    setTextColor(ContextCompat.getColor(context, R.color.flux_text_primary))
                    insetTop = 0
                    insetBottom = 0
                    setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onDigit(key) }
                }
            }
            addView(view, LayoutParams(spec(UNDEFINED, 1f), spec(UNDEFINED, 1f)).apply {
                width = 0
                this.height = height
            })
        }
    }
}
