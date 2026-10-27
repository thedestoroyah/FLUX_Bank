package com.flux.bank.payment

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Khoá merchant VNPay (sandbox) và các địa chỉ dùng khi thanh toán. */
data class VnPayConfig(
    val tmnCode: String,
    val hashSecret: String,
    val payUrl: String = VnPay.SANDBOX_URL,
    val returnUrl: String = VnPay.RETURN_URL
)

/** Kết quả đọc từ URL VNPay trả về. */
sealed interface VnPayResult {
    data class Success(val transactionNo: String) : VnPayResult
    /** Khách bấm huỷ trên trang VNPay (`vnp_ResponseCode` = 24). */
    data object Cancelled : VnPayResult
    data class Failed(val code: String) : VnPayResult
    data object BadSignature : VnPayResult
    /** Mã đơn hoặc số tiền khác đơn đang chờ. */
    data object Mismatch : VnPayResult
}

/**
 * Cổng VNPay phiên bản 2.1.0: dựng URL thanh toán đã ký và kiểm URL trả về.
 * Ký và kiểm ngay trên máy vì gói Spark không có server (giới hạn ghi trong README).
 */
class VnPay(private val config: VnPayConfig, private val clock: () -> Long = System::currentTimeMillis) {

    /** URL mở trong WebView. [amount] tính bằng đồng; [orderInfo] phải không dấu, không ký tự đặc biệt. */
    fun paymentUrl(txnRef: String, amount: Long, orderInfo: String): String {
        val now = Instant.ofEpochMilli(clock())
        val params = mapOf(
            "vnp_Version" to VERSION, "vnp_Command" to "pay", "vnp_TmnCode" to config.tmnCode,
            "vnp_Amount" to (amount * 100).toString(), "vnp_CurrCode" to "VND", "vnp_TxnRef" to txnRef,
            "vnp_OrderInfo" to orderInfo, "vnp_OrderType" to "other", "vnp_Locale" to "vn",
            "vnp_ReturnUrl" to config.returnUrl, "vnp_IpAddr" to IP_ADDR,
            "vnp_CreateDate" to DATE_FORMAT.format(now), "vnp_ExpireDate" to DATE_FORMAT.format(now.plusSeconds(EXPIRE_SECONDS))
        )
        return config.payUrl + "?" + canonical(params) + "&vnp_SecureHash=" + sign(params, config.hashSecret)
    }

    fun isReturnUrl(url: String): Boolean = url.startsWith(config.returnUrl)

    /** Kiểm chữ ký trước, rồi mã đơn/số tiền, rồi mã kết quả. [expectedAmount] tính bằng đồng. */
    fun parseReturn(query: Map<String, String>, expectedTxnRef: String, expectedAmount: Long): VnPayResult {
        val received = query["vnp_SecureHash"] ?: return VnPayResult.BadSignature
        val signed = query.filterKeys { it.startsWith("vnp_") }
        if (!sign(signed, config.hashSecret).equals(received, ignoreCase = true)) return VnPayResult.BadSignature
        if (query["vnp_TxnRef"] != expectedTxnRef || query["vnp_Amount"] != (expectedAmount * 100).toString()) {
            return VnPayResult.Mismatch
        }
        val code = query["vnp_ResponseCode"].orEmpty()
        return when {
            code == CODE_CANCELLED -> VnPayResult.Cancelled
            code == CODE_OK && query["vnp_TransactionStatus"] == CODE_OK -> VnPayResult.Success(query["vnp_TransactionNo"].orEmpty())
            else -> VnPayResult.Failed(code)
        }
    }

    companion object {
        const val SANDBOX_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
        // App chặn URL này trong WebView nên không cần tồn tại thật
        const val RETURN_URL = "https://fluxbank.example/vnpay-return"
        private const val VERSION = "2.1.0"
        private const val CODE_OK = "00"
        private const val CODE_CANCELLED = "24"
        // shortcut: không có server để biết IP thật của khách, VNPay sandbox chấp nhận IP bất kỳ
        private const val IP_ADDR = "127.0.0.1"
        private const val EXPIRE_SECONDS = 15 * 60L
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
        private val UNSIGNED = setOf("vnp_SecureHash", "vnp_SecureHashType")

        /** HMAC-SHA512 (hex thường) trên chuỗi tham số đã sắp xếp và URL-encode, bỏ các trường chữ ký. */
        fun sign(params: Map<String, String>, secret: String): String {
            val mac = Mac.getInstance("HmacSHA512").apply { init(SecretKeySpec(secret.toByteArray(), "HmacSHA512")) }
            return mac.doFinal(canonical(params).toByteArray()).joinToString("") { "%02x".format(it) }
        }

        /** Tách và decode query của [url] (đổi `+` thành dấu cách), không cần `android.net.Uri`. */
        fun parseQuery(url: String): Map<String, String> =
            url.substringAfter('?', "").split('&').filter { it.isNotEmpty() }.associate { pair ->
                decode(pair.substringBefore('=')) to decode(pair.substringAfter('=', ""))
            }

        private fun canonical(params: Map<String, String>): String =
            params.filterKeys { it !in UNSIGNED }.toSortedMap().entries.joinToString("&") { (k, v) -> encode(k) + "=" + encode(v) }

        private fun encode(s: String) = URLEncoder.encode(s, StandardCharsets.US_ASCII.name())
        private fun decode(s: String) = URLDecoder.decode(s, StandardCharsets.UTF_8.name())
    }
}
