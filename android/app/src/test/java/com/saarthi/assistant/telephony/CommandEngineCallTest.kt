package com.saarthi.assistant.telephony

import com.saarthi.assistant.core.CommandEngine
import com.saarthi.assistant.core.CommandResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommandEngineCallTest {

    private lateinit var engine: CommandEngine

    @Before
    fun setUp() {
        engine = CommandEngine()
    }

    @Test
    fun answerCall_englishPhrases_routeToAnswerCall() {
        val phrases = listOf(
            "answer",
            "answer call",
            "answer the call",
            "receive call",
            "receive the call",
            "pick up",
            "pick up call",
            "pick up the phone",
            "accept call"
        )

        for (phrase in phrases) {
            val result = engine.process(phrase)
            assertEquals("Expected AnswerCall for phrase: '$phrase'", CommandResult.AnswerCall, result)
        }
    }

    @Test
    fun answerCall_banglaPhrases_routeToAnswerCall() {
        val phrases = listOf(
            "কল ধরো",
            "ফোন ধরো",
            "ধরো",
            "কলটা ধরো",
            "ফোনটা ধরো",
            "রিসিভ করো",
            "কল রিসিভ করো",
            "ফোন রিসিভ করো"
        )

        for (phrase in phrases) {
            val result = engine.process(phrase)
            assertEquals("Expected AnswerCall for Bangla phrase: '$phrase'", CommandResult.AnswerCall, result)
        }
    }

    @Test
    fun answerCall_banglishPhrases_routeToAnswerCall() {
        val phrases = listOf(
            "call dhoro",
            "phone dhoro",
            "dhoro",
            "call receive koro",
            "receive koro",
            "phone ta dhoro",
            "call ta dhoro",
            "pick up koro"
        )

        for (phrase in phrases) {
            val result = engine.process(phrase)
            assertEquals("Expected AnswerCall for Banglish phrase: '$phrase'", CommandResult.AnswerCall, result)
        }
    }

    @Test
    fun rejectCall_englishPhrases_routeToRejectCall() {
        val phrases = listOf(
            "reject",
            "reject call",
            "reject the call",
            "decline",
            "decline call",
            "decline the call",
            "ignore",
            "ignore call",
            "hang up",
            "cut call",
            "cut the call"
        )

        for (phrase in phrases) {
            val result = engine.process(phrase)
            assertEquals("Expected RejectCall for phrase: '$phrase'", CommandResult.RejectCall, result)
        }
    }

    @Test
    fun rejectCall_banglaPhrases_routeToRejectCall() {
        val phrases = listOf(
            "কল কাটো",
            "ফোন কাটো",
            "কাটো",
            "কলটা কাটো",
            "ফোনটা কাটো",
            "কেটে দাও",
            "কল কেটে দাও",
            "ফোন কেটে দাও",
            "রিজেক্ট করো",
            "কল রিজেক্ট করো"
        )

        for (phrase in phrases) {
            val result = engine.process(phrase)
            assertEquals("Expected RejectCall for Bangla phrase: '$phrase'", CommandResult.RejectCall, result)
        }
    }

    @Test
    fun rejectCall_banglishPhrases_routeToRejectCall() {
        val phrases = listOf(
            "call kato",
            "phone kato",
            "kato",
            "kete dao",
            "call kete dao",
            "phone kete dao",
            "reject koro",
            "call reject koro",
            "decline koro"
        )

        for (phrase in phrases) {
            val result = engine.process(phrase)
            assertEquals("Expected RejectCall for Banglish phrase: '$phrase'", CommandResult.RejectCall, result)
        }
    }

    @Test
    fun outgoingCallCommands_doNotCollideWithIncomingCallHandling() {
        // "Call Mom" is an outgoing call request and must route to SensitiveConfirmation, not AnswerCall/RejectCall
        val outgoingResult = engine.process("call Mom")
        assertTrue("Expected SensitiveConfirmation for 'call Mom', got $outgoingResult",
            outgoingResult is CommandResult.SensitiveConfirmation)
    }
}

