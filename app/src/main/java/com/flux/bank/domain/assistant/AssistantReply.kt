package com.flux.bank.domain.assistant

import org.json.JSONException
import org.json.JSONObject

/**
 * Một câu trả lời của Trợ lý: [text] hiện trong bong bóng, [actions] là các nút (tối đa 3).
 * [basicMode] = true khi câu trả lời do bộ từ khoá trên máy viết (mất mạng / lỗi AI), không phải Gemini.
 */
data class AssistantReply(
    val text: String,
    val actions: List<AssistantAction>,
    val basicMode: Boolean = false
)

/** Đọc JSON `{reply, actions}` mà Gemini trả theo responseSchema. */
object AssistantReplyParser {
    const val MAX_ACTIONS = 3

    private val FENCE = Regex("^```(?:json)?\\s*|\\s*```$")

    /**
     * Parse [raw]. Action lạ hoặc trùng bị bỏ, nhận cả chữ thường / có khoảng trắng; giữ tối đa [MAX_ACTIONS].
     *
     * @throws IllegalArgumentException khi [raw] không phải JSON hoặc `reply` rỗng (để chuyển sang chế độ cơ bản).
     */
    fun parse(raw: String): AssistantReply {
        val json = try {
            JSONObject(FENCE.replace(raw.trim(), ""))
        } catch (e: JSONException) {
            throw IllegalArgumentException("Câu trả lời không phải JSON", e)
        }
        val text = json.optString("reply").trim()
        require(text.isNotEmpty()) { "Câu trả lời rỗng" }
        val names = json.optJSONArray("actions")
        val actions = (0 until (names?.length() ?: 0))
            .mapNotNull { i -> AssistantAction.entries.firstOrNull { it.name == names!!.optString(i).trim().uppercase() } }
            .distinct()
            .take(MAX_ACTIONS)
        return AssistantReply(text, actions)
    }
}
