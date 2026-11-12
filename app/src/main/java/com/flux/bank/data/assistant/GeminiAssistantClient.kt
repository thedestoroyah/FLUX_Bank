package com.flux.bank.data.assistant

import com.flux.bank.domain.assistant.AssistantAction
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig

/**
 * Gemini qua Firebase AI Logic, backend Gemini Developer API (free tier, chạy trên gói Spark, không có API key trong APK).
 * Trả lời theo responseSchema `{reply, actions}` để app tự kiểm tra action.
 */
class GeminiAssistantClient : AssistantClient {

    override suspend fun ask(systemPrompt: String, turns: List<ChatTurn>): String {
        // System instruction cố định lúc tạo model, mà ngữ cảnh đổi mỗi lượt, nên tạo model mới mỗi lần (rẻ, không gọi mạng)
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = SCHEMA
                // Tắt "thinking": để mặc định thì một lượt mất hơn 60 s (đo 2026-11-13), tắt thì ~6 s, đủ cho câu trả lời ngắn
                thinkingConfig = thinkingConfig { thinkingBudget = 0 }
            },
            systemInstruction = content { text(systemPrompt) }
        )
        val history = turns.map { turn -> content(role = if (turn.fromUser) "user" else "model") { text(turn.text) } }
        return model.generateContent(history).text
            ?: throw IllegalStateException("Gemini không trả nội dung")
    }

    companion object {
        /**
         * Model dùng cho Trợ lý. Ngày 2026-11-13 `gemini-3.8-flash` (gợi ý của trang get-started) liên tục báo
         * "high demand" (HTTP 500) ở free tier, còn bản này trả lời trong ~4 s, nên chọn bản này cho ổn định khi demo.
         */
        const val MODEL = "gemini-3.5-flash"

        private val SCHEMA = Schema.obj(
            mapOf(
                "reply" to Schema.string(),
                "actions" to Schema.array(Schema.enumeration(AssistantAction.entries.map { it.name }))
            )
        )
    }
}
