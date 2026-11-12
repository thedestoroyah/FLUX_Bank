package com.flux.bank.data.model

/** Một dòng nhãn–giá trị hiện trên biên lai thanh toán dịch vụ. Constructor không tham số để Firestore đọc được. */
data class DetailRow(val label: String = "", val value: String = "")

/**
 * Yêu cầu thanh toán dịch vụ (Feature 5) trước khi ghi thành giao dịch `PAYMENT`.
 * [service] là tên enum `Utility` của ô tiện ích; [merchant] ghi vào `counterpartyName`,
 * [reference] (mã khách hàng / SĐT / số hiệu chuyến…) ghi vào `counterpartyAccount`.
 * [paymentRef] là khoá hoá đơn hoặc mã đặt chỗ, null với nạp điện thoại.
 */
data class ServicePayment(
    val service: String,
    val amount: Long,
    val merchant: String,
    val reference: String,
    val description: String,
    val paymentRef: String?,
    val details: List<DetailRow>
)
