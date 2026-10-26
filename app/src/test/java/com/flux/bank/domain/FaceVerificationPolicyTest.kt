package com.flux.bank.domain

import com.flux.bank.data.model.KycRecord
import com.flux.bank.data.model.KycStatus
import com.flux.bank.fakes.customer
import org.junit.Assert.assertEquals
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

    @Test
    fun precheckNeedsVerifiedKyc() {
        val pending = customer("c1", kyc = KycStatus.PENDING)
        assertEquals(FacePrecheck.NOT_VERIFIED, FaceVerificationPolicy.precheck(pending, KycRecord(faceEmbedding = listOf(1.0))))
    }

    @Test
    fun precheckNeedsEmbedding() {
        val verified = customer("c1", kyc = KycStatus.VERIFIED)
        assertEquals(FacePrecheck.NO_EMBEDDING, FaceVerificationPolicy.precheck(verified, KycRecord()))
        assertEquals(FacePrecheck.NO_EMBEDDING, FaceVerificationPolicy.precheck(verified, null))
        assertEquals(FacePrecheck.OK, FaceVerificationPolicy.precheck(verified, KycRecord(faceEmbedding = listOf(1.0, 0.0))))
    }
}
