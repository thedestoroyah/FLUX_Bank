package com.flux.bank.data.location

import com.flux.bank.domain.branch.GeoPoint

/** Kết quả một lần lấy vị trí. */
sealed interface LocationResult {
    data class Found(val point: GeoPoint) : LocationResult
    /** Định vị của máy đang tắt. */
    data object Disabled : LocationResult
    /** Quá thời gian chờ mà chưa có vị trí. */
    data object Timeout : LocationResult
}

/** Lấy vị trí hiện tại một lần. Bên gọi phải kiểm quyền vị trí trước. */
interface LocationSource {
    suspend fun current(): LocationResult
}
