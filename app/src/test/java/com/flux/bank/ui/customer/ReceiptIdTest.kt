package com.flux.bank.ui.customer

import com.flux.bank.ui.customer.account.ReceiptFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ReceiptIdTest {

    @Test
    fun normalIdShowsFirstTenCharsUppercase() = assertEquals("B8TOKNE9MZ", ReceiptFragment.displayId("b8tokne9mzXYZ"))

    @Test
    fun billIdsAreDistinctAndHideUid() {
        val power = ReceiptFragment.displayId("bill_XRF37abc_EVN_HCM_PE01000123456_202610")
        val water = ReceiptFragment.displayId("bill_XRF37abc_SAWACO_2210456789_202610")
        assertNotEquals(power, water)
        assertFalse(power.contains("XRF37"))
        assertFalse(power.startsWith("BILL"))
        assertEquals(power, ReceiptFragment.displayId("bill_XRF37abc_EVN_HCM_PE01000123456_202610"))
    }
}
