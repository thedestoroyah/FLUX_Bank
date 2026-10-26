package com.flux.bank.domain

import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.UserRole
import com.flux.bank.fakes.FakeAccountRepository
import com.flux.bank.fakes.FakeAuthRepository
import com.flux.bank.fakes.FakeUserRepository
import com.flux.bank.fakes.customer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerOnboardingServiceTest {

    private val auth = FakeAuthRepository()
    private val users = FakeUserRepository(listOf(customer("c1", phone = "0912345678", nationalId = "079203001234")))
    private val accounts = FakeAccountRepository()
    private val service = CustomerOnboardingService(auth, users, accounts) { "officer1" }

    private fun input(
        phone: String = "0987 111 222",
        nationalId: String = "079301004567",
        email: String = "trang@gmail.com",
        password: String = "flux2026",
        name: String = "Phạm Thu Trang"
    ) = NewCustomerInput(name, email, phone, nationalId, "45 Lê Lợi", password)

    private fun errorOf(result: Result<*>) = (result.exceptionOrNull() as OnboardingException).error

    @Test
    fun createsProfileAndChecking() = runTest {
        val result = service.create(input())
        val user = result.getOrThrow()
        assertEquals(UserRole.CUSTOMER, user.role)
        assertTrue(user.mustChangePassword)
        assertEquals("officer1", user.createdBy)
        assertEquals("0987111222", users.users[user.uid]!!.phoneNumber)
        assertEquals(1, accounts.accounts.size)
        assertEquals(AccountType.CHECKING, accounts.accounts[0].type)
        assertEquals(0L, accounts.accounts[0].balance)
        assertTrue(auth.released)
    }

    @Test
    fun rejectsDuplicatePhoneBeforeAnyWrite() = runTest {
        val result = service.create(input(phone = "0912 345 678"))
        assertEquals(OnboardingError.PhoneTaken, errorOf(result))
        assertTrue(auth.created.isEmpty())
    }

    @Test
    fun rejectsDuplicateNationalId() = runTest {
        val result = service.create(input(nationalId = "079203001234"))
        assertEquals(OnboardingError.NationalIdTaken, errorOf(result))
        assertTrue(auth.created.isEmpty())
    }

    @Test
    fun rollsBackAuthUserWhenProfileWriteFails() = runTest {
        users.failNextCreate = true
        val result = service.create(input())
        assertTrue(result.isFailure)
        assertTrue(errorOf(result) is OnboardingError.Failed)
        assertTrue(auth.deleted)
    }

    @Test
    fun rollsBackWhenAccountCreateFails() = runTest {
        accounts.failNextCreate = true
        val result = service.create(input())
        assertTrue(result.isFailure)
        assertTrue(auth.deleted)
        assertEquals(1, users.users.size) // chỉ còn khách c1 ban đầu
    }

    @Test
    fun authCreateFailureIsReportedWithoutRollback() = runTest {
        auth.failCreate = true
        val result = service.create(input())
        assertTrue(errorOf(result) is OnboardingError.Failed)
        assertTrue(!auth.deleted)
    }

    @Test
    fun editAllowsOwnPhoneAndId() = runTest {
        assertNull(service.validateEdit("c1", "0912345678", "079203001234"))
    }

    @Test
    fun editRejectsOthersPhone() = runTest {
        assertEquals(OnboardingError.PhoneTaken, service.validateEdit("c2", "0912 345 678", "079301004567"))
    }

    @Test
    fun editRejectsOthersNationalId() = runTest {
        assertEquals(OnboardingError.NationalIdTaken, service.validateEdit("c2", "0987111222", "079203001234"))
    }

    @Test
    fun validationErrorsMapped() = runTest {
        assertEquals(OnboardingError.InvalidEmail, errorOf(service.create(input(email = "trang@"))))
        assertEquals(OnboardingError.WeakPassword, errorOf(service.create(input(password = "abc1"))))
        assertEquals(OnboardingError.EmptyName, errorOf(service.create(input(name = "  "))))
        assertEquals(OnboardingError.InvalidPhone, errorOf(service.create(input(phone = "12345"))))
        assertEquals(OnboardingError.InvalidNationalId, errorOf(service.create(input(nationalId = "123"))))
        assertTrue(auth.created.isEmpty())
    }

    @Test
    fun customerPhoneChangeChecksOnlyPhoneIndex() = runTest {
        assertNull(service.validatePhoneChange("c1", "0912 345 678"))
        assertEquals(OnboardingError.PhoneTaken, service.validatePhoneChange("c2", "0912345678"))
        assertEquals(OnboardingError.InvalidPhone, service.validatePhoneChange("c2", "12345"))
        assertEquals(0, users.nationalIdQueries)
    }
}
