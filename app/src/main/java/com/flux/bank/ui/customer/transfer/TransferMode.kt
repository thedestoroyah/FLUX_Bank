package com.flux.bank.ui.customer.transfer

import com.flux.bank.data.model.Banks

/** Ba cách chuyển trên tab Chuyển tiền (spec §4). */
enum class TransferMode { INTERNAL, OWN, EXTERNAL }

/** Người nhận đã chuyển trước đây, suy ra từ lịch sử TRANSFER_OUT. */
data class RecentPayee(val bankCode: String, val accountNumber: String, val name: String)

/** Dữ liệu điền sẵn vào form chuyển (từ "Người nhận gần đây" hoặc mã QR). */
data class TransferPrefill(val accountNumber: String, val bankCode: String = Banks.FLUX, val name: String = "")
