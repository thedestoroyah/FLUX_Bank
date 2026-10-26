package com.flux.bank.ui.officer.customers

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.data.model.KycStatus
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CustomerListViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val users = FakeUserRepository(
        listOf(
            customer("c1", phone = "0912345678", name = "Nguyễn Văn An", kyc = KycStatus.VERIFIED),
            customer("c2", phone = "0987111222", name = "Trần Bảo Ngọc", kyc = KycStatus.PENDING),
            customer("c3", phone = "0903456789", name = "Đặng Thu Trang", kyc = KycStatus.NOT_SUBMITTED)
        )
    )
    private val vm by lazy { CustomerListViewModel(users).apply { load() } }

    private fun names() = vm.visible.value!!.map { it.fullName }

    @Test
    fun searchIgnoresVietnameseAccents() {
        vm.search("nguyen")
        assertEquals(listOf("Nguyễn Văn An"), names())
        vm.search("dang")
        assertEquals(listOf("Đặng Thu Trang"), names())
    }

    @Test
    fun searchMatchesPhoneAndEmail() {
        vm.search("0912")
        assertEquals(listOf("Nguyễn Văn An"), names())
        vm.search("c2@mail")
        assertEquals(listOf("Trần Bảo Ngọc"), names())
    }

    @Test
    fun filterByKycStatus() {
        vm.filter(KycFilter.PENDING)
        assertEquals(listOf("Trần Bảo Ngọc"), names())
        vm.filter(KycFilter.ALL)
        assertEquals(3, names().size)
    }
}
