package com.flux.bank.ui.common

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.flux.bank.R
import com.flux.bank.data.model.KycStatus

/** Hiển thị trạng thái eKYC thành pill màu (xanh: đã duyệt, vàng: chờ, đỏ: từ chối, xám: chưa nộp). */
fun TextView.showKycPill(status: String) {
    val (label, fg, bg) = when (status) {
        KycStatus.VERIFIED -> Triple(R.string.kyc_pill_verified, R.color.flux_emerald_dark, R.color.flux_emerald_light)
        KycStatus.PENDING -> Triple(R.string.kyc_pill_pending, R.color.flux_warning_dark, R.color.flux_warning_bg)
        KycStatus.REJECTED -> Triple(R.string.kyc_pill_rejected, R.color.flux_error_dark, R.color.flux_error_bg)
        else -> Triple(R.string.kyc_pill_none, R.color.flux_text_secondary, R.color.flux_border_subtle)
    }
    setText(label)
    setTextColor(ContextCompat.getColor(context, fg))
    backgroundTintList = ContextCompat.getColorStateList(context, bg)
}
