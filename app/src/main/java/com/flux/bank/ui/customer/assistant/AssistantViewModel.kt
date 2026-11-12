package com.flux.bank.ui.customer.assistant

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.data.assistant.AssistantClient
import com.flux.bank.data.assistant.ChatTurn
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.RatesRepository
import com.flux.bank.data.repository.TransactionRepository
import com.flux.bank.data.repository.UserRepository
import com.flux.bank.domain.TransferPolicy
import com.flux.bank.domain.assistant.AssistantFallback
import com.flux.bank.domain.assistant.AssistantPrompt
import com.flux.bank.domain.assistant.AssistantReply
import com.flux.bank.domain.assistant.AssistantReplyParser
import com.flux.bank.domain.assistant.FinanceSnapshot
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.YearMonth

/** Một dòng của màn chat. [sensitive] = tin có mã OTP / mật khẩu và lời nhắc của nó: hiện trên màn nhưng không gửi lên AI. */
sealed class ChatItem {
    data class User(val text: String, val sensitive: Boolean = false) : ChatItem()
    data class Bot(val reply: AssistantReply, val sensitive: Boolean = false) : ChatItem()
    object Typing : ChatItem()
}

/**
 * Màn Trợ lý FLUX (spec F7 §5–6): gửi câu hỏi + ngữ cảnh tài chính cho Gemini, lỗi / quá [timeoutMs]
 * thì trả lời bằng [AssistantFallback]. Cuộc trò chuyện chỉ sống trong ViewModel (không lưu Firestore).
 *
 * @param onError nhận lỗi của AI trước khi chuyển sang chế độ cơ bản (mặc định ghi Logcat để chẩn đoán trên máy).
 */
class AssistantViewModel(
    private val client: AssistantClient = FluxApp.instance.assistantClient,
    private val auth: AuthRepository = FluxApp.instance.authRepository,
    private val users: UserRepository = FluxApp.instance.userRepository,
    private val accounts: AccountRepository = FluxApp.instance.accountRepository,
    private val transactions: TransactionRepository = FluxApp.instance.transactionRepository,
    private val rates: RatesRepository = FluxApp.instance.ratesRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val timeoutMs: Long = TIMEOUT_MS,
    private val onError: (Throwable) -> Unit = { Log.w(TAG, "AI lỗi, dùng chế độ cơ bản", it) }
) : ViewModel() {

    private val _items = MutableLiveData<List<ChatItem>>(emptyList())
    val items: LiveData<List<ChatItem>> = _items

    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy

    private val _firstName = MutableLiveData<String>()
    val firstName: LiveData<String> = _firstName

    private var fullName = ""
    private var savingId: String? = null

    /** Tháng hiện tại theo giờ Việt Nam, cho nhãn nút "Xem giao dịch tháng N". */
    val currentMonth: YearMonth get() = YearMonth.from(Instant.ofEpochMilli(now()).atZone(TransferPolicy.VN_ZONE))

    init {
        viewModelScope.launch {
            fullName = auth.currentUid()?.let { users.getUser(it).getOrNull()?.fullName }.orEmpty()
            _firstName.value = FinanceSnapshot.firstName(fullName)
        }
    }

    /** Tài khoản tiết kiệm đầu tiên (biết sau lượt hỏi đầu); null = chưa có hoặc chưa tải. */
    fun savingAccountId(): String? = savingId

    /**
     * Gửi câu hỏi; bỏ qua khi rỗng hoặc đang chờ câu trả lời trước. Câu dài hơn [MAX_INPUT] bị cắt.
     * Câu có vẻ chứa OTP / mật khẩu được trả lời ngay trên máy, không gửi lên AI.
     */
    fun send(text: String) {
        val question = text.trim().take(MAX_INPUT)
        if (question.isEmpty() || _busy.value == true) return
        AssistantFallback.secretWarning(question)?.let { warning ->
            _items.value = _items.value.orEmpty() + ChatItem.User(question, sensitive = true) + ChatItem.Bot(warning, sensitive = true)
            return
        }
        val history = _items.value.orEmpty() + ChatItem.User(question)
        _items.value = history + ChatItem.Typing
        _busy.value = true
        viewModelScope.launch {
            _items.value = history + ChatItem.Bot(answer(question, history))
            _busy.value = false
        }
    }

    // Một hạn [timeoutMs] cho cả tải số liệu lẫn gọi AI; tải số liệu chậm quá [SNAPSHOT_TIMEOUT_MS] thì hỏi AI không kèm số liệu
    private suspend fun answer(question: String, history: List<ChatItem>): AssistantReply {
        var snapshot: FinanceSnapshot? = null
        return try {
            withTimeout(timeoutMs) {
                snapshot = withTimeoutOrNull(SNAPSHOT_TIMEOUT_MS) { loadSnapshot() }
                AssistantReplyParser.parse(client.ask(AssistantPrompt.system(snapshot), turnsOf(history)))
            }
        } catch (e: Exception) {
            // Gồm cả hết giờ và CancellationException từ thư viện (Task bị huỷ); chỉ ném tiếp khi chính màn này bị huỷ
            currentCoroutineContext().ensureActive()
            onError(e)
            AssistantFallback.reply(question, snapshot)
        }
    }

    private suspend fun loadSnapshot(): FinanceSnapshot? {
        val uid = auth.currentUid() ?: return null
        val owned = accounts.listByOwner(uid).getOrNull() ?: return null
        val history = transactions.listRecent(uid).getOrNull() ?: return null
        savingId = owned.firstOrNull { it.type == AccountType.SAVING }?.accountId
        return FinanceSnapshot.build(fullName, owned, history, rates.get().getOrNull(), now())
    }

    // Gemini cần lịch sử bắt đầu bằng lượt của khách, nên bỏ các tin bot ở đầu sau khi cắt
    private fun turnsOf(history: List<ChatItem>): List<ChatTurn> = history
        .filterNot { (it as? ChatItem.User)?.sensitive == true || (it as? ChatItem.Bot)?.sensitive == true }
        .mapNotNull {
            when (it) {
                is ChatItem.User -> ChatTurn(true, it.text)
                is ChatItem.Bot -> ChatTurn(false, it.reply.text)
                ChatItem.Typing -> null
            }
        }
        .takeLast(HISTORY_TURNS)
        .dropWhile { !it.fromUser }

    companion object {
        const val TIMEOUT_MS = 20_000L
        const val SNAPSHOT_TIMEOUT_MS = 5_000L
        const val MAX_INPUT = 500
        const val HISTORY_TURNS = 10
        private const val TAG = "AssistantViewModel"
    }
}
