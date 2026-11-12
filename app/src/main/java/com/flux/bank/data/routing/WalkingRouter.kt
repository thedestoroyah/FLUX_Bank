package com.flux.bank.data.routing

import com.flux.bank.domain.branch.GeoPoint
import java.io.IOException

/** Một đường đi bộ: quãng đường (m), thời gian (s) và các điểm để vẽ lên bản đồ. */
data class WalkingRoute(val distanceMeters: Double, val durationSeconds: Double, val path: List<GeoPoint>)

/** Server chỉ đường trả lỗi hoặc phản hồi không đọc được. */
class RoutingException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Tính đường đi bộ giữa hai điểm. */
interface WalkingRouter {
    /** Ném [RoutingException] hoặc [IOException] khi không có mạng, server lỗi hay không có đường. */
    suspend fun route(from: GeoPoint, to: GeoPoint): WalkingRoute
}
