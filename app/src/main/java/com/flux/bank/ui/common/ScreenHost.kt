package com.flux.bank.ui.common

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

/** Activity chính của mỗi vai trò: mở màn phụ có back stack (ẩn thanh tab). */
interface ScreenHost {
    fun openScreen(fragment: Fragment)
}

/** Lấy host từ một Fragment đang gắn vào Activity chính. */
val Fragment.screenHost: ScreenHost get() = requireActivity() as ScreenHost

private const val ARG_SUB_SCREEN = "flux_sub_screen"

/** Đánh dấu [this] là màn phụ (mở bằng [ScreenHost.openScreen]); trả lại chính fragment để dùng trong `replace`. */
fun Fragment.markAsSubScreen(): Fragment = apply {
    arguments = (arguments ?: Bundle()).apply { putBoolean(ARG_SUB_SCREEN, true) }
}

/**
 * Gọi [onShown] (true = màn phụ) mỗi khi một fragment trong [containerId] được resume.
 * Không dựa vào `backStackEntryCount` trong back stack listener: khi vuốt back (predictive back)
 * listener nhận số cũ nên thanh tab bị ẩn mãi sau khi đã về màn tab.
 */
fun FragmentActivity.trackSubScreens(@IdRes containerId: Int, onShown: (isSubScreen: Boolean) -> Unit) {
    supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            if (f.id == containerId) onShown(f.arguments?.getBoolean(ARG_SUB_SCREEN) == true)
        }
    }, false)
}
