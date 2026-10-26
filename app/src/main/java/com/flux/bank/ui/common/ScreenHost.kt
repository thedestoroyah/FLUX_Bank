package com.flux.bank.ui.common

import androidx.fragment.app.Fragment

/** Activity chính của mỗi vai trò: mở màn phụ có back stack (ẩn thanh tab). */
interface ScreenHost {
    fun openScreen(fragment: Fragment)
}

/** Lấy host từ một Fragment đang gắn vào Activity chính. */
val Fragment.screenHost: ScreenHost get() = requireActivity() as ScreenHost
