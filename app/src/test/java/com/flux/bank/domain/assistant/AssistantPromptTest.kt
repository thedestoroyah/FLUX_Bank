package com.flux.bank.domain.assistant

import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPromptTest {

    private val snapshot = FinanceSnapshot.build(
        "Trần Thị Bình", listOf(BankAccount(type = AccountType.CHECKING, balance = 2_217_800)), emptyList(), null, 1_794_000_000_000
    )

    @Test
    fun mentionsEveryAction() {
        val prompt = AssistantPrompt.system(snapshot)
        AssistantAction.entries.forEach { assertTrue(it.name, prompt.contains(it.name)) }
    }

    @Test
    fun usesPolicyNumbers() {
        val prompt = AssistantPrompt.system(snapshot)
        assertTrue(prompt.contains("10.000 ₫"))
        assertTrue(prompt.contains("100.000.000 ₫"))
        assertTrue(prompt.contains("200.000.000 ₫"))
        assertTrue(prompt.contains("10.000.000 ₫"))
        assertTrue(prompt.contains("120 giây"))
    }

    @Test
    fun forbidsSecrets() {
        val prompt = AssistantPrompt.system(snapshot)
        assertTrue(prompt.contains("OTP"))
        assertTrue(prompt.contains("mật khẩu"))
    }

    @Test
    fun appendsSnapshot() {
        assertTrue(AssistantPrompt.system(snapshot).contains(snapshot.toPromptText()))
    }

    @Test
    fun nullSnapshotSaysUnavailable() {
        assertTrue(AssistantPrompt.system(null).contains("chưa tải được số liệu"))
    }
}
