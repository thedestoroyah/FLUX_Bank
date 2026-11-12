package com.flux.bank.domain.assistant

/**
 * Các chức năng Trợ lý được phép mở bằng nút dưới câu trả lời (spec F7 §5).
 * Tên enum cũng là giá trị Gemini trả trong mảng `actions`, nên đổi tên là đổi hợp đồng với prompt.
 */
enum class AssistantAction {
    TOPUP, BILL_ELECTRICITY, BILL_WATER, MONTH_HISTORY, TRANSFER, DEPOSIT, WITHDRAW,
    SAVING, FLIGHT, MOVIE, HOTEL, SHOP, BRANCH
}
