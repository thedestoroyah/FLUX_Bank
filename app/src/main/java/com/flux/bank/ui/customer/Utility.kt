package com.flux.bank.ui.customer

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.flux.bank.R

/** 12 ô tiện ích trên trang chủ (lưới 4x3), đúng thứ tự trong mockup đã duyệt. */
enum class Utility(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @ColorRes val tint: Int,
    @ColorRes val tileBg: Int
) {
    NAP_TIEN(R.string.util_deposit, R.drawable.ic_plus, R.color.cat_emerald, R.color.cat_emerald_bg),
    RUT_TIEN(R.string.util_withdraw, R.drawable.ic_arrow_down, R.color.cat_emerald, R.color.cat_emerald_bg),
    TIEN_DIEN(R.string.util_electricity, R.drawable.ic_lightning, R.color.cat_gold, R.color.cat_gold_bg),
    TIEN_NUOC(R.string.util_water, R.drawable.ic_drop, R.color.cat_blue, R.color.cat_blue_bg),
    NAP_DT(R.string.util_topup, R.drawable.ic_phone, R.color.cat_blue, R.color.cat_blue_bg),
    VE_MAY_BAY(R.string.util_flight, R.drawable.ic_airplane, R.color.cat_sky, R.color.cat_sky_bg),
    VE_PHIM(R.string.util_movie, R.drawable.ic_film, R.color.cat_rose, R.color.cat_rose_bg),
    KHACH_SAN(R.string.util_hotel, R.drawable.ic_bed, R.color.cat_violet, R.color.cat_violet_bg),
    MUA_SAM(R.string.util_shopping, R.drawable.ic_bag, R.color.cat_rose, R.color.cat_rose_bg),
    TIET_KIEM(R.string.util_saving, R.drawable.ic_piggy, R.color.cat_emerald, R.color.cat_emerald_bg),
    CHI_NHANH(R.string.util_branch, R.drawable.ic_map_pin, R.color.cat_blue, R.color.cat_blue_bg),
    TRO_LY_AI(R.string.util_assistant, R.drawable.ic_sparkle, R.color.cat_violet, R.color.cat_violet_bg)
}
