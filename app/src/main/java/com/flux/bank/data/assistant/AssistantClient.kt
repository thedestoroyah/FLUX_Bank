package com.flux.bank.data.assistant

/** Một tin trong lịch sử gửi cho mô hình: [fromUser] = khách nói, ngược lại là bot. */
data class ChatTurn(val fromUser: Boolean, val text: String)

/** Gọi mô hình AI của Trợ lý; tách interface để ViewModel test được bằng client giả. */
interface AssistantClient {
    /**
     * Gửi [turns] (tin cuối là câu khách vừa hỏi) kèm [systemPrompt]; trả JSON thô `{reply, actions}`.
     * Ném lỗi khi mạng / quota / mô hình lỗi.
     */
    suspend fun ask(systemPrompt: String, turns: List<ChatTurn>): String
}
