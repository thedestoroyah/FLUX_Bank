package com.flux.bank.domain

/** Nội dung mã QR nhận tiền của FLUX: `fluxbank:<số tài khoản>` (định dạng riêng, không theo VietQR). */
object QrPayload {
    const val PREFIX = "fluxbank:"

    fun encode(accountNumber: String): String = PREFIX + accountNumber

    /** Số tài khoản FLUX trong mã, hoặc null nếu không phải mã của FLUX. */
    fun decode(text: String): String? =
        text.trim().removePrefix(PREFIX).takeIf { text.trim().startsWith(PREFIX) && TransferPolicy.isFluxAccountNumber(it) }
}
