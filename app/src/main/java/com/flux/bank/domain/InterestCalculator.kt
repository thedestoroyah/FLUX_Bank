package com.flux.bank.domain

import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

/** Kỳ trả nợ của khoản vay thế chấp. */
enum class PaymentFrequency { MONTHLY, BIWEEKLY }

/**
 * Các công thức tiền lãi của Feature 3 (spec §4). Lãi suất truyền vào là %/năm (ví dụ 8.5).
 * Tiền tính bằng Double rồi làm tròn: đủ chính xác cho VND vì không có phần lẻ dưới 1 đồng.
 */
object InterestCalculator {

    private const val MONTHS_PER_YEAR = 12
    private const val BIWEEKS_PER_YEAR = 26
    private const val DAYS_PER_BIWEEK = 14L

    /** Lãi tiết kiệm dự kiến mỗi tháng, làm tròn xuống đến đồng. */
    fun monthlySavingInterest(balance: Long, annualRatePercent: Double): Long =
        floor(balance * annualRatePercent / 100 / MONTHS_PER_YEAR).toLong()

    /** Số kỳ trả: hàng tháng = số tháng; 2 tuần = số tháng × 26 / 12, làm tròn. */
    fun periods(termMonths: Int, frequency: PaymentFrequency): Int = when (frequency) {
        PaymentFrequency.MONTHLY -> termMonths
        PaymentFrequency.BIWEEKLY -> (termMonths * BIWEEKS_PER_YEAR / MONTHS_PER_YEAR.toDouble()).roundToInt()
    }

    /**
     * Tiền trả mỗi kỳ theo niên kim P·r / (1 − (1+r)^−n), làm tròn lên đến đồng.
     * Lãi suất 0 thì chia đều gốc cho số kỳ.
     */
    fun installment(principal: Long, annualRatePercent: Double, termMonths: Int, frequency: PaymentFrequency): Long {
        val n = periods(termMonths, frequency)
        if (annualRatePercent == 0.0) return ceil(principal.toDouble() / n).toLong()
        val perYear = if (frequency == PaymentFrequency.MONTHLY) MONTHS_PER_YEAR else BIWEEKS_PER_YEAR
        val r = annualRatePercent / 100 / perYear
        return ceil(principal * r / (1 - (1 + r).pow(-n))).toLong()
    }

    /** Tổng lãi phải trả trong cả thời hạn vay. */
    fun totalInterest(principal: Long, annualRatePercent: Double, termMonths: Int, frequency: PaymentFrequency): Long =
        installment(principal, annualRatePercent, termMonths, frequency) * periods(termMonths, frequency) - principal

    /**
     * Kỳ trả đầu tiên không trước [today] (đúng hôm nay vẫn tính).
     * Hàng tháng luôn cộng từ [startDate] để ngày 31 được kẹp về cuối tháng chứ không trôi dần.
     */
    fun nextDueDate(startDate: LocalDate, frequency: PaymentFrequency, today: LocalDate): LocalDate {
        var k = 1L
        while (true) {
            val due = when (frequency) {
                PaymentFrequency.MONTHLY -> startDate.plusMonths(k)
                PaymentFrequency.BIWEEKLY -> startDate.plusDays(DAYS_PER_BIWEEK * k)
            }
            if (!due.isBefore(today)) return due
            k++
        }
    }
}
