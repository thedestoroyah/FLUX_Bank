package com.flux.bank.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.flux.bank.R

/** 6 ô hiển thị mã OTP; ô kế tiếp cần nhập có viền emerald. Chỉ hiển thị, không nhận bàn phím hệ thống. */
class OtpCellsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val cells: List<TextView>

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        val density = resources.displayMetrics.density
        cells = List(CELL_COUNT) {
            TextView(context).apply {
                gravity = Gravity.CENTER
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Flux_TitleL)
                setTextColor(ContextCompat.getColor(context, R.color.flux_text_primary))
                background = ContextCompat.getDrawable(context, R.drawable.bg_otp_cell)
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                layoutParams = LayoutParams((44 * density).toInt(), (52 * density).toInt()).apply {
                    marginStart = (4 * density).toInt()
                    marginEnd = (4 * density).toInt()
                }
            }.also(::addView)
        }
        render("")
    }

    fun render(digits: String) {
        cells.forEachIndexed { i, cell ->
            cell.text = digits.getOrNull(i)?.toString().orEmpty()
            cell.isActivated = i == digits.length
        }
        contentDescription = digits.toList().joinToString(" ")
    }

    private companion object {
        const val CELL_COUNT = 6
    }
}
