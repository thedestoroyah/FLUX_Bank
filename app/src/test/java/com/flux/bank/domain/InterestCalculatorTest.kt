package com.flux.bank.domain

import com.flux.bank.domain.PaymentFrequency.BIWEEKLY
import com.flux.bank.domain.PaymentFrequency.MONTHLY
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class InterestCalculatorTest {

    @Test
    fun savingInterestFloors() {
        assertEquals(450_000L, InterestCalculator.monthlySavingInterest(120_000_000, 4.5))
        assertEquals(416_666L, InterestCalculator.monthlySavingInterest(100_000_000, 5.0))
    }

    @Test
    fun monthlyInstallmentMatchesMockup() {
        assertEquals(13_017_349L, InterestCalculator.installment(1_500_000_000, 8.5, 240, MONTHLY))
        assertEquals(1_624_163_760L, InterestCalculator.totalInterest(1_500_000_000, 8.5, 240, MONTHLY))
    }

    @Test
    fun biweeklyInstallmentMatchesMockup() {
        assertEquals(520, InterestCalculator.periods(240, BIWEEKLY))
        assertEquals(6_003_662L, InterestCalculator.installment(1_500_000_000, 8.5, 240, BIWEEKLY))
        assertEquals(1_621_904_240L, InterestCalculator.totalInterest(1_500_000_000, 8.5, 240, BIWEEKLY))
    }

    @Test
    fun zeroRateSplitsEvenlyRoundingUp() {
        assertEquals(100_000L, InterestCalculator.installment(1_200_000, 0.0, 12, MONTHLY))
        assertEquals(100_001L, InterestCalculator.installment(1_200_001, 0.0, 12, MONTHLY))
    }

    @Test
    fun nextDueDate() {
        val start = LocalDate.of(2026, 10, 5)
        val today = LocalDate.of(2026, 10, 27)
        assertEquals(LocalDate.of(2026, 11, 5), InterestCalculator.nextDueDate(start, MONTHLY, today))
        assertEquals(LocalDate.of(2026, 11, 2), InterestCalculator.nextDueDate(start, BIWEEKLY, today))
        // Hạn đúng hôm nay vẫn tính là hôm nay
        assertEquals(LocalDate.of(2026, 11, 5), InterestCalculator.nextDueDate(start, MONTHLY, LocalDate.of(2026, 11, 5)))
        assertEquals(LocalDate.of(2026, 2, 28), InterestCalculator.nextDueDate(LocalDate.of(2026, 1, 31), MONTHLY, LocalDate.of(2026, 2, 10)))
    }
}
