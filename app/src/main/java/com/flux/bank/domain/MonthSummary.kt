package com.flux.bank.domain

import com.flux.bank.data.model.Transaction
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * Tổng tiền vào / ra của một tháng (F7: màn "Giao dịch tháng này" và ngữ cảnh của Trợ lý).
 * [transactions] là giao dịch thuộc tháng đó, mới nhất trước. Chiều tiền theo `TransactionType.isIncoming`.
 */
data class MonthSummary(
    val month: YearMonth,
    val incoming: Long,
    val incomingCount: Int,
    val outgoing: Long,
    val outgoingCount: Int,
    val transactions: List<Transaction>
) {
    companion object {
        /**
         * Lọc [all] theo [month] (tháng tính theo múi giờ [zone], mặc định giờ Việt Nam) rồi cộng tổng.
         * Danh sách rỗng cho các tổng bằng 0.
         */
        fun of(all: List<Transaction>, month: YearMonth, zone: ZoneId = TransferPolicy.VN_ZONE): MonthSummary {
            val inMonth = all
                .filter { YearMonth.from(Instant.ofEpochMilli(it.createdAt).atZone(zone)) == month }
                .sortedByDescending { it.createdAt }
            val (incoming, outgoing) = inMonth.partition { it.type.isIncoming }
            return MonthSummary(
                month = month,
                incoming = incoming.sumOf { it.amount },
                incomingCount = incoming.size,
                outgoing = outgoing.sumOf { it.amount },
                outgoingCount = outgoing.size,
                transactions = inMonth
            )
        }
    }
}
