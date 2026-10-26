package com.flux.bank.ui.ekyc

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.data.model.KycStatus
import com.flux.bank.fakes.FakeKycRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.Dispatchers

class EkycViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private class FakePhoto(val tag: String) : CapturedPhoto {
        override fun toBase64() = "b64-$tag"
    }

    private val kyc = FakeKycRepository()
    private val users = FakeUserRepository(
        listOf(customer("c1", kyc = KycStatus.REJECTED).copy(kycRejectReason = "Ảnh CCCD bị mờ"))
    )
    private var analysis: FaceAnalysis = FaceAnalysis.One(floatArrayOf(0.6f, 0.8f))
    private val vm = EkycViewModel(kyc, users, analyzeFace = { analysis }, worker = Dispatchers.Unconfined)

    @Test
    fun rejectedStatusExposesReason() {
        vm.load("c1")
        val s = vm.state.value!!
        assertEquals(KycStatus.REJECTED, s.status)
        assertEquals("Ảnh CCCD bị mờ", s.rejectReason)
    }

    @Test
    fun submitWithoutIdPhotoShowsError() {
        vm.load("c1")
        vm.setSelfie(FakePhoto("face"))
        vm.submit("c1", submittedBy = "c1")
        assertEquals(R.string.ekyc_need_id, vm.state.value!!.errorRes)
        assertTrue(kyc.submitted.isEmpty())
    }

    @Test
    fun selfieWithTwoFacesIsRejectedBeforeUpload() {
        analysis = FaceAnalysis.MultipleFaces
        vm.load("c1")
        vm.setIdPhoto(FakePhoto("id"))
        vm.setSelfie(FakePhoto("face"))
        vm.submit("c1", submittedBy = "c1")
        assertEquals(R.string.ekyc_need_one_face, vm.state.value!!.errorRes)
        assertTrue(kyc.submitted.isEmpty())
    }

    @Test
    fun selfieWithoutFaceIsRejected() {
        analysis = FaceAnalysis.NoFace
        vm.load("c1")
        vm.setIdPhoto(FakePhoto("id"))
        vm.setSelfie(FakePhoto("face"))
        vm.submit("c1", submittedBy = "c1")
        assertEquals(R.string.ekyc_need_one_face, vm.state.value!!.errorRes)
    }

    @Test
    fun validSubmitSendsEmbeddingAndSubmitter() {
        vm.load("c1")
        vm.setIdPhoto(FakePhoto("id"))
        vm.setSelfie(FakePhoto("face"))
        vm.submit("c1", submittedBy = "officer1")
        val record = kyc.submitted.single()
        assertEquals("c1", record.uid)
        assertEquals("officer1", record.submittedBy)
        assertEquals("b64-id", record.idCardBase64)
        assertEquals("b64-face", record.faceImageBase64)
        assertEquals(2, record.faceEmbedding.size)
        assertTrue(vm.state.value!!.submitted)
    }
}
