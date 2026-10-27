package com.flux.bank.data.model

/**
 * Lãi suất theo chính sách ngân hàng, document `config/rates`. Đơn vị %/năm.
 * Giá trị mặc định dùng khi document chưa tồn tại.
 */
data class Rates(
    val savingRate: Double = 4.5,
    val mortgageRate: Double = 8.5,
    val updatedAt: Long = 0,
    val updatedBy: String = "",
    val updatedByName: String = ""
)
