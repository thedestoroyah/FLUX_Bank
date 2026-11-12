package com.flux.bank.ui.customer.branch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.data.location.LocationResult
import com.flux.bank.domain.branch.BranchRecommender
import com.flux.bank.domain.branch.Branches
import com.flux.bank.domain.branch.GeoPoint
import com.flux.bank.fakes.FakeLocationSource
import com.flux.bank.fakes.FakeWalkingRouter
import com.flux.bank.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BranchMapViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val location = FakeLocationSource()
    private val router = FakeWalkingRouter()
    private val vm = BranchMapViewModel(location, BranchRecommender(router))

    private fun s() = vm.state.value!!
    private fun branch(id: String) = Branches.ALL.first { it.id == id }

    @Test
    fun noPermissionShowsBranchesByName() {
        vm.start(hasPermission = false)
        assertEquals(LocationIssue.NO_PERMISSION, s().issue)
        assertNull(s().user)
        assertEquals(Branches.ALL.map { it.name }.sorted(), s().branches.map { it.branch.name })
        assertEquals(0, location.calls)
    }

    @Test
    fun blockedPermission() {
        vm.onPermissionResult(granted = false, canAskAgain = false)
        assertEquals(LocationIssue.PERMISSION_BLOCKED, s().issue)
    }

    @Test
    fun locationDisabled() {
        location.result = LocationResult.Disabled
        vm.start(hasPermission = true)
        assertEquals(LocationIssue.DISABLED, s().issue)
        assertFalse(s().loading)
    }

    @Test
    fun locationTimeout() {
        location.result = LocationResult.Timeout
        vm.start(hasPermission = true)
        assertEquals(LocationIssue.TIMEOUT, s().issue)
        assertFalse(s().loading)
    }

    @Test
    fun recommendsAndDrawsWalkingRoute() {
        vm.start(hasPermission = true)
        val rec = s().recommendation!!
        assertNull(s().issue)
        assertEquals(Branches.DEMO_ANCHOR, s().user)
        assertEquals(rec.pick.branch.id, s().selectedId)
        assertTrue(s().route is RouteUi.Walking)
        assertFalse(s().routeFailed)
        assertFalse(s().loading)
    }

    @Test
    fun allRoutesFailShowsStraightLine() {
        router.fail = true
        vm.start(hasPermission = true)
        val pick = s().recommendation!!.pick
        assertEquals(RouteUi.Straight(pick.straightMeters), s().route)
        assertTrue(s().routeFailed)
    }

    @Test
    fun selectOtherBranchRoutesIt() {
        vm.start(hasPermission = true)
        vm.select("q1-ben-thanh")
        assertEquals("q1-ben-thanh", s().selectedId)
        assertEquals(branch("q1-ben-thanh").location, (s().route as RouteUi.Walking).route.path.last())
        assertEquals(4, router.calls.size)
    }

    @Test
    fun selectCachedBranchDoesNotCallRouter() {
        vm.start(hasPermission = true)
        val candidate = s().recommendation!!.ranked.first { it.walk != null && it.branch.id != s().selectedId }
        vm.select(candidate.branch.id)
        assertEquals(3, router.calls.size)
        assertEquals(RouteUi.Walking(candidate.walk!!), s().route)
    }

    @Test
    fun backToRecommendationRestoresPick() {
        vm.start(hasPermission = true)
        val pick = s().recommendation!!.pick
        vm.select("q1-ben-thanh")
        vm.backToRecommendation()
        assertEquals(pick.branch.id, s().selectedId)
        assertEquals(RouteUi.Walking(pick.walk!!), s().route)
    }

    @Test
    fun latestSelectionWins() {
        vm.start(hasPermission = true)
        val slow = CompletableDeferred<Unit>()
        router.gates[branch("q1-ben-thanh").location] = slow
        vm.select("q1-ben-thanh")
        vm.select("q4-khanh-hoi")
        slow.complete(Unit)
        assertEquals("q4-khanh-hoi", s().selectedId)
        assertEquals(branch("q4-khanh-hoi").location, (s().route as RouteUi.Walking).route.path.last())
        assertFalse(s().loading)
    }

    @Test
    fun relocateClearsRouteCache() {
        vm.start(hasPermission = true)
        location.result = LocationResult.Found(GeoPoint(10.7725, 106.6980))
        vm.locate()
        assertEquals(6, router.calls.size)
        assertEquals(GeoPoint(10.7725, 106.6980), s().user)
    }

    @Test
    fun startTwiceLocatesOnce() {
        vm.start(hasPermission = true)
        vm.start(hasPermission = true)
        assertEquals(1, location.calls)
    }

    @Test
    fun permissionGrantedLaterLocates() {
        vm.start(hasPermission = false)
        vm.onPermissionResult(granted = true, canAskAgain = true)
        assertEquals(1, location.calls)
        assertNull(s().issue)
        assertNotNull(s().recommendation)
    }

    @Test
    fun retryRouteAfterFailure() {
        router.fail = true
        vm.start(hasPermission = true)
        router.fail = false
        vm.retryRoute()
        assertTrue(s().route is RouteUi.Walking)
        assertFalse(s().routeFailed)
    }
}
