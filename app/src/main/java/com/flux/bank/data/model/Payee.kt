package com.flux.bank.data.model

/**
 * Người nhận trong FLUX, đọc từ `accountIndex/{accountNumber}`.
 * [holderName] là tên gốc có dấu (đúng `users.fullName`, rules đối chiếu); hiển thị qua `TextNormalizer.holderName`.
 */
data class Payee(
    val accountId: String = "",
    val accountNumber: String = "",
    val ownerUid: String = "",
    val holderName: String = "",
    val type: AccountType = AccountType.CHECKING
)
