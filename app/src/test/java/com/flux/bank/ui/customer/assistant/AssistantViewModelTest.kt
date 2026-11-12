package com.flux.bank.ui.customer.assistant

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.data.assistant.ChatTurn
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.User
import com.flux.bank.domain.assistant.AssistantAction.TOPUP
import com.flux.bank.domain.assistant.AssistantReply
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeAssistantClient
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeRatesRepository
import com.flux.bank.fakes.FakeTransactionRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.testutil.MainDispatcherRule
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.repository.TransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class AssistantViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val client = FakeAssistantClient()
    private val accounts = FakeAccountRepository().apply {
        this.accounts += BankAccount(accountId = "chk", ownerUid = "c1", type = AccountType.CHECKING, accountNumber = "886810968242", balance = 2_217_800)
    }

    private fun vm(timeoutMs: Long = AssistantViewModel.TIMEOUT_MS, transactions: TransactionRepository = FakeTransactionRepository()) = AssistantViewModel(
        client = client,
        auth = FakeAuthRepository().apply { uid = "c1" },
        users = FakeUserRepository(listOf(User(uid = "c1", fullName = "Trần Thị Bình"))),
        accounts = accounts,
        transactions = transactions,
        rates = FakeRatesRepository(),
        timeoutMs = timeoutMs,
        onError = {}
    )

    private fun AssistantViewModel.bots() = items.value!!.filterIsInstance<ChatItem.Bot>()

    @Test
    fun aiReplyAppearsWithActions() {
        client.answer = { _, _ -> """{"reply":"Nạp ở đây.","actions":["TOPUP"]}""" }
        val vm = vm()
        vm.send("nạp dt")
        assertEquals(listOf(ChatItem.User("nạp dt"), ChatItem.Bot(AssistantReply("Nạp ở đây.", listOf(TOPUP)))), vm.items.value)
        assertFalse(vm.busy.value!!)
    }

    @Test
    fun clientErrorFallsBackToBasicMode() {
        client.answer = { _, _ -> throw IOException("offline") }
        val vm = vm()
        vm.send("nạp dt")
        val reply = vm.bots().single().reply
        assertTrue(reply.basicMode)
        assertEquals(listOf(TOPUP), reply.actions)
    }

    @Test
    fun badJsonFallsBack() {
        client.answer = { _, _ -> "xin chào" }
        val vm = vm()
        vm.send("nạp dt")
        assertTrue(vm.bots().single().reply.basicMode)
    }

    @Test
    fun timeoutFallsBack() {
        client.answer = { _, _ -> awaitCancellation() }
        val vm = vm(timeoutMs = 1_000)
        vm.send("nạp dt")
        assertTrue(vm.busy.value!!)
        assertEquals(ChatItem.Typing, vm.items.value!!.last())

        main.dispatcher.scheduler.advanceTimeBy(1_001)
        main.dispatcher.scheduler.runCurrent()

        assertTrue(vm.bots().single().reply.basicMode)
        assertFalse(vm.busy.value!!)
    }

    @Test
    fun ignoresBlankAndWhileBusy() {
        val gate = CompletableDeferred<String>()
        client.answer = { _, _ -> gate.await() }
        val vm = vm()
        vm.send("  ")
        assertTrue(vm.items.value!!.isEmpty())

        vm.send("a")
        vm.send("b")

        assertEquals(1, client.calls.size)
        assertEquals(1, vm.items.value!!.count { it is ChatItem.User })
    }

    @Test
    fun sendsOnlyLastTenTurnsStartingWithUser() {
        val vm = vm()
        (1..6).forEach { vm.send("câu $it") }
        val turns = client.calls.last().second
        assertTrue(turns.size <= AssistantViewModel.HISTORY_TURNS)
        assertTrue(turns.first().fromUser)
        assertEquals(ChatTurn(true, "câu 6"), turns.last())
        assertEquals(ChatTurn(true, "câu 2"), turns.first())
    }

    @Test
    fun capsInputAt500() {
        val vm = vm()
        vm.send("a".repeat(600))
        assertEquals(500, (vm.items.value!!.first() as ChatItem.User).text.length)
    }

    @Test
    fun snapshotSentWithoutAccountNumber() {
        val vm = vm()
        vm.send("số dư của tôi")
        val prompt = client.calls.single().first
        assertFalse(prompt.contains("886810968242"))
        assertTrue(prompt.contains("Thanh toán 2.217.800 ₫"))
    }

    @Test
    fun firstNameLoaded() {
        assertEquals("Bình", vm().firstName.value)
    }

    @Test
    fun hangingDataLoadStillAsksAiWithinDeadline() {
        val hanging = object : TransactionRepository {
            override suspend fun listRecent(ownerUid: String, limit: Long): Result<List<Transaction>> = awaitCancellation()
            override suspend fun findByPaymentRef(ownerUid: String, paymentRef: String): Result<Transaction?> = Result.success(null)
        }
        val vm = vm(transactions = hanging)
        vm.send("tháng này tôi chi bao nhiêu")

        main.dispatcher.scheduler.advanceTimeBy(AssistantViewModel.SNAPSHOT_TIMEOUT_MS + 1)
        main.dispatcher.scheduler.runCurrent()

        assertFalse(vm.busy.value!!)
        assertFalse(vm.bots().single().reply.basicMode)
        assertTrue(client.calls.single().first.contains("chưa tải được số liệu"))
    }

    @Test
    fun foreignCancellationFallsBackInsteadOfHanging() {
        client.answer = { _, _ -> throw CancellationException("task cancelled") }
        val vm = vm()
        vm.send("nạp dt")
        assertTrue(vm.bots().single().reply.basicMode)
        assertFalse(vm.busy.value!!)
    }

    @Test
    fun otpIsAnsweredLocallyAndNeverSent() {
        val vm = vm()
        vm.send("mã OTP của tôi là 482915")
        assertTrue(client.calls.isEmpty())
        val reply = vm.bots().single().reply
        assertTrue(reply.text, reply.text.startsWith("Bạn đừng gửi mã OTP"))
        assertFalse(reply.basicMode)

        vm.send("nạp dt")
        assertTrue(client.calls.single().second.none { it.text.contains("482915") || it.text.contains("đừng gửi") })
    }
}
