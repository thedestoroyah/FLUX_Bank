package com.flux.bank.domain.assistant

import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import com.flux.bank.domain.assistant.AssistantAction.BILL_ELECTRICITY
import com.flux.bank.domain.assistant.AssistantAction.BRANCH
import com.flux.bank.domain.assistant.AssistantAction.MONTH_HISTORY
import com.flux.bank.domain.assistant.AssistantAction.TOPUP
import com.flux.bank.domain.assistant.AssistantAction.TRANSFER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class AssistantFallbackTest {

    private val vn = ZoneId.of("Asia/Ho_Chi_Minh")
    private val now = LocalDateTime.of(2026, 11, 12, 10, 0).atZone(vn).toInstant().toEpochMilli()
    private val checking = BankAccount(accountId = "chk", type = AccountType.CHECKING, balance = 2_217_800)
    private fun pay(amount: Long) = Transaction(accountId = "chk", type = TransactionType.PAYMENT, amount = amount, service = "TIEN_DIEN", createdAt = now - 1_000)
    private fun snapshot(history: List<Transaction>) = FinanceSnapshot.build("Trần Thị Bình", listOf(checking), history, null, now)

    @Test
    fun topupWithAndWithoutDiacritics() {
        assertEquals(listOf(TOPUP), AssistantFallback.reply("Nạp tiền điện thoại", null).actions)
        assertEquals(listOf(TOPUP), AssistantFallback.reply("nap dt cho me", null).actions)
    }

    @Test
    fun electricityBill() {
        val actions = AssistantFallback.reply("hoá đơn điện tháng này", null).actions
        assertTrue(actions.toString(), BILL_ELECTRICITY in actions && MONTH_HISTORY in actions)
    }

    @Test
    fun matchesWholeWordsOnly() {
        assertEquals(listOf(TRANSFER), AssistantFallback.reply("ck cho mẹ 500k", null).actions)
        assertTrue(TRANSFER !in AssistantFallback.reply("check lại giúp mình", null).actions)
    }

    @Test
    fun branchSuggestion() {
        assertEquals(listOf(BRANCH), AssistantFallback.reply("Chi nhánh gần nhất", null).actions)
    }

    @Test
    fun monthSpendUsesSnapshot() {
        val reply = AssistantFallback.reply("Tháng này tôi chi bao nhiêu?", snapshot(listOf(pay(2_434_700), pay(357_500), pay(1_490_000))))
        assertEquals("Tháng 11 bạn đã chi 4.282.200 ₫ qua 3 giao dịch.", reply.text)
        assertEquals(listOf(MONTH_HISTORY), reply.actions)
    }

    @Test
    fun monthSpendEmptyMonth() {
        assertEquals("Tháng 11 bạn chưa có giao dịch nào.", AssistantFallback.reply("chi tiêu tháng này", snapshot(emptyList())).text)
    }

    @Test
    fun monthSpendWithoutSnapshot() {
        assertEquals("Bạn xem giao dịch tháng này ở đây.", AssistantFallback.reply("lịch sử", null).text)
    }

    @Test
    fun otpWarningHasNoActions() {
        val reply = AssistantFallback.reply("mã OTP của tôi là 482915", null)
        assertTrue(reply.text, reply.text.startsWith("Bạn đừng gửi mã OTP hay mật khẩu"))
        assertTrue(reply.actions.isEmpty())
    }

    @Test
    fun unknownSuggestsThree() {
        val reply = AssistantFallback.reply("thời tiết hôm nay", null)
        assertEquals("Mình chưa hiểu ý bạn. Bạn có thể thử:", reply.text)
        assertEquals(listOf(TOPUP, BILL_ELECTRICITY, MONTH_HISTORY), reply.actions)
    }

    @Test
    fun alwaysBasicMode() {
        assertTrue(AssistantFallback.reply("nạp dt", null).basicMode)
        assertTrue(AssistantFallback.reply("xyz", null).basicMode)
        assertTrue(AssistantFallback.reply("otp 123456", null).basicMode)
    }

    @Test
    fun amountsAreNotMistakenForOtp() {
        assertEquals(listOf(TOPUP), AssistantFallback.reply("nạp dt 100000 cho mẹ", null).actions)
        assertEquals(listOf(TRANSFER), AssistantFallback.reply("ck 500000", null).actions)
    }

    @Test
    fun secretWarningOnlyForSecrets() {
        assertTrue(AssistantFallback.secretWarning("mã OTP của tôi là 482915") != null)
        assertTrue(AssistantFallback.secretWarning("482915") != null)
        assertTrue(AssistantFallback.secretWarning("mật khẩu là abc") != null)
        assertTrue(AssistantFallback.secretWarning("nạp dt 100000") == null)
    }
}
