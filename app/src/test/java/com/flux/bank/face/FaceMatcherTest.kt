package com.flux.bank.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceMatcherTest {

    @Test
    fun cosineOfIdenticalVectorsIsOne() {
        assertEquals(1f, FaceMatcher.cosine(floatArrayOf(1f, 2f), floatArrayOf(1f, 2f)), 1e-5f)
    }

    @Test
    fun cosineOfOrthogonalVectorsIsZero() {
        assertEquals(0f, FaceMatcher.cosine(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f)), 1e-5f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun lengthMismatchThrows() {
        FaceMatcher.cosine(floatArrayOf(1f), floatArrayOf(1f, 0f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroVectorThrows() {
        FaceMatcher.cosine(floatArrayOf(0f, 0f), floatArrayOf(1f, 0f))
    }

    @Test
    fun matchUsesThresholdInclusive() {
        assertTrue(FaceMatcher.isMatch(floatArrayOf(1f, 0f), floatArrayOf(1f, 0f)))
        assertFalse(FaceMatcher.isMatch(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f)))
    }
}
