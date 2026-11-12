package com.flux.bank.ui.customer.assistant

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.flux.bank.R
import com.flux.bank.domain.assistant.AssistantAction
import com.flux.bank.ui.customer.Utility

/** Nhãn, icon và màu của một nút hành động (duotone như lưới tiện ích). Nhãn MONTH_HISTORY có `%d` = số tháng. */
data class ActionVisual(@StringRes val label: Int, @DrawableRes val icon: Int, @ColorRes val tint: Int, @ColorRes val tileBg: Int)

/** Nối action của Trợ lý với ô tiện ích của trang chủ, để nút mở cùng màn và mang cùng icon/màu. */
object AssistantActionUi {

    /** Ô tiện ích tương ứng; null với TRANSFER và MONTH_HISTORY (màn riêng, không phải ô tiện ích). */
    fun utilityFor(action: AssistantAction): Utility? = when (action) {
        AssistantAction.TOPUP -> Utility.NAP_DT
        AssistantAction.BILL_ELECTRICITY -> Utility.TIEN_DIEN
        AssistantAction.BILL_WATER -> Utility.TIEN_NUOC
        AssistantAction.DEPOSIT -> Utility.NAP_TIEN
        AssistantAction.WITHDRAW -> Utility.RUT_TIEN
        AssistantAction.SAVING -> Utility.TIET_KIEM
        AssistantAction.FLIGHT -> Utility.VE_MAY_BAY
        AssistantAction.MOVIE -> Utility.VE_PHIM
        AssistantAction.HOTEL -> Utility.KHACH_SAN
        AssistantAction.SHOP -> Utility.MUA_SAM
        AssistantAction.BRANCH -> Utility.CHI_NHANH
        AssistantAction.TRANSFER, AssistantAction.MONTH_HISTORY -> null
    }

    fun visualFor(action: AssistantAction): ActionVisual {
        val label = LABELS.getValue(action)
        val utility = utilityFor(action)
        return when {
            utility != null -> ActionVisual(label, utility.icon, utility.tint, utility.tileBg)
            action == AssistantAction.TRANSFER -> ActionVisual(label, R.drawable.ic_arrows_swap, R.color.cat_emerald, R.color.cat_emerald_bg)
            else -> ActionVisual(label, R.drawable.ic_history, R.color.cat_emerald, R.color.cat_emerald_bg)
        }
    }

    private val LABELS = mapOf(
        AssistantAction.TOPUP to R.string.assistant_action_topup,
        AssistantAction.BILL_ELECTRICITY to R.string.assistant_action_electricity,
        AssistantAction.BILL_WATER to R.string.assistant_action_water,
        AssistantAction.MONTH_HISTORY to R.string.assistant_action_month,
        AssistantAction.TRANSFER to R.string.assistant_action_transfer,
        AssistantAction.DEPOSIT to R.string.assistant_action_deposit,
        AssistantAction.WITHDRAW to R.string.assistant_action_withdraw,
        AssistantAction.SAVING to R.string.assistant_action_saving,
        AssistantAction.FLIGHT to R.string.assistant_action_flight,
        AssistantAction.MOVIE to R.string.assistant_action_movie,
        AssistantAction.HOTEL to R.string.assistant_action_hotel,
        AssistantAction.SHOP to R.string.assistant_action_shop,
        AssistantAction.BRANCH to R.string.assistant_action_branch
    )
}
