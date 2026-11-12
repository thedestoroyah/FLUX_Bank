package com.flux.bank.domain.assistant

import com.flux.bank.domain.assistant.AssistantAction.BILL_WATER
import com.flux.bank.domain.assistant.AssistantAction.BRANCH
import com.flux.bank.domain.assistant.AssistantAction.FLIGHT
import com.flux.bank.domain.assistant.AssistantAction.MOVIE
import com.flux.bank.domain.assistant.AssistantAction.SHOP
import com.flux.bank.domain.assistant.AssistantAction.TOPUP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantReplyParserTest {

    @Test
    fun parsesReplyAndActions() {
        val reply = AssistantReplyParser.parse("""{"reply":"Chạm bên dưới để nạp nhé.","actions":["TOPUP"]}""")
        assertEquals(AssistantReply("Chạm bên dưới để nạp nhé.", listOf(TOPUP)), reply)
    }

    @Test
    fun dropsUnknownAndDuplicateActions() {
        val reply = AssistantReplyParser.parse("""{"reply":"Ok","actions":["TOPUP","LOAN_APPLY","TOPUP"]}""")
        assertEquals(listOf(TOPUP), reply.actions)
    }

    @Test
    fun acceptsLowercaseAndSpaces() {
        val reply = AssistantReplyParser.parse("""{"reply":"Ok","actions":[" bill_water "]}""")
        assertEquals(listOf(BILL_WATER), reply.actions)
    }

    @Test
    fun capsAtThree() {
        val reply = AssistantReplyParser.parse("""{"reply":"Ok","actions":["FLIGHT","MOVIE","SHOP","BRANCH","TOPUP"]}""")
        assertEquals(listOf(FLIGHT, MOVIE, SHOP), reply.actions)
        assertTrue(BRANCH !in reply.actions)
    }

    @Test
    fun stripsCodeFence() {
        val reply = AssistantReplyParser.parse("```json\n{\"reply\":\"Ok\",\"actions\":[\"TOPUP\"]}\n```")
        assertEquals(AssistantReply("Ok", listOf(TOPUP)), reply)
    }

    @Test
    fun missingActionsMeansNone() {
        assertEquals(AssistantReply("Chào bạn", emptyList()), AssistantReplyParser.parse("""{"reply":"Chào bạn"}"""))
    }

    @Test
    fun blankReplyThrows() {
        assertThrows(IllegalArgumentException::class.java) { AssistantReplyParser.parse("""{"reply":"  ","actions":[]}""") }
    }

    @Test
    fun notJsonThrows() {
        assertThrows(IllegalArgumentException::class.java) { AssistantReplyParser.parse("xin chào") }
    }
}
