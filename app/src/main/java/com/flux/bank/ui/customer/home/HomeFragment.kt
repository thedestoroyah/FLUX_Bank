package com.flux.bank.ui.customer.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.flux.bank.R
import com.flux.bank.data.model.KycStatus
import com.flux.bank.databinding.FragmentHomeBinding
import com.flux.bank.ui.common.ComingSoonFragment
import com.flux.bank.ui.customer.CustomerMainActivity
import com.flux.bank.ui.ekyc.EkycCaptureFragment

/** Trang chủ khách hàng: lời chào, thẻ Titanium, nhắc eKYC, lưới tiện ích 4x3, giao dịch gần đây. */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private val cardAdapter = AccountCardAdapter { viewModel.toggleBalance() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        applyStatusBarInset()
        sizeCardToRealRatio()

        binding.pagerCards.adapter = cardAdapter
        binding.pagerCards.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = renderDots(cardAdapter.itemCount, position)
        })
        binding.rvUtilities.layoutManager = GridLayoutManager(requireContext(), GRID_COLUMNS)
        binding.rvUtilities.adapter = UtilityGridAdapter { utility ->
            host().openScreen(ComingSoonFragment.newInstance(utility.label, utility.icon, utility.tint))
        }
        binding.btnBell.setOnClickListener {
            host().openScreen(ComingSoonFragment.newInstance(R.string.cd_notifications, R.drawable.ic_bell, R.color.cat_blue))
        }
        binding.btnSeeAll.setOnClickListener { host().showTab(CustomerMainActivity.CustomerTab.HISTORY) }
        binding.cardKyc.setOnClickListener {
            val uid = com.flux.bank.FluxApp.instance.authRepository.currentUid() ?: return@setOnClickListener
            host().openScreen(EkycCaptureFragment.newInstance(uid, submittedBy = uid))
        }

        viewModel.state.observe(viewLifecycleOwner) { render(it) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun render(state: HomeUiState) {
        binding.skeletonCard.isVisible = state.loading
        // INVISIBLE (không phải GONE) để giữ kích thước thẻ cho skeleton
        binding.pagerCards.visibility = if (state.loading) View.INVISIBLE else View.VISIBLE
        binding.tvGreeting.setText(state.greeting)
        binding.tvName.text = state.firstName
        binding.tvAvatar.text = state.initials
        cardAdapter.submit(state.accounts, state.balanceHidden)
        renderDots(state.accounts.size, binding.pagerCards.currentItem)
        renderKyc(state.kycStatus)
    }

    private fun renderKyc(status: String) {
        binding.cardKyc.isVisible = status != KycStatus.VERIFIED
        binding.tvKycBody.setText(
            when (status) {
                KycStatus.PENDING -> R.string.home_kyc_pending
                KycStatus.REJECTED -> R.string.home_kyc_rejected
                else -> R.string.home_kyc_body
            }
        )
        binding.cardKyc.isClickable = status != KycStatus.PENDING
    }

    /** Chấm chỉ trang: chấm đang chọn dài và màu emerald. */
    private fun renderDots(count: Int, selected: Int) {
        val dots = binding.dots
        dots.removeAllViews()
        if (count <= 1) return
        val density = resources.displayMetrics.density
        repeat(count) { i ->
            val active = i == selected
            dots.addView(View(requireContext()).apply {
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_dot)
                backgroundTintList = ContextCompat.getColorStateList(
                    requireContext(), if (active) R.color.flux_emerald else R.color.flux_text_tertiary
                )
                layoutParams = LinearLayout.LayoutParams(((if (active) 16 else 5) * density).toInt(), (5 * density).toInt())
                    .apply { marginEnd = (5 * density).toInt() }
            })
        }
    }

    /**
     * Chiều cao pager sao cho thẻ bên trong đúng tỉ lệ thẻ thật 1.586:1.
     * Bề rộng thẻ = bề rộng màn hình - padding pager (2x16dp) - padding item (2x4dp); item còn đệm trên 4dp, dưới 12dp.
     */
    private fun sizeCardToRealRatio() {
        val density = resources.displayMetrics.density
        val cardWidth = resources.displayMetrics.widthPixels - (2 * 16 + 2 * 4) * density
        val height = cardWidth / CARD_RATIO + (4 + 12) * density
        binding.pagerCards.updateLayoutParams<ViewGroup.LayoutParams> { this.height = height.toInt() }
        // Cho bóng đổ của thẻ tràn ra ngoài vùng RecyclerView bên trong pager
        (binding.pagerCards.getChildAt(0) as? ViewGroup)?.apply {
            clipChildren = false
            clipToPadding = false
        }
    }

    private fun applyStatusBarInset() {
        val baseMargin = resources.getDimensionPixelSize(R.dimen.space_12)
        ViewCompat.setOnApplyWindowInsetsListener(binding.tvAvatar) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = top + baseMargin }
            insets
        }
    }

    private fun host() = requireActivity() as CustomerMainActivity

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val GRID_COLUMNS = 4
        const val CARD_RATIO = 1.586f
    }
}
