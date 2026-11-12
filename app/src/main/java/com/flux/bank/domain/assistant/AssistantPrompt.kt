package com.flux.bank.domain.assistant

import com.flux.bank.core.format.MoneyFormatter.formatVnd
import com.flux.bank.core.security.OtpManager
import com.flux.bank.core.validation.Validators
import com.flux.bank.domain.FaceVerificationPolicy
import com.flux.bank.domain.TransferPolicy

/**
 * System instruction cho Gemini (spec F7 §5). Số liệu chính sách lấy từ hằng số trong code để không lệch với app.
 * Đây là lời dặn cho bot nên viết thẳng tiếng Việt trong code, không nằm trong strings.xml.
 */
object AssistantPrompt {

    // Một dòng cho mỗi action: Gemini chọn action theo mô tả này
    private val ACTION_GUIDE = mapOf(
        AssistantAction.TOPUP to "nạp tiền điện thoại, mua thẻ cào",
        AssistantAction.BILL_ELECTRICITY to "thanh toán hoá đơn tiền điện",
        AssistantAction.BILL_WATER to "thanh toán hoá đơn tiền nước",
        AssistantAction.MONTH_HISTORY to "xem giao dịch / chi tiêu của tháng này",
        AssistantAction.TRANSFER to "chuyển tiền trong FLUX, giữa tài khoản của mình hoặc sang ngân hàng khác",
        AssistantAction.DEPOSIT to "nạp tiền vào tài khoản (qua VNPay)",
        AssistantAction.WITHDRAW to "rút tiền",
        AssistantAction.SAVING to "tài khoản tiết kiệm, lãi tiết kiệm",
        AssistantAction.FLIGHT to "mua vé máy bay",
        AssistantAction.MOVIE to "mua vé xem phim",
        AssistantAction.HOTEL to "đặt phòng khách sạn",
        AssistantAction.SHOP to "mua sắm",
        AssistantAction.BRANCH to "tìm chi nhánh FLUX gần nhất, giờ mở cửa, đường đi bộ"
    )

    /** Ghép lời dặn cố định với số liệu của khách; [snapshot] null = chưa tải được số liệu. */
    fun system(snapshot: FinanceSnapshot?): String = listOf(
        ROLE,
        rules(),
        "Các action được phép (chỉ dùng đúng tên, tối đa ${AssistantReplyParser.MAX_ACTIONS}, chọn action khớp với việc khách muốn làm):\n" +
            AssistantAction.entries.joinToString("\n") { "${it.name}: ${ACTION_GUIDE.getValue(it)}" },
        facts(),
        snapshot?.let { "Số liệu của khách:\n" + it.toPromptText() }
            ?: "Số liệu của khách: chưa tải được số liệu, hãy nói là bạn chưa xem được."
    ).joinToString("\n\n")

    private val ROLE = """
        Bạn là "Trợ lý FLUX", trợ lý trong ứng dụng ngân hàng FLUX Bank.
        Luôn trả lời bằng tiếng Việt, tối đa 3 câu ngắn, thân thiện, xưng "mình" và gọi khách là "bạn".
        Trả về JSON đúng dạng {"reply": "...", "actions": [...]}.
    """.trimIndent()

    private fun rules() = """
        Quy tắc:
        - Chỉ nói về FLUX Bank và tài chính cá nhân. Câu ngoài lề thì từ chối nhẹ nhàng và gợi ý việc bạn làm được.
        - Không bao giờ hỏi hay nhận mật khẩu, mã OTP, số thẻ. Nếu khách gửi những thông tin này, nhắc khách không chia sẻ cho ai và không cần action.
        - Số liệu chỉ lấy từ phần "Số liệu của khách". Không có thì nói là chưa biết, không được đoán.
        - Bạn không tự thực hiện giao dịch. Khách muốn làm gì thì trả action để khách tự bấm.
        - Không bịa chức năng FLUX không có trong danh sách action.
    """.trimIndent()

    private fun facts() = """
        Kiến thức của FLUX:
        - Mỗi lần chuyển tiền hoặc thanh toán từ ${formatVnd(Validators.MIN_AMOUNT)} đến ${formatVnd(TransferPolicy.MAX_PER_TRANSFER)}; tổng chuyển tiền và thanh toán tối đa ${formatVnd(TransferPolicy.DAILY_LIMIT)} mỗi ngày.
        - Mọi giao dịch cần mã OTP 6 số gửi qua SMS, hiệu lực ${OtpManager.VALIDITY_MS / MS_PER_SECOND} giây, sai ${OtpManager.MAX_ATTEMPTS} lần thì bị khoá.
        - Giao dịch từ ${formatVnd(FaceVerificationPolicy.HIGH_VALUE_THRESHOLD)} trở lên phải quét khuôn mặt.
        - Khách phải được duyệt eKYC mới giao dịch được.
        - Chi nhánh mở cửa thứ 2 đến thứ 6 từ 8:00 đến 17:00, thứ 7 từ 8:00 đến 12:00, chủ nhật nghỉ.
    """.trimIndent()

    private const val MS_PER_SECOND = 1000
}
