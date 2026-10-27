package com.flux.bank.ui.customer

import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import com.flux.bank.ui.customer.transfer.RecentPayee
import com.flux.bank.ui.customer.transfer.TransferHubViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferHubViewModelTest {

    private fun out(bank: String, number: String, name: String) =
        Transaction(type = TransactionType.TRANSFER_OUT, counterpartyBank = bank, counterpartyAccount = number, counterpartyName = name)

    @Test
    fun recentPayeesDistinctNewestFirst() {
        val history = listOf(
            out("FLUX", "886855309182", "TRAN THI BINH"),
            Transaction(type = TransactionType.TRANSFER_IN, counterpartyBank = "FLUX", counterpartyAccount = "886800000009", counterpartyName = "Ai Do"),
            out("VCB", "0071000412345", "LE VAN CUONG"),
            out("FLUX", "886855309182", "TRAN THI BINH")
        ) + (1..5).map { out("FLUX", "88680000000$it", "NGUOI $it") }
        val recent = TransferHubViewModel.recentPayees(history)
        assertEquals(TransferHubViewModel.MAX_RECENT, recent.size)
        assertEquals(RecentPayee("FLUX", "886855309182", "TRAN THI BINH"), recent[0])
        assertEquals(RecentPayee("VCB", "0071000412345", "LE VAN CUONG"), recent[1])
        assertEquals(recent.size, recent.distinct().size)
    }

    @Test
    fun emptyHistoryGivesEmpty() = assertTrue(TransferHubViewModel.recentPayees(emptyList()).isEmpty())
}
