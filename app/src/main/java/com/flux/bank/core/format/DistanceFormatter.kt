package com.flux.bank.core.format

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/** Định dạng quãng đường và thời gian đi bộ cho màn chi nhánh. */
object DistanceFormatter {

    private val VI = Locale.forLanguageTag("vi-VN")

    /** Dưới 1 km: "850 m"; từ 1 km: "2,1 km" (dấu phẩy thập phân kiểu Việt Nam). */
    fun distance(meters: Double): String =
        if (meters < 1_000) "${meters.roundToInt()} m" else String.format(VI, "%.1f km", meters / 1_000)

    /** Số phút đi bộ, làm tròn lên và tối thiểu 1 (đứng sát chi nhánh không hiện "0 phút"). */
    fun minutes(seconds: Double): Int = max(1, ceil(seconds / 60).toInt())
}
