package com.flux.bank.face

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlinkDetectorTest {

    @Test
    fun openClosedOpenIsABlink() {
        val b = BlinkDetector()
        assertFalse(b.onFrame(.9f, .9f))
        assertFalse(b.onFrame(.1f, .1f))
        assertTrue(b.onFrame(.9f, .9f))
    }

    @Test
    fun eyesAlwaysOpenIsNotABlink() {
        val b = BlinkDetector()
        repeat(10) { assertFalse(b.onFrame(.9f, .9f)) }
    }

    @Test
    fun startingWithClosedEyesNeedsOpenFirst() {
        val b = BlinkDetector()
        assertFalse(b.onFrame(.1f, .1f))
        assertFalse(b.onFrame(.9f, .9f))
        assertFalse(b.onFrame(.1f, .1f))
        assertTrue(b.onFrame(.9f, .9f))
    }

    @Test
    fun nullProbabilitiesAreIgnored() {
        val b = BlinkDetector()
        b.onFrame(.9f, .9f)
        b.onFrame(null, null)
        b.onFrame(.1f, .1f)
        assertTrue(b.onFrame(.9f, .9f))
    }

    @Test
    fun oneEyeClosedIsNotABlink() {
        val b = BlinkDetector()
        b.onFrame(.9f, .9f)
        b.onFrame(.1f, .9f)
        assertFalse(b.onFrame(.9f, .9f))
    }

    @Test
    fun resetStartsOver() {
        val b = BlinkDetector()
        b.onFrame(.9f, .9f)
        b.onFrame(.1f, .1f)
        b.reset()
        assertFalse(b.onFrame(.9f, .9f))
    }
}
