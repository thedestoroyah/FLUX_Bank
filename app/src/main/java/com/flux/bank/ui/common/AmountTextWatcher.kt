package com.flux.bank.ui.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.core.validation.Validators

/**
 * Chèn dấu chấm hàng nghìn khi gõ số tiền ("2000000" → "2.000.000") và báo số tiền mới qua [onAmount].
 * Con trỏ luôn đặt về cuối: đủ dùng vì người dùng gõ số tiền từ trái sang phải.
 */
class AmountTextWatcher(private val field: EditText, private val onAmount: (Long) -> Unit) : TextWatcher {

    private var formatting = false

    override fun afterTextChanged(s: Editable) {
        if (formatting) return
        val amount = Validators.parseAmount(s.toString())
        val text = if (s.isEmpty()) "" else MoneyFormatter.formatVnd(amount).removeSuffix(" ₫")
        if (text != s.toString()) {
            formatting = true
            field.setText(text)
            field.setSelection(text.length)
            formatting = false
        }
        onAmount(amount)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
}
