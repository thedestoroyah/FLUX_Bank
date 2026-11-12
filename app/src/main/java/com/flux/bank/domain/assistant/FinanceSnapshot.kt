package com.flux.bank.domain.assistant

import com.flux.bank.core.format.MoneyFormatter.formatRate
import com.flux.bank.core.format.MoneyFormatter.formatVnd
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.Rates
import com.flux.bank.data.model.Transaction
import com.flux.bank.data.model.TransactionType
import com.flux.bank.domain.InterestCalculator
import com.flux.bank.domain.MonthSummary
import com.flux.bank.domain.PaymentFrequency
import com.flux.bank.domain.TransferPolicy
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Số liệu tối thiểu của khách gửi kèm câu hỏi cho Gemini (spec F7 §5 "Ngữ cảnh").
 * Cố ý không giữ số tài khoản, SĐT, tên người nhận/người gửi: free tier của Google có thể dùng nội dung gửi lên.
 */
data class FinanceSnapshot(
    val today: LocalDate,
    val firstName: String,
    val accounts: List<BankAccount>,
    val rates: Rates?,
    val month: MonthSummary,
    val remainingToday: Long
) {

    /** Các dòng tiếng Việt đưa vào cuối system prompt. */
    fun toPromptText(): String = buildList {
        add("Hôm nay: ${today.format(DATE)} (giờ Việt Nam). Tên khách: $firstName.")
        add("Tài khoản: " + accounts.joinToString("; ", transform = ::accountLine) + ".")
        rates?.let { add("Lãi suất hiện hành: tiết kiệm ${formatRate(it.savingRate)}%/năm, vay ${formatRate(it.mortgageRate)}%/năm.") }
        addAll(monthLines())
        add("Hạn mức chuyển tiền và thanh toán còn lại hôm nay: ${formatVnd(remainingToday)}.")
    }.joinToString("\n")

    private fun monthLines(): List<String> {
        val label = "Tháng ${month.month.monthValue}/${month.month.year}"
        if (month.transactions.isEmpty()) return listOf("$label: chưa có giao dịch nào.")
        val spending = month.transactions.filterNot { it.type.isIncoming }
        val groups = spending.groupBy(::categoryLabel)
            .map { (name, items) -> name to items.sumOf { it.amount } }
            .sortedByDescending { it.second }
        val top = spending.sortedByDescending { it.amount }.take(TOP_COUNT)
        return listOfNotNull(
            "$label: tiền vào ${formatVnd(month.incoming)} (${month.incomingCount} giao dịch), " +
                "tiền ra ${formatVnd(month.outgoing)} (${month.outgoingCount} giao dịch).",
            groups.takeIf { it.isNotEmpty() }
                ?.joinToString("; ", "Chi theo nhóm tháng này: ", ".") { "${it.first} ${formatVnd(it.second)}" },
            top.takeIf { it.isNotEmpty() }
                ?.joinToString("; ", "Giao dịch tiền ra lớn nhất tháng này: ", ".") { "${dayOf(it)} ${categoryLabel(it)} ${formatVnd(it.amount)}" }
        )
    }

    private fun accountLine(account: BankAccount): String = when (account.type) {
        AccountType.CHECKING -> "Thanh toán ${formatVnd(account.balance)}"
        AccountType.SAVING -> "Tiết kiệm ${formatVnd(account.balance)}"
        AccountType.MORTGAGE -> {
            val due = InterestCalculator.installment(account.loanAmount, account.annualRate, account.termMonths, PaymentFrequency.MONTHLY)
            "Vay dư nợ ${formatVnd(account.balance)}, trả ${formatVnd(due)}/tháng"
        }
    }

    private fun dayOf(tx: Transaction) = Instant.ofEpochMilli(tx.createdAt).atZone(TransferPolicy.VN_ZONE).format(DAY)

    companion object {
        private const val TOP_COUNT = 5
        private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val DAY = DateTimeFormatter.ofPattern("dd/MM")

        // shortcut: trùng nghĩa với nhãn `Utility` trong strings.xml vì domain không đọc resource; thêm tiện ích mới thì thêm cả ở đây
        private val SERVICE_LABELS = mapOf(
            "TIEN_DIEN" to "tiền điện", "TIEN_NUOC" to "tiền nước", "NAP_DT" to "nạp điện thoại",
            "VE_MAY_BAY" to "vé máy bay", "VE_PHIM" to "vé xem phim", "KHACH_SAN" to "khách sạn", "MUA_SAM" to "mua sắm"
        )

        /**
         * Dựng ngữ cảnh từ dữ liệu đã tải. [history] là giao dịch gần đây của khách (mọi tài khoản),
         * [now] là thời điểm hiện tại (ms); tháng và "hôm nay" tính theo giờ Việt Nam. [rates] null = chưa tải được.
         */
        fun build(fullName: String, accounts: List<BankAccount>, history: List<Transaction>, rates: Rates?, now: Long): FinanceSnapshot {
            val today = Instant.ofEpochMilli(now).atZone(TransferPolicy.VN_ZONE).toLocalDate()
            return FinanceSnapshot(
                today = today,
                firstName = firstName(fullName),
                accounts = accounts,
                rates = rates,
                month = MonthSummary.of(history, YearMonth.from(today)),
                remainingToday = TransferPolicy.remainingToday(history, now)
            )
        }

        /** Tên gọi = từ cuối của họ tên ("Trần Thị Bình" -> "Bình"); họ tên rỗng -> "bạn". */
        fun firstName(fullName: String): String = fullName.trim().split(Regex("\\s+")).last().ifEmpty { "bạn" }

        /** Nhóm của giao dịch, tiếng Việt chữ thường ("tiền điện", "chuyển đi"...). */
        fun categoryLabel(tx: Transaction): String = when (tx.type) {
            TransactionType.TRANSFER_OUT -> "chuyển đi"
            TransactionType.WITHDRAW -> "rút tiền"
            TransactionType.PAYMENT -> SERVICE_LABELS[tx.service] ?: "thanh toán dịch vụ"
            TransactionType.DEPOSIT -> "nạp tiền"
            TransactionType.TRANSFER_IN -> "nhận chuyển khoản"
        }
    }
}
