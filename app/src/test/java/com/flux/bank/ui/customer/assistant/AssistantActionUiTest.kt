package com.flux.bank.ui.customer.assistant

import com.flux.bank.domain.assistant.AssistantAction
import com.flux.bank.ui.customer.Utility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AssistantActionUiTest {

    @Test
    fun everyActionHasADestination() {
        val ownScreens = setOf(AssistantAction.TRANSFER, AssistantAction.MONTH_HISTORY)
        AssistantAction.entries.forEach { action ->
            val utility = AssistantActionUi.utilityFor(action)
            if (action in ownScreens) assertNull(action.name, utility)
            else {
                assertNotNull(action.name, utility)
                assertNotEquals(action.name, Utility.TRO_LY_AI, utility)
            }
        }
    }

    @Test
    fun topupOpensPhoneTopUp() {
        assertEquals(Utility.NAP_DT, AssistantActionUi.utilityFor(AssistantAction.TOPUP))
        assertEquals(Utility.TIEN_DIEN, AssistantActionUi.utilityFor(AssistantAction.BILL_ELECTRICITY))
    }

    @Test
    fun everyActionHasLabelAndIcon() {
        AssistantAction.entries.forEach { action ->
            val visual = AssistantActionUi.visualFor(action)
            assertNotEquals(action.name, 0, visual.label)
            assertNotEquals(action.name, 0, visual.icon)
        }
    }
}
