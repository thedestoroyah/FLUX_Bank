package com.flux.bank.core

import com.flux.bank.core.format.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun stripsVietnameseMarksIncludingD() =
        assertEquals("Tran Thi Binh Dang", TextNormalizer.stripDiacritics("Trần Thị Bình Đặng"))

    @Test
    fun holderNameIsUpperCaseNoMarksSingleSpaced() =
        assertEquals("NGUYEN VAN AN", TextNormalizer.holderName("  nguyễn   văn  an "))
}
