package com.flux.bank.ui.common

import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.flux.bank.databinding.ViewSmsBannerBinding

/** Banner kiểu tin nhắn SMS hiện ở đầu màn hình khi không được phép đăng notification. */
object SimulatedSmsBanner {

    private const val SHOW_MS = 8_000L

    fun show(activity: Activity, text: String, onTap: () -> Unit) {
        val root = activity.window.decorView as ViewGroup
        val binding = ViewSmsBannerBinding.inflate(LayoutInflater.from(activity), root, false)
        binding.tvSmsBody.text = text
        val topInset = ViewCompat.getRootWindowInsets(root)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        binding.root.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP
        ).apply { topMargin = topInset }
        val dismiss = { root.removeView(binding.root) }
        binding.root.setOnClickListener {
            onTap()
            dismiss()
        }
        root.addView(binding.root)
        binding.root.postDelayed({ if (binding.root.parent != null) dismiss() }, SHOW_MS)
    }
}
