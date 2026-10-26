package com.flux.bank.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceVerificationPolicyTest {

    @Test
    fun faceRequiredFromTenMillionInclusive() {
        assertTrue(FaceVerificationPolicy.requiresFace(10_000_000))
        assertFalse(FaceVerificationPolicy.requiresFace(9_999_999))
        assertFalse(FaceVerificationPolicy.requiresFace(null))
    }
}
