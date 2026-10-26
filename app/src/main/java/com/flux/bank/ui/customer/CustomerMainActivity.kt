package com.flux.bank.ui.customer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.flux.bank.R
import com.flux.bank.databinding.ActivityCustomerMainBinding
import com.flux.bank.ui.common.ComingSoonFragment
import com.flux.bank.ui.common.ScreenHost
import com.flux.bank.ui.customer.home.HomeFragment
import com.flux.bank.ui.customer.profile.ProfileFragment

/**
 * Khung app khách hàng: 5 tab (Trang chủ · Chuyển tiền · [Quét QR] · Lịch sử · Cá nhân).
 * Màn phụ mở bằng [openScreen] (có back stack, ẩn thanh tab).
 */
class CustomerMainActivity : AppCompatActivity(), ScreenHost {

    enum class CustomerTab(val menuId: Int) {
        HOME(R.id.tabHome), TRANSFER(R.id.tabTransfer), HISTORY(R.id.tabHistory), PROFILE(R.id.tabProfile)
    }

    private lateinit var binding: ActivityCustomerMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            val tab = CustomerTab.entries.firstOrNull { it.menuId == item.itemId } ?: return@setOnItemSelectedListener false
            replaceRoot(fragmentFor(tab))
            true
        }
        binding.btnQr.setOnClickListener {
            openScreen(ComingSoonFragment.newInstance(R.string.tab_qr, R.drawable.ic_scan, R.color.cat_emerald))
        }
        supportFragmentManager.addOnBackStackChangedListener { syncChrome() }
        if (savedInstanceState == null) {
            showTab(CustomerTab.HOME)
            requestNotificationPermission()
        }
    }

    private val notificationPermission =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    /** Android 13+: xin quyền thông báo để nhận SMS giả lập chứa OTP (từ chối thì dùng banner trong app). */
    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun showTab(tab: CustomerTab) {
        binding.bottomNav.selectedItemId = tab.menuId
    }

    /** Mở một màn phụ phủ lên tab hiện tại; nút back sẽ quay lại. */
    override fun openScreen(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            replace(R.id.container, fragment)
            addToBackStack(null)
        }
    }

    private fun replaceRoot(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.container, fragment)
        }
    }

    private fun fragmentFor(tab: CustomerTab): Fragment = when (tab) {
        CustomerTab.HOME -> HomeFragment()
        CustomerTab.TRANSFER -> ComingSoonFragment.newInstance(R.string.tab_transfer, R.drawable.ic_arrows_swap, R.color.cat_blue, showBack = false)
        CustomerTab.HISTORY -> ComingSoonFragment.newInstance(R.string.tab_history, R.drawable.ic_history, R.color.cat_blue, showBack = false)
        CustomerTab.PROFILE -> ProfileFragment.newInstance()
    }

    /** Ẩn thanh tab và nút QR khi đang ở màn phụ. */
    private fun syncChrome() {
        val inSubScreen = supportFragmentManager.backStackEntryCount > 0
        binding.bottomNav.isVisible = !inSubScreen
        binding.btnQr.isVisible = !inSubScreen
        binding.navDivider.isVisible = !inSubScreen
    }
}
