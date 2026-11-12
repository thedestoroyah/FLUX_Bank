package com.flux.bank.data.model

/** Loại giao dịch. PAYMENT = thanh toán dịch vụ của Feature 5 (tiền ra, như WITHDRAW). */
enum class TransactionType(val isIncoming: Boolean) {
    DEPOSIT(true),
    WITHDRAW(false),
    TRANSFER_OUT(false),
    TRANSFER_IN(true),
    PAYMENT(false)
}

/**
 * Một giao dịch làm đổi số dư, document `transactions/{id}` (chỉ tạo, không sửa/xoá).
 * [amount] luôn dương; chiều tiền suy ra từ [type]. [balanceAfter] để rules đối chiếu với số dư mới;
 * ở TRANSFER_IN là null vì người gửi không đọc được số dư người nhận.
 *
 * Chuyển tiền: `counterparty*` là bên kia (tên, số tài khoản, mã ngân hàng — `Banks.FLUX` nếu cùng ngân hàng);
 * [pairTxId] nối OUT với IN của cùng một lần chuyển trong FLUX (null nếu liên ngân hàng).
 * [paymentRef] là mã giao dịch VNPay của lần nạp; với PAYMENT là khoá hoá đơn hoặc mã đặt chỗ.
 * Thanh toán dịch vụ: [service] = tên enum `Utility`, [details] = các dòng biên lai theo thứ tự.
 */
data class Transaction(
    val id: String = "",
    val accountId: String = "",
    val ownerUid: String = "",
    val accountNumber: String = "",
    val type: TransactionType = TransactionType.DEPOSIT,
    val amount: Long = 0,
    val balanceAfter: Long? = 0,
    val description: String = "",
    val counterpartyName: String = "",
    val counterpartyAccount: String = "",
    val counterpartyBank: String = "",
    val pairTxId: String? = null,
    val paymentRef: String? = null,
    val service: String? = null,
    val details: List<DetailRow> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
