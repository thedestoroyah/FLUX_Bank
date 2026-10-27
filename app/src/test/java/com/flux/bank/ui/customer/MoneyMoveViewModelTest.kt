package com.flux.bank.ui.customer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.core.validation.AmountError
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.repository.InsufficientFundsException
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.payment.VnPay
import com.flux.bank.payment.VnPayConfig
import com.flux.bank.testutil.MainDispatcherRule
import com.flux.bank.ui.customer.account.MoneyMoveViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MoneyMoveViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val accounts = FakeAccountRepository().apply {
        this.accounts += BankAccount(accountId = "chk", ownerUid = "c1", type = AccountType.CHECKING, balance = 499_999)
        this.accounts += BankAccount(accountId = "sav", ownerUid = "c1", type = AccountType.SAVING, balance = 120_000_000)
        this.accounts += BankAccount(accountId = "mor", ownerUid = "c1", type = AccountType.MORTGAGE, balance = 1_000_000)
    }

    private val vnPay = VnPay(VnPayConfig("TESTCODE", "SECRET"))

    private fun vm(deposit: Boolean, kyc: String = KycStatus.VERIFIED, preselect: String? = null, payments: VnPay? = vnPay) = MoneyMoveViewModel(
        deposit, accounts, FakeUserRepository(listOf(customer("c1", kyc = kyc))), FakeAuthRepository().apply { uid = "c1" }, payments
    ).apply { load(preselect) }

    /** Query VNPay trả về cho đơn [txnRef], ký bằng khoá test; [extra] ghi đè sau khi ký (giả lập bị sửa). */
    private fun signedReturn(txnRef: String, amount: Long, code: String = "00", status: String = "00", extra: Map<String, String> = emptyMap()): Map<String, String> {
        val params = mapOf(
            "vnp_TxnRef" to txnRef, "vnp_Amount" to (amount * 100).toString(), "vnp_ResponseCode" to code,
            "vnp_TransactionStatus" to status, "vnp_TransactionNo" to "14000001"
        )
        return params + ("vnp_SecureHash" to VnPay.sign(params, "SECRET")) + extra
    }

    /** Nạp [amount] vào chk, xác thực xong và đã mở VNPay. */
    private fun depositUntilPayment(amount: String = "1.000.000") = vm(deposit = true).apply {
        setAmount(amount)
        confirm()
        onAuthResult(true)
    }

    @Test
    fun depositOpensVnPayAfterAuth() {
        val vm = depositUntilPayment()
        assertTrue(vm.openPayment.value!!.peek().startsWith(VnPay.SANDBOX_URL))
        assertTrue(accounts.deposits.isEmpty())
    }

    @Test
    fun successfulPaymentDepositsWithRef() {
        val vm = depositUntilPayment()
        vm.onPaymentResult(signedReturn("vnp0", 1_000_000))
        assertEquals(listOf("chk" to 1_000_000L), accounts.deposits)
        val tx = vm.done.value!!.peek()
        assertEquals("vnp0", tx.id)
        assertEquals("14000001", tx.paymentRef)
    }

    @Test
    fun paymentResultHandledOnce() {
        val vm = depositUntilPayment()
        val query = signedReturn("vnp0", 1_000_000)
        vm.onPaymentResult(query)
        vm.onPaymentResult(query)
        assertEquals(1, accounts.deposits.size)
    }

    @Test
    fun badSignatureDoesNotDeposit() {
        val vm = depositUntilPayment()
        vm.onPaymentResult(signedReturn("vnp0", 1_000_000, extra = mapOf("vnp_Amount" to "900000000")))
        assertTrue(accounts.deposits.isEmpty())
        assertEquals(R.string.err_vnpay_bad_result, vm.state.value!!.errorRes)
    }

    @Test
    fun cancelledShowsMessage() {
        val vm = depositUntilPayment()
        vm.onPaymentResult(signedReturn("vnp0", 1_000_000, code = "24"))
        assertTrue(accounts.deposits.isEmpty())
        assertEquals(R.string.err_vnpay_cancelled, vm.state.value!!.errorRes)
    }

    @Test
    fun closedWebViewClearsPending() {
        val vm = depositUntilPayment()
        vm.onPaymentClosed()
        vm.onPaymentResult(signedReturn("vnp0", 1_000_000))
        assertTrue(accounts.deposits.isEmpty())
        assertEquals(R.string.err_vnpay_cancelled, vm.state.value!!.errorRes)
    }

    @Test
    fun notConfiguredShowsError() {
        val vm = vm(deposit = true, payments = null)
        vm.setAmount("1.000.000")
        vm.confirm()
        vm.onAuthResult(true)
        assertEquals(R.string.err_vnpay_not_configured, vm.state.value!!.errorRes)
        assertNull(vm.openPayment.value)
    }

    @Test
    fun listsOnlyCheckingAndSavingAndDefaultsToChecking() {
        val s = vm(deposit = true).state.value!!
        assertEquals(listOf("chk", "sav"), s.accounts.map { it.accountId })
        assertEquals("chk", s.selectedId)
        assertEquals("sav", vm(deposit = true, preselect = "sav").state.value!!.selectedId)
    }

    @Test
    fun notVerifiedIsBlocked() {
        val vm = vm(deposit = true, kyc = KycStatus.PENDING)
        assertTrue(vm.state.value!!.kycBlocked)
        vm.setAmount("2.000.000")
        vm.confirm()
        assertNull(vm.askAuth.value)
    }

    @Test
    fun withdrawOverBalanceShowsError() {
        val vm = vm(deposit = false)
        vm.setAmount("500000")
        vm.confirm()
        assertEquals(AmountError.INSUFFICIENT_FUNDS, vm.state.value!!.amountError)
        assertNull(vm.askAuth.value)
    }

    @Test
    fun balanceAfterPreview() {
        val vm = vm(deposit = true, preselect = "sav")
        vm.setAmount("2.000.000")
        assertEquals(122_000_000L, vm.state.value!!.balanceAfter)
    }

    @Test
    fun verifiedAuthWithdrawsOnceAndEmitsReceipt() {
        val vm = vm(deposit = false, preselect = "sav")
        vm.setAmount("2.000.000")
        vm.confirm()
        assertEquals(2_000_000L, vm.askAuth.value!!.peek())
        vm.onAuthResult(true)
        assertEquals(listOf("sav" to 2_000_000L), accounts.withdrawals)
        assertEquals(118_000_000L, vm.done.value!!.peek().balanceAfter)
        assertFalse(vm.state.value!!.submitting)
    }

    @Test
    fun secondConfirmWhileSubmittingIsIgnored() {
        val vm = vm(deposit = false)
        vm.setAmount("100000")
        accounts.moveGate = CompletableDeferred()
        vm.confirm()
        vm.confirm()
        vm.onAuthResult(true)
        vm.confirm()
        vm.onAuthResult(true)
        accounts.moveGate!!.complete(Unit)
        assertEquals(1, accounts.withdrawals.size)
    }

    @Test
    fun cancelledAuthResetsWithoutWriting() {
        val vm = vm(deposit = true)
        vm.setAmount("100000")
        vm.confirm()
        vm.onAuthResult(false)
        assertTrue(accounts.deposits.isEmpty())
        assertFalse(vm.state.value!!.submitting)
    }

    @Test
    fun insufficientFundsFromRepositoryShowsMessage() {
        val vm = vm(deposit = false, preselect = "sav")
        vm.setAmount("1.000.000")
        accounts.failNextMove = InsufficientFundsException()
        vm.confirm()
        vm.onAuthResult(true)
        assertEquals(R.string.err_insufficient_funds, vm.state.value!!.errorRes)
        assertNull(vm.done.value)
    }

    @Test
    fun profileLoadFailureIsNotTreatedAsMissingKyc() {
        val vm = MoneyMoveViewModel(true, accounts, FakeUserRepository(), FakeAuthRepository().apply { uid = "c1" }, vnPay).apply { load(null) }
        assertFalse(vm.state.value!!.kycBlocked)
        assertEquals(R.string.err_generic, vm.state.value!!.errorRes)
        vm.setAmount("100000")
        vm.confirm()
        assertNull(vm.askAuth.value)
    }

    @Test
    fun noNegativeBalancePreviewWhenWithdrawingTooMuch() {
        val vm = vm(deposit = false)
        vm.setAmount("500000")
        assertNull(vm.state.value!!.balanceAfter)
    }
}
