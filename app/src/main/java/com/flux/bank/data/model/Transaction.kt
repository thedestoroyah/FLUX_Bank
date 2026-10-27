package com.flux.bank.data.model

/** Loại giao dịch. Feature 4/5 sẽ thêm chuyển tiền, thanh toán hoá đơn... */
enum class TransactionType(val isIncoming: Boolean) {
    DEPOSIT(true),
    WITHDRAW(false)
}

/**
 * Một giao dịch làm đổi số dư, document `transactions/{id}` (chỉ tạo, không sửa/xoá).
 * [amount] luôn dương; chiều tiền suy ra từ [type]. [balanceAfter] để rules đối chiếu với số dư mới.
 */
data class Transaction(
    val id: String = "",
    val accountId: String = "",
    val ownerUid: String = "",
    val accountNumber: String = "",
    val type: TransactionType = TransactionType.DEPOSIT,
    val amount: Long = 0,
    val balanceAfter: Long = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
