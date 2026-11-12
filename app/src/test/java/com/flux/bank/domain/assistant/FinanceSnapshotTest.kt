package com.flux.bank.domain.assistant

import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Rates
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class FinanceSnapshotTest {

    private val vn = ZoneId.of("Asia/Ho_Chi_Minh")
    private fun at(m: Int, d: Int, h: Int, min: Int) = LocalDateTime.of(2026, m, d, h, min).atZone(vn).toInstant().toEpochMilli()

    private val checking = BankAccount(accountId = "chk", ownerUid = "c1", type = AccountType.CHECKING, accountNumber = "886810968242", balance = 2_217_800)
    private fun pay(service: String, amount: Long, time: Long) =
        Transaction(accountId = "chk", accountNumber = "886810968242", type = TransactionType.PAYMENT, amount = amount, service = service, createdAt = time)
    private val november = listOf(
        pay("TIEN_DIEN", 2_434_700, at(11, 12, 8, 40)),
        pay("TIEN_NUOC", 357_500, at(11, 12, 9, 12)),
        pay("MUA_SAM", 1_490_000, at(11, 10, 20, 5)),
        Transaction(
            accountId = "chk", accountNumber = "886810968242", type = TransactionType.TRANSFER_IN, amount = 500_000,
            counterpartyName = "NGUYEN CUSTOMER", counterpartyAccount = "886800001111", createdAt = at(11, 10, 14, 31)
        )
    )

    private fun snapshot(history: List<Transaction>, now: Long = at(11, 12, 10, 0)) =
        FinanceSnapshot.build("Trần Thị Bình", listOf(checking), history, Rates(savingRate = 5.0), now)

    @Test
    fun promptHasMonthTotalsAndGroups() {
        val text = snapshot(november).toPromptText()
        assertTrue(text, text.contains("tiền ra 4.282.200 ₫ (3 giao dịch)"))
        assertTrue(text, text.contains("tiền vào 500.000 ₫ (1 giao dịch)"))
        assertTrue(text, text.contains("tiền điện 2.434.700 ₫"))
        assertTrue(text, text.contains("Thanh toán 2.217.800 ₫"))
        assertTrue(text, text.contains("Tên khách: Bình"))
    }

    @Test
    fun promptNeverLeaksIdentifiers() {
        val text = snapshot(november).toPromptText()
        assertFalse(text, text.contains("886810968242"))
        assertFalse(text, text.contains("886800001111"))
        assertFalse(text, text.contains("NGUYEN"))
    }

    @Test
    fun emptyMonthSaysNoTransactions() {
        val text = snapshot(emptyList()).toPromptText()
        assertTrue(text, text.contains("Tháng 11/2026: chưa có giao dịch nào."))
        assertFalse(text, text.contains("tiền ra"))
    }

    @Test
    fun lateNightBelongsToMonth() {
        val late = listOf(pay("TIEN_DIEN", 100_000, at(11, 30, 23, 30)))
        assertEquals(1, snapshot(late, now = at(11, 30, 23, 45)).month.outgoingCount)
    }

    @Test
    fun firstNameTakesLastWord() {
        assertEquals("Bình", FinanceSnapshot.firstName("Trần Thị Bình"))
        assertEquals("bạn", FinanceSnapshot.firstName("  "))
    }
}
