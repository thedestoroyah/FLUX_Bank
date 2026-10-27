package com.flux.bank.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrPayloadTest {

    @Test
    fun encodeAndDecodeRoundTrip() {
        assertEquals("fluxbank:886812034471", QrPayload.encode("886812034471"))
        assertEquals("886812034471", QrPayload.decode(QrPayload.encode("886812034471")))
    }

    @Test
    fun rejectsNonFluxNumbersAndOtherCodes() {
        assertNull(QrPayload.decode("fluxbank:886712034471"))
        assertNull(QrPayload.decode("https://example.com"))
    }

    @Test
    fun trimsWhitespace() = assertEquals("886812034471", QrPayload.decode(" fluxbank:886812034471\n"))
}
