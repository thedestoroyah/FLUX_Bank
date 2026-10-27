package com.flux.bank.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class VnPayTest {

    private fun at(iso: String) = ZonedDateTime.parse(iso).toInstant().toEpochMilli()
    private val vnp = VnPay(VnPayConfig("TESTCODE", "SECRET"), clock = { at("2026-10-27T09:05:00+07:00") })

    /** URL trả về hợp lệ cho đơn "abc" 1.000.000 ₫, ký bằng "SECRET"; [extra] ghi đè trước khi ký. */
    private fun returned(vararg extra: Pair<String, String>): Map<String, String> {
        val params = mapOf(
            "vnp_TxnRef" to "abc", "vnp_Amount" to "100000000", "vnp_ResponseCode" to "00",
            "vnp_TransactionStatus" to "00", "vnp_TransactionNo" to "14000001", "vnp_OrderInfo" to "Nap tien FLUX abc"
        ) + extra
        return params + ("vnp_SecureHash" to VnPay.sign(params, "SECRET"))
    }

    @Test
    fun signMatchesKnownVector() = assertEquals(
        "668e6ae63a0afeb54120e36e6e10ae55f1770a10b9f50be5bb016536b29c21cc22058bcaf654cf2984efb9f5eb26c7821a20263222edf479db2b0affa0ab1c1d",
        VnPay.sign(mapOf("vnp_Amount" to "100000000", "vnp_OrderInfo" to "Nap tien FLUX abc", "vnp_TxnRef" to "abc"), "SECRET")
    )

    @Test
    fun paymentUrlHasRequiredParams() {
        val url = vnp.paymentUrl("abc", 1_000_000, "Nap tien FLUX abc")
        assertTrue(url.startsWith(VnPay.SANDBOX_URL + "?"))
        val query = VnPay.parseQuery(url)
        assertEquals("100000000", query["vnp_Amount"])
        assertEquals("20261027090500", query["vnp_CreateDate"])
        assertEquals("20261027092000", query["vnp_ExpireDate"])
        assertEquals("TESTCODE", query["vnp_TmnCode"])
        assertEquals(VnPay.RETURN_URL, query["vnp_ReturnUrl"])
        assertEquals(VnPay.sign(query - "vnp_SecureHash", "SECRET"), query["vnp_SecureHash"])
    }

    @Test
    fun successfulReturn() = assertEquals(VnPayResult.Success("14000001"), vnp.parseReturn(returned(), "abc", 1_000_000))

    @Test
    fun tamperedAmountIsBadSignature() =
        assertEquals(VnPayResult.BadSignature, vnp.parseReturn(returned() + ("vnp_Amount" to "900000000"), "abc", 1_000_000))

    @Test
    fun otherTxnRefIsMismatch() = assertEquals(VnPayResult.Mismatch, vnp.parseReturn(returned(), "xyz", 1_000_000))

    @Test
    fun cancelled() = assertEquals(VnPayResult.Cancelled, vnp.parseReturn(returned("vnp_ResponseCode" to "24"), "abc", 1_000_000))

    @Test
    fun failedStatus() {
        assertEquals(VnPayResult.Failed("00"), vnp.parseReturn(returned("vnp_TransactionStatus" to "02"), "abc", 1_000_000))
        assertEquals(
            VnPayResult.Failed("51"),
            vnp.parseReturn(returned("vnp_ResponseCode" to "51", "vnp_TransactionStatus" to "02"), "abc", 1_000_000)
        )
    }

    @Test
    fun parseQueryDecodesPlusAndPercent() = assertEquals(
        mapOf("vnp_OrderInfo" to "Nap tien: A", "vnp_TxnRef" to "abc"),
        VnPay.parseQuery("https://fluxbank.example/vnpay-return?vnp_OrderInfo=Nap+tien%3A+A&vnp_TxnRef=abc")
    )

    @Test
    fun isReturnUrl() {
        assertTrue(vnp.isReturnUrl(VnPay.RETURN_URL + "?a=1"))
        assertFalse(vnp.isReturnUrl(VnPay.SANDBOX_URL))
    }
}
