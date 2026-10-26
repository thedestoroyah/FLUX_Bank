package com.flux.bank.data.model

enum class AccountType { CHECKING, SAVING, MORTGAGE }

/** Tài khoản ngân hàng, document `accounts/{accountId}`. Số dư lưu bằng VND kiểu Long. */
data class BankAccount(
    val accountId: String = "",
    val ownerUid: String = "",
    val type: AccountType = AccountType.CHECKING,
    val accountNumber: String = "",
    val balance: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
