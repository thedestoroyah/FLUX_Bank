package com.flux.bank.domain.assistant

import com.flux.bank.core.format.MoneyFormatter.formatVnd
import com.flux.bank.core.format.TextNormalizer
import com.flux.bank.domain.assistant.AssistantAction.BILL_ELECTRICITY
import com.flux.bank.domain.assistant.AssistantAction.MONTH_HISTORY
import com.flux.bank.domain.assistant.AssistantAction.TOPUP

/**
 * Chế độ cơ bản của Trợ lý: trả lời bằng từ khoá ngay trên máy khi mất mạng, hết lượt Gemini hay AI trả lỗi.
 * Câu trả lời ở đây là "lời bot nói" nên viết thẳng trong code (giống câu trả lời của Gemini), không nằm trong strings.xml.
 */
object AssistantFallback {

    // Thứ tự quan trọng: cụm khớp trước bị xoá khỏi câu, nên "nap tien dien thoai" không còn khớp "tien dien"
    private val KEYWORDS: List<Pair<AssistantAction, List<String>>> = listOf(
        TOPUP to listOf("nap tien dien thoai", "tien dien thoai", "nap dien thoai", "nap dt", "the cao", "nap card", "nap the"),
        AssistantAction.BRANCH to listOf("chi nhanh", "phong giao dich", "atm", "gan nhat"),
        BILL_ELECTRICITY to listOf("tien dien", "hoa don dien"),
        AssistantAction.BILL_WATER to listOf("tien nuoc", "hoa don nuoc"),
        MONTH_HISTORY to listOf("thang nay", "chi tieu", "tieu het", "giao dich thang", "lich su", "sao ke"),
        AssistantAction.TRANSFER to listOf("chuyen tien", "chuyen khoan", "ck"),
        AssistantAction.DEPOSIT to listOf("nap tien vao", "nap tien tai khoan", "vnpay"),
        AssistantAction.WITHDRAW to listOf("rut tien"),
        AssistantAction.SAVING to listOf("tiet kiem", "lai suat"),
        AssistantAction.FLIGHT to listOf("may bay", "ve bay"),
        AssistantAction.MOVIE to listOf("xem phim", "ve phim", "rap"),
        AssistantAction.HOTEL to listOf("khach san", "phong"),
        AssistantAction.SHOP to listOf("mua sam", "mua hang")
    )
    private val SUGGESTIONS = listOf(TOPUP, BILL_ELECTRICITY, MONTH_HISTORY)
    private val SIX_DIGITS = Regex("\\b\\d{6}\\b")
    private val NON_WORD = Regex("[^a-z0-9]+")

    private const val OTP_WARNING = "Bạn đừng gửi mã OTP hay mật khẩu cho ai, kể cả mình. FLUX không bao giờ hỏi những mã này."
    private const val UNKNOWN = "Mình chưa hiểu ý bạn. Bạn có thể thử:"
    private const val GENERIC = "Bạn có thể làm việc này ở đây."
    private const val MONTH_NO_DATA = "Bạn xem giao dịch tháng này ở đây."

    /**
     * Trả lời [question] bằng bảng từ khoá (bỏ dấu, so nguyên từ). [snapshot] null = chưa tải được số liệu.
     * Luôn trả `basicMode = true`; tối đa [AssistantReplyParser.MAX_ACTIONS] nút.
     */
    fun reply(question: String, snapshot: FinanceSnapshot?): AssistantReply {
        val text = " " + NON_WORD.replace(TextNormalizer.stripDiacritics(question).lowercase(), " ").trim() + " "
        if (SIX_DIGITS.containsMatchIn(question) || " otp " in text || " mat khau " in text) {
            return AssistantReply(OTP_WARNING, emptyList(), basicMode = true)
        }
        val actions = match(text)
        val answer = when {
            actions.isEmpty() -> return AssistantReply(UNKNOWN, SUGGESTIONS, basicMode = true)
            MONTH_HISTORY in actions -> monthText(snapshot)
            else -> GENERIC
        }
        return AssistantReply(answer, actions, basicMode = true)
    }

    private fun match(normalized: String): List<AssistantAction> {
        var rest = normalized
        val found = mutableListOf<AssistantAction>()
        for ((action, phrases) in KEYWORDS) {
            for (phrase in phrases) {
                if (" $phrase " !in rest) continue
                rest = rest.replace(" $phrase ", "  ")
                if (action !in found) found += action
            }
        }
        return found.take(AssistantReplyParser.MAX_ACTIONS)
    }

    private fun monthText(snapshot: FinanceSnapshot?): String {
        val month = snapshot?.month ?: return MONTH_NO_DATA
        val m = month.month.monthValue
        return if (month.outgoingCount == 0 && month.incomingCount == 0) "Tháng $m bạn chưa có giao dịch nào."
        else "Tháng $m bạn đã chi ${formatVnd(month.outgoing)} qua ${month.outgoingCount} giao dịch."
    }
}
