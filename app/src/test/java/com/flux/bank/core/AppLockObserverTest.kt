package com.flux.bank.core

import com.flux.bank.core.security.AppLockObserver
import com.flux.bank.core.security.LockPolicy
import com.flux.bank.fakes.FakeAuthRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLockObserverTest {

    private var now = 0L
    private var unlockStarts = 0
    private val auth = FakeAuthRepository().apply { uid = "c1" }

    private fun observer() = AppLockObserver(
        policy = LockPolicy(),
        auth = auth,
        biometricEnabled = { true },
        biometricAvailable = { true },
        startUnlock = { unlockStarts++ },
        now = { now }
    )

    @Test
    fun processRestoredByAndroidLocksOnFirstForeground() {
        // Android khôi phục thẳng màn chính (không qua LoginActivity) sau khi kill tiến trình
        observer().onAppForeground()
        assertEquals(1, unlockStarts)
    }

    @Test
    fun coldStartThroughLoginIsNotLockedTwice() {
        val o = observer()
        o.markColdStartHandled()
        o.onAppForeground()
        assertEquals(0, unlockStarts)
    }

    @Test
    fun locksOnlyAfterSixtySecondsInBackground() {
        val o = observer()
        o.markColdStartHandled()
        o.onAppForeground()
        o.onAppBackground(); now = 10_000; o.onAppForeground()
        assertEquals(0, unlockStarts)
        o.onAppBackground(); now = 70_000; o.onAppForeground()
        assertEquals(1, unlockStarts)
    }

    @Test
    fun noSessionNeverLocks() {
        auth.uid = null
        observer().onAppForeground()
        assertEquals(0, unlockStarts)
    }

    @Test
    fun visibleLockScreenIsNotStackedAgain() {
        val o = observer()
        o.lockScreenVisible = true
        o.onAppForeground()
        assertEquals(0, unlockStarts)
    }
}
