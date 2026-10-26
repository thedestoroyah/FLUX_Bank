package com.flux.bank.data

import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.KycRecord
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.model.User
import com.flux.bank.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelDefaultsTest {

    @Test
    fun kycStatusConstantsMatchFirestoreValues() {
        assertEquals("NOT_SUBMITTED", KycStatus.NOT_SUBMITTED)
        assertEquals("PENDING", KycStatus.PENDING)
        assertEquals("VERIFIED", KycStatus.VERIFIED)
        assertEquals("REJECTED", KycStatus.REJECTED)
    }

    @Test
    fun userDefaultsAreSafe() {
        val user = User()
        assertEquals(UserRole.CUSTOMER, user.role)
        assertEquals(KycStatus.NOT_SUBMITTED, user.kycStatus)
        assertFalse(user.biometricEnabled)
        assertFalse(user.mustChangePassword)
        assertNull(user.kycRejectReason)
        assertNull(user.avatarBase64)
        assertTrue(user.createdAt > 0L)
    }

    @Test
    fun bankAccountDefaultsToEmptyChecking() {
        val account = BankAccount()
        assertEquals(0L, account.balance)
        assertEquals(AccountType.CHECKING, account.type)
    }

    @Test
    fun kycRecordStartsUnreviewedWithoutEmbedding() {
        val record = KycRecord()
        assertTrue(record.faceEmbedding.isEmpty())
        assertNull(record.reviewedBy)
        assertNull(record.reviewedAt)
    }
}
