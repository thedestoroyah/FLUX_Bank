package com.flux.bank.ui.common

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Đẩy nội dung bottom sheet lên trên bàn phím: cộng chiều cao bàn phím vào padding dưới.
 * Cần vì cửa sổ edge-to-edge (targetSdk ≥ 35) bỏ qua adjustResize, bàn phím che mất ô nhập và nút Lưu.
 */
fun View.padAboveKeyboard() {
    val base = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        v.updatePadding(bottom = base + insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)
        insets
    }
}
