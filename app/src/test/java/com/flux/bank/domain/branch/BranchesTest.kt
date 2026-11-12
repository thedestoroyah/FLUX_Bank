package com.flux.bank.domain.branch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BranchesTest {

    @Test
    fun idsUnique() {
        assertEquals(Branches.ALL.size, Branches.ALL.map { it.id }.toSet().size)
    }

    @Test
    fun atLeastTwelve() {
        assertTrue(Branches.ALL.size >= 12)
    }

    @Test
    fun allInsideHoChiMinhCity() {
        Branches.ALL.forEach {
            assertTrue(it.name, it.location.lat in 10.6..11.0 && it.location.lng in 106.5..106.9)
        }
    }

    @Test
    fun demoClusterNearTdtu() {
        val near = Branches.ALL.count { GeoMath.distanceMeters(Branches.DEMO_ANCHOR, it.location) <= 2_000.0 }
        assertTrue("chỉ có $near chi nhánh trong 2 km", near >= 3)
    }
}
