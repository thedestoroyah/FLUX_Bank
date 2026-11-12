package com.flux.bank.domain.branch

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {

    @Test
    fun samePointIsZero() {
        val p = GeoPoint(10.7325, 106.6992)
        assertEquals(0.0, GeoMath.distanceMeters(p, p), 0.0)
    }

    @Test
    fun knownDistance() {
        val benThanh = GeoPoint(10.7725, 106.6980)
        val ducBa = GeoPoint(10.7798, 106.6990)
        assertEquals(818.0, GeoMath.distanceMeters(benThanh, ducBa), 10.0)
    }
}
