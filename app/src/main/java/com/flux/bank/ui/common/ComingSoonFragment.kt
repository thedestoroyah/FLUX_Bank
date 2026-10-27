package com.flux.bank.ui.common

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.flux.bank.R
import com.flux.bank.databinding.FragmentComingSoonBinding

/** Màn "Sắp ra mắt" dùng chung cho các tiện ích/tab chưa làm. Mở dạng tab thì không có nút quay lại. */
class ComingSoonFragment : Fragment() {

    private var _binding: FragmentComingSoonBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentComingSoonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        val label = getString(args.getInt(ARG_LABEL))
        val tint = ContextCompat.getColor(requireContext(), args.getInt(ARG_TINT))
        val showBack = args.getBoolean(ARG_SHOW_BACK)

        binding.header.tvHeaderTitle.text = label
        binding.header.btnBack.isVisible = showBack
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.ivIcon.setImageResource(args.getInt(ARG_ICON))
        ImageViewCompat.setImageTintList(binding.ivIcon, ColorStateList.valueOf(tint))
        // Nền ô icon: cùng màu với icon nhưng rất nhạt
        binding.ivIcon.backgroundTintList = ColorStateList.valueOf(tint).withAlpha(BG_ALPHA)
        val messageRes = args.getInt(ARG_MESSAGE)
        binding.tvBody.text = if (messageRes != 0) getString(messageRes) else getString(R.string.coming_soon_body, label)
        args.getInt(ARG_TITLE).takeIf { it != 0 }?.let(binding.tvTitle::setText)
        binding.btnHome.isVisible = showBack
        binding.btnHome.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_LABEL = "label"
        private const val ARG_ICON = "icon"
        private const val ARG_TINT = "tint"
        private const val ARG_SHOW_BACK = "show_back"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val BG_ALPHA = 26

        /** [titleRes]/[messageRes] khác 0 thì thay chữ "Sắp ra mắt" — dùng làm empty state (ví dụ chưa có sổ tiết kiệm). */
        fun newInstance(
            @StringRes label: Int,
            @DrawableRes icon: Int,
            @ColorRes tint: Int,
            showBack: Boolean = true,
            @StringRes titleRes: Int = 0,
            @StringRes messageRes: Int = 0
        ) = ComingSoonFragment().apply {
            arguments = bundleOf(
                ARG_LABEL to label, ARG_ICON to icon, ARG_TINT to tint, ARG_SHOW_BACK to showBack,
                ARG_TITLE to titleRes, ARG_MESSAGE to messageRes
            )
        }
    }
}
