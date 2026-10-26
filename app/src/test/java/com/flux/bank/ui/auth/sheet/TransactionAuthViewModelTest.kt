package com.flux.bank.ui.auth.sheet

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.core.security.OtpManager
import com.flux.bank.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TransactionAuthViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private var now = 0L
    private val delivered = mutableListOf<String>()
    private val vm = TransactionAuthViewModel(OtpManager { now }, deliver = { delivered += it })

    private fun type(code: String) = code.forEach { vm.onDigit(it) }
    private fun wrong(code: String) = if (code == "000000") "111111" else "000000"

    @Test
    fun startDeliversExactlyOneSixDigitCode() {
        vm.start(amount = null)
        assertEquals(1, delivered.size)
        assertTrue(delivered[0].matches(Regex("\\d{6}")))
        assertEquals(120L, (vm.state.value as AuthSheetState.EnteringOtp).remainingSec)
    }

    @Test
    fun correctCodeVerifiesWithoutAmount() {
        vm.start(amount = null)
        type(delivered[0]); vm.confirm()
        assertEquals(AuthSheetState.Verified, vm.state.value)
    }

    @Test
    fun justBelowThresholdSkipsFace() {
        vm.start(amount = 9_999_999)
        type(delivered[0]); vm.confirm()
        assertEquals(AuthSheetState.Verified, vm.state.value)
    }

    @Test
    fun thresholdAmountGoesToFaceStep() {
        vm.start(amount = 10_000_000)
        type(delivered[0]); vm.confirm()
        assertEquals(AuthSheetState.FaceStep, vm.state.value)
    }

    @Test
    fun wrongCodeShowsAttemptsLeftAndClearsCells() {
        vm.start(amount = null)
        type(wrong(delivered[0])); vm.confirm()
        val s = vm.state.value as AuthSheetState.EnteringOtp
        assertEquals(R.string.otp_wrong, s.errorRes)
        assertEquals(4, s.attemptsLeft)
        assertEquals("", s.digits)
    }

    @Test
    fun fiveWrongCodesLock() {
        vm.start(amount = null)
        repeat(5) { type(wrong(delivered[0])); vm.confirm() }
        type(delivered[0]); vm.confirm()
        assertEquals(AuthSheetState.Locked, vm.state.value)
    }

    @Test
    fun expiredCodeShowsExpiredError() {
        vm.start(amount = null)
        now = 121_000
        type(delivered[0]); vm.confirm()
        assertEquals(R.string.otp_expired, (vm.state.value as AuthSheetState.EnteringOtp).errorRes)
    }

    @Test
    fun resendIssuesNewCodeAndResetsCountdown() {
        vm.start(amount = null)
        now = 60_000
        vm.tick()
        assertEquals(60L, (vm.state.value as AuthSheetState.EnteringOtp).remainingSec)
        vm.resend()
        assertEquals(2, delivered.size)
        assertEquals(120L, (vm.state.value as AuthSheetState.EnteringOtp).remainingSec)
    }

    @Test
    fun digitsCapAtSixAndBackspaceRemoves() {
        vm.start(amount = null)
        type("1234567")
        assertEquals("123456", (vm.state.value as AuthSheetState.EnteringOtp).digits)
        vm.onBackspace()
        assertEquals("12345", (vm.state.value as AuthSheetState.EnteringOtp).digits)
    }

    @Test
    fun autofillReplacesDigits() {
        vm.start(amount = null)
        vm.fill(delivered[0])
        assertEquals(delivered[0], (vm.state.value as AuthSheetState.EnteringOtp).digits)
        assertNotEquals(AuthSheetState.Verified, vm.state.value)
    }
}
