package com.flux.bank.data.model

enum class AccountType { CHECKING, SAVING, MORTGAGE }

/**
 * Tài khoản ngân hàng, document `accounts/{accountId}`. Số dư lưu bằng VND kiểu Long;
 * với MORTGAGE, [balance] là dư nợ.
 *
 * [lastTxId] là giao dịch vừa làm đổi số dư (rules dùng để đối chiếu). Các field khoản vay
 * ([loanAmount], [annualRate] %/năm cố định từ lúc mở, [termMonths], [startDate]) chỉ có ở MORTGAGE.
 */
data class BankAccount(
    val accountId: String = "",
    val ownerUid: String = "",
    val type: AccountType = AccountType.CHECKING,
    val accountNumber: String = "",
    val balance: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTxId: String? = null,
    val loanAmount: Long = 0,
    val annualRate: Double = 0.0,
    val termMonths: Int = 0,
    val startDate: Long = 0
)
