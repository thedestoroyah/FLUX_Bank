package com.flux.bank.ui.officer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.data.model.KycRecord
import com.flux.bank.data.model.UserRole
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeKycRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import com.flux.bank.ui.officer.overview.OfficerOverviewViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OfficerOverviewViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun countsCustomersAndPendingKyc() {
        val users = FakeUserRepository(
            listOf(customer("c1"), customer("c2"), customer("o1", name = "Trần Thị Lan").copy(role = UserRole.BANK_OFFICER))
        )
        val kyc = FakeKycRepository().apply { queue = mutableListOf(customer("c2") to KycRecord(uid = "c2")) }
        val vm = OfficerOverviewViewModel(FakeAuthRepository().apply { uid = "o1" }, users, kyc).apply { load() }
        val s = vm.state.value!!
        assertEquals(2, s.customerCount)
        assertEquals(1, s.pendingCount)
        assertEquals("Trần Thị Lan", s.officerName)
        assertEquals(listOf("c2"), s.recentPending.map { it.first.uid })
    }
}
