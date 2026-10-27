package com.flux.bank.ui.officer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.flux.bank.R
import com.flux.bank.databinding.ActivityOfficerMainBinding
import com.flux.bank.ui.common.ScreenHost
import com.flux.bank.ui.common.markAsSubScreen
import com.flux.bank.ui.common.trackSubScreens
import com.flux.bank.ui.customer.profile.ProfileFragment
import com.flux.bank.ui.officer.customers.CustomerListFragment
import com.flux.bank.ui.officer.ekyc.EkycReviewFragment
import com.flux.bank.ui.officer.overview.OfficerOverviewFragment
import com.flux.bank.ui.officer.rates.RatesFragment

/** Khung app nhân viên: Tổng quan · Khách hàng · eKYC · Lãi suất · Cá nhân. */
class OfficerMainActivity : AppCompatActivity(), ScreenHost {

    enum class OfficerTab(val menuId: Int) {
        OVERVIEW(R.id.otabOverview), CUSTOMERS(R.id.otabCustomers), EKYC(R.id.otabEkyc),
        RATES(R.id.otabRates), PROFILE(R.id.otabProfile)
    }

    private lateinit var binding: ActivityOfficerMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfficerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bottomNav.setOnItemSelectedListener { item ->
            val tab = OfficerTab.entries.firstOrNull { it.menuId == item.itemId } ?: return@setOnItemSelectedListener false
            replaceRoot(fragmentFor(tab))
            true
        }
        trackSubScreens(R.id.container, ::syncChrome)
        if (savedInstanceState == null) showTab(OfficerTab.OVERVIEW)
    }

    /** Ẩn thanh tab khi đang ở màn phụ. */
    private fun syncChrome(sub: Boolean) {
        binding.bottomNav.isVisible = !sub
        binding.navDivider.isVisible = !sub
    }

    fun showTab(tab: OfficerTab) {
        binding.bottomNav.selectedItemId = tab.menuId
    }

    override fun openScreen(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            replace(R.id.container, fragment.markAsSubScreen())
            addToBackStack(null)
        }
    }

    private fun replaceRoot(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.container, fragment)
        }
    }

    private fun fragmentFor(tab: OfficerTab): Fragment = when (tab) {
        OfficerTab.OVERVIEW -> OfficerOverviewFragment()
        OfficerTab.CUSTOMERS -> CustomerListFragment()
        OfficerTab.EKYC -> EkycReviewFragment()
        OfficerTab.RATES -> RatesFragment()
        OfficerTab.PROFILE -> ProfileFragment.newInstance(officer = true)
    }
}
