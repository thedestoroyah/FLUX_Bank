package com.flux.bank.ui.common

import android.view.LayoutInflater
import android.widget.LinearLayout
import com.flux.bank.databinding.ItemKeyValueBinding

/** Thay toàn bộ dòng trong [container] bằng các cặp (nhãn, giá trị) — dùng cho thẻ thông tin khoản vay, biên lai. */
fun LinearLayout.setKeyValueRows(rows: List<Pair<String, String>>) {
    removeAllViews()
    val inflater = LayoutInflater.from(context)
    rows.forEach { (key, value) ->
        ItemKeyValueBinding.inflate(inflater, this, true).apply {
            tvKey.text = key
            tvValue.text = value
        }
    }
}
