package com.flux.bank.ui.customer

import com.flux.bank.R
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import com.flux.bank.domain.utility.CatalogKind
import com.flux.bank.ui.customer.account.TransactionRowAdapter
import com.flux.bank.ui.customer.account.TransactionRowAdapter.TxLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionLabelTest {

    private fun label(tx: Transaction) = TransactionRowAdapter.labelFor(tx)

    @Test
    fun transferOutNamesRecipient() = assertEquals(
        TxLabel(R.string.tx_transfer_out, "TRAN THI BINH"),
        label(Transaction(type = TransactionType.TRANSFER_OUT, counterpartyName = "TRAN THI BINH"))
    )

    @Test
    fun transferInNamesSenderWithoutMarks() = assertEquals(
        TxLabel(R.string.tx_transfer_in, "NGUYEN VAN AN"),
        label(Transaction(type = TransactionType.TRANSFER_IN, counterpartyName = "Nguyễn Văn An"))
    )

    @Test
    fun depositViaVnPay() =
        assertEquals(TxLabel(R.string.tx_deposit_vnpay), label(Transaction(type = TransactionType.DEPOSIT, paymentRef = "14000001")))

    @Test
    fun plainDepositAndWithdraw() {
        assertEquals(TxLabel(R.string.util_deposit), label(Transaction(type = TransactionType.DEPOSIT)))
        assertEquals(TxLabel(R.string.util_withdraw), label(Transaction(type = TransactionType.WITHDRAW)))
    }

    @Test
    fun paymentShowsDescription() = assertEquals(
        TxLabel(R.string.tx_payment_title, "Tiền điện kỳ 10/2026"),
        label(Transaction(type = TransactionType.PAYMENT, description = "Tiền điện kỳ 10/2026"))
    )

    @Test
    fun fromServiceMapsUtilityNames() {
        assertEquals(Utility.VE_PHIM, Utility.fromService("VE_PHIM"))
        assertNull(Utility.fromService("XYZ"))
        assertNull(Utility.fromService(null))
    }

    @Test
    fun catalogServicesAreUtilities() = CatalogKind.entries.forEach { assertNotNull(it.name, Utility.fromService(it.service)) }
}
