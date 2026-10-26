package com.flux.bank.ui.officer.customers

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.flux.bank.R
import com.flux.bank.domain.CustomerOnboardingService
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import com.flux.bank.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CustomerDetailViewModelTest {

    @get:Rule val instant = InstantTaskExecutorRule()
    @get:Rule val main = MainDispatcherRule()

    private val auth = FakeAuthRepository().apply { uid = "o1" }
    private val users = FakeUserRepository(
        listOf(
            customer("c1", phone = "0912345678", nationalId = "079203001234").copy(address = "Q.7"),
            customer("c2", phone = "0987111222", nationalId = "079301004567")
        )
    )
    private val service = CustomerOnboardingService(auth, users, FakeAccountRepository()) { "o1" }
    private val vm by lazy { CustomerDetailViewModel(users, auth, service).apply { load("c1") } }

    @Test
    fun anotherCustomersNationalIdIsRejectedWithoutWriting() {
        vm.save("Khách c1", "0912345678", "079301004567", "Q.7")
        assertEquals(mapOf(CustomerField.NATIONAL_ID to R.string.err_national_id_taken), vm.state.value!!.fieldErrors)
        assertTrue(users.updates.isEmpty())
    }

    @Test
    fun savesChangedFieldsAndMovesPhone() {
        vm.save("Nguyễn Văn An", "0909000111", "079203001234", "Q.1")
        assertEquals(Triple("c1", "0912345678", "0909000111"), users.phoneChanges.single())
        assertEquals("c1" to mapOf<String, Any?>("fullName" to "Nguyễn Văn An", "address" to "Q.1"), users.updates.single())
        assertTrue(vm.state.value!!.saved)
    }

    @Test
    fun blankNameIsRejected() {
        vm.save("  ", "0912345678", "079203001234", "Q.7")
        assertEquals(R.string.err_name_required, vm.state.value!!.fieldErrors[CustomerField.NAME])
    }

    @Test
    fun resetPasswordEmailsTheCustomer() {
        vm.resetPassword()
        assertEquals("c1@mail.com", auth.resetEmail)
    }
}
