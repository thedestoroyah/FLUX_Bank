package com.flux.bank.data.routing

import com.flux.bank.domain.branch.GeoPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OsrmWalkingRouterTest {

    // Rút gọn từ phản hồi thật của routing.openstreetmap.de (2026-11-12)
    private val realJson = """
        {"code":"Ok","routes":[{"legs":[{"steps":[],"weight":105.14,"summary":"","duration":776.9,"distance":968.6}],
        "weight_name":"routability","geometry":{"coordinates":[[106.697335,10.732705],[106.697327,10.73217],[106.69733,10.731832]],"type":"LineString"},
        "weight":105.14,"duration":776.9,"distance":968.6}],"waypoints":[]}
    """.trimIndent()

    @Test
    fun parsesRealResponse() {
        val route = OsrmWalkingRouter.parse(realJson)
        assertEquals(968.6, route.distanceMeters, 0.0)
        assertEquals(776.9, route.durationSeconds, 0.0)
        assertEquals(3, route.path.size)
        assertEquals(GeoPoint(10.732705, 106.697335), route.path.first())
    }

    @Test(expected = RoutingException::class)
    fun errorCodeThrows() {
        OsrmWalkingRouter.parse("""{"code":"NoRoute","routes":[]}""")
    }

    @Test(expected = RoutingException::class)
    fun missingRoutesThrows() {
        OsrmWalkingRouter.parse("""{"code":"Ok"}""")
    }

    @Test
    fun urlUsesLngLatOrderAndFootProfile() {
        assertEquals(
            "https://routing.openstreetmap.de/routed-foot/route/v1/foot/106.2,10.1;106.4,10.3?overview=full&geometries=geojson",
            OsrmWalkingRouter.url(GeoPoint(10.1, 106.2), GeoPoint(10.3, 106.4))
        )
    }

    @Test
    fun routeUsesFetcher() = runTest {
        var requested = ""
        val router = OsrmWalkingRouter { url -> requested = url; realJson }
        val route = router.route(GeoPoint(10.1, 106.2), GeoPoint(10.3, 106.4))
        assertEquals(968.6, route.distanceMeters, 0.0)
        assertEquals(OsrmWalkingRouter.url(GeoPoint(10.1, 106.2), GeoPoint(10.3, 106.4)), requested)
    }
}
