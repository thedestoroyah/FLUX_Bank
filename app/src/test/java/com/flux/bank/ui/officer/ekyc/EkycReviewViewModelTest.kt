package com.flux.bank.ui.officer.ekyc

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.data.model.KycRecord
import com.flux.bank.data.model.KycStatus
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeKycRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EkycReviewViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val kyc = FakeKycRepository().apply {
        queue = mutableListOf(
            customer("c1", kyc = KycStatus.PENDING) to KycRecord(uid = "c1", submittedBy = "c1", submittedAt = 1),
            customer("c2", kyc = KycStatus.PENDING) to KycRecord(uid = "c2", submittedBy = "o1", submittedAt = 2)
        )
    }
    private val vm by lazy { EkycReviewViewModel(kyc, FakeAuthRepository().apply { uid = "o1" }).apply { load(); select("c1") } }

    @Test
    fun rejectWithoutReasonIsBlocked() {
        assertEquals(R.string.reject_reason_required, vm.reject(null, ""))
        assertTrue(kyc.reviews.isEmpty())
    }

    @Test
    fun otherReasonNeedsText() {
        assertEquals(R.string.reject_reason_required, vm.reject(RejectReason.OTHER, "   "))
        assertTrue(kyc.reviews.isEmpty())
    }

    @Test
    fun rejectSendsChosenReason() {
        assertNull(vm.reject(RejectReason.OTHER, "Ảnh bị loá"))
        assertEquals(listOf("c1", false, "Ảnh bị loá", "o1"), kyc.reviews.single())
    }

    @Test
    fun approveRemovesItemFromQueue() {
        vm.approve()
        assertEquals(listOf("c1", true, null, "o1"), kyc.reviews.single())
        assertEquals(listOf("c2"), vm.queue.value!!.map { it.first.uid })
        assertNull(vm.selected.value)
    }

    @Test
    fun assistedSubmissionsAreFlagged() {
        val assisted = vm.queue.value!!.associate { it.first.uid to EkycReviewViewModel.isAssisted(it.second) }
        assertEquals(mapOf("c1" to false, "c2" to true), assisted)
    }
}
