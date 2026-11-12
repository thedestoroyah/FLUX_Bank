package com.flux.bank.domain.branch

import com.flux.bank.data.routing.RoutingException
import com.flux.bank.data.routing.WalkingRoute
import com.flux.bank.data.routing.WalkingRouter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BranchRecommenderTest {

    private val user = GeoPoint(10.73256, 106.69982)

    /** Chi nhánh ở phía bắc người dùng, cách khoảng [meters] mét theo đường thẳng. */
    private fun branch(id: String, meters: Double) =
        Branch(id, "FLUX $id", "addr", GeoPoint(user.lat + meters / 111_195.0, user.lng), "028")

    private class FakeRouter(private val walks: Map<String, Double?>, private val byLocation: Map<GeoPoint, String>) : WalkingRouter {
        val calls = mutableListOf<String>()
        override suspend fun route(from: GeoPoint, to: GeoPoint): WalkingRoute {
            val id = byLocation.getValue(to)
            calls += id
            val m = walks[id] ?: throw RoutingException("down")
            return WalkingRoute(m, m / 1.25, listOf(from, to))
        }
    }

    private fun setup(vararg spec: Pair<Double, Double?>): Pair<BranchRecommender, FakeRouter> {
        val branches = spec.mapIndexed { i, (straight, _) -> branch("b$i", straight) }
        val router = FakeRouter(
            branches.mapIndexed { i, b -> b.id to spec[i].second }.toMap(),
            branches.associate { it.location to it.id }
        )
        return BranchRecommender(router, branches) to router
    }

    @Test
    fun picksShortestWalkNotStraightLine() = runTest {
        val (rec, _) = setup(450.0 to 2_100.0, 700.0 to 850.0, 900.0 to 1_400.0)
        val r = rec.recommend(user)
        assertEquals("b1", r.pick.branch.id)
        assertEquals("b0", r.straightNearest.branch.id)
        assertTrue(r.routed)
        assertTrue(r.showsDetour)
    }

    @Test
    fun noDetourWhenSameBranch() = runTest {
        val (rec, _) = setup(450.0 to 500.0, 700.0 to 850.0, 900.0 to 1_400.0)
        val r = rec.recommend(user)
        assertEquals("b0", r.pick.branch.id)
        assertFalse(r.showsDetour)
    }

    @Test
    fun routesOnlyThreeNearest() = runTest {
        val (rec, router) = setup(900.0 to 1_000.0, 450.0 to 500.0, 3_000.0 to 3_100.0, 700.0 to 800.0, 5_000.0 to 5_100.0)
        rec.recommend(user)
        assertEquals(setOf("b0", "b1", "b3"), router.calls.toSet())
        assertEquals(3, router.calls.size)
    }

    @Test
    fun skipsFailedCandidate() = runTest {
        val (rec, _) = setup(450.0 to 2_100.0, 700.0 to null, 900.0 to 1_400.0)
        assertEquals("b2", rec.recommend(user).pick.branch.id)
    }

    @Test
    fun allFailFallsBackToStraight() = runTest {
        val (rec, _) = setup(450.0 to null, 700.0 to null, 900.0 to null)
        val r = rec.recommend(user)
        assertFalse(r.routed)
        assertEquals(r.straightNearest, r.pick)
        assertTrue(r.ranked.all { it.walk == null })
        assertFalse(r.showsDetour)
        assertFalse(r.tooFar)
    }

    @Test
    fun farAwayDoesNotCallRouter() = runTest {
        val (rec, router) = setup(21_000.0 to 21_500.0, 25_000.0 to 26_000.0)
        val r = rec.recommend(user)
        assertEquals(0, router.calls.size)
        assertFalse(r.routed)
        assertEquals("b0", r.pick.branch.id)
        assertTrue(r.tooFar)
    }

    @Test
    fun rankedOrder() = runTest {
        val (rec, _) = setup(450.0 to 2_100.0, 700.0 to 850.0, 900.0 to 1_400.0, 3_000.0 to 3_100.0, 2_000.0 to 2_200.0)
        val r = rec.recommend(user)
        assertEquals(listOf("b1", "b2", "b0", "b4", "b3"), r.ranked.map { it.branch.id })
        assertNull(r.ranked[3].walk)
    }
}
