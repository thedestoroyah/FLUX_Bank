package com.flux.bank.ui.customer

import androidx.fragment.app.Fragment
import com.flux.bank.R
import com.flux.bank.domain.utility.BillKind
import com.flux.bank.domain.utility.CatalogKind
import com.flux.bank.ui.common.ComingSoonFragment
import com.flux.bank.ui.customer.account.AccountDetailFragment
import com.flux.bank.ui.customer.account.MoneyMoveFragment
import com.flux.bank.ui.customer.assistant.AssistantFragment
import com.flux.bank.ui.customer.branch.BranchMapFragment
import com.flux.bank.ui.customer.utility.BillPayFragment
import com.flux.bank.ui.customer.utility.CatalogFragment
import com.flux.bank.ui.customer.utility.TopUpFragment

/** Màn mở ra khi chạm một ô tiện ích; trang chủ và Trợ lý (F7) dùng chung để luôn mở cùng một màn. */
object UtilityScreens {

    /** [savingAccountId] null = khách chưa có tài khoản tiết kiệm (ô Tiết kiệm mở màn "chưa có"). */
    fun screenFor(utility: Utility, savingAccountId: String?): Fragment = when (utility) {
        Utility.NAP_TIEN -> MoneyMoveFragment.newInstance(deposit = true)
        Utility.RUT_TIEN -> MoneyMoveFragment.newInstance(deposit = false)
        Utility.TIEN_DIEN -> BillPayFragment.newInstance(BillKind.ELECTRICITY)
        Utility.TIEN_NUOC -> BillPayFragment.newInstance(BillKind.WATER)
        Utility.NAP_DT -> TopUpFragment()
        Utility.VE_MAY_BAY -> CatalogFragment.newInstance(CatalogKind.FLIGHT)
        Utility.VE_PHIM -> CatalogFragment.newInstance(CatalogKind.MOVIE)
        Utility.KHACH_SAN -> CatalogFragment.newInstance(CatalogKind.HOTEL)
        Utility.MUA_SAM -> CatalogFragment.newInstance(CatalogKind.SHOP)
        Utility.CHI_NHANH -> BranchMapFragment()
        Utility.TIET_KIEM -> savingAccountId?.let(AccountDetailFragment::newInstance)
            ?: ComingSoonFragment.newInstance(
                utility.label, utility.icon, utility.tint, titleRes = R.string.saving_none_title, messageRes = R.string.saving_none
            )
        Utility.TRO_LY_AI -> AssistantFragment()
    }
}
