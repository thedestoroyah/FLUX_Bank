package com.flux.bank.domain.branch

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Hình học trên mặt cầu cho khoảng cách ngắn. */
object GeoMath {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Khoảng cách đường chim bay (haversine) giữa [a] và [b], tính bằng mét. */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(sqrt(h))
    }
}
