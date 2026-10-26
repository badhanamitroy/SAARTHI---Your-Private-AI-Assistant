package com.saarthi.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechPronunciationFormatterTest {

    @Test
    fun exactName_transformsToPronunciationSafeAlias() {
        val formatted = SpeechPronunciationFormatter.formatForSpeech("SAARTHI")
        assertEquals("Saarthi", formatted)
    }

    @Test
    fun standardSentences_transformCorrectly() {
        val testCases = mapOf(
            "I am SAARTHI." to "I am Saarthi.",
            "Hey, I'm SAARTHI, your personal assistant." to "Hey, I'm Saarthi, your personal assistant.",
            "Hello! I am SAARTHI. How can I help you?" to "Hello! I am Saarthi. How can I help you?",
            "My name is SAARTHI, your private AI assistant." to "My name is Saarthi, your private AI assistant.",
            "Here is the result from SAARTHI:" to "Here is the result from Saarthi:"
        )

        for ((input, expected) in testCases) {
            val result = SpeechPronunciationFormatter.formatForSpeech(input)
            assertEquals("Failed for sentence: $input", expected, result)
        }
    }

    @Test
    fun mixedCaseOccurrences_transformToPronunciationSafeAlias() {
        val cases = listOf(
            "SAARTHI",
            "saarthi",
            "Saarthi",
            "sAaRtHi",
            "SaArThI",
            "SAARThi"
        )

        for (variant in cases) {
            val result = SpeechPronunciationFormatter.formatForSpeech("Hello $variant!")
            assertEquals("Hello Saarthi!", result)
        }
    }

    @Test
    fun punctuationAndBoundaries_handledCorrectly() {
        assertEquals("Hello, Saarthi!", SpeechPronunciationFormatter.formatForSpeech("Hello, SAARTHI!"))
        assertEquals("Who is Saarthi?", SpeechPronunciationFormatter.formatForSpeech("Who is SAARTHI?"))
        assertEquals("Saarthi's response", SpeechPronunciationFormatter.formatForSpeech("SAARTHI's response"))
        assertEquals("(Saarthi)", SpeechPronunciationFormatter.formatForSpeech("(SAARTHI)"))
        assertEquals("\"Saarthi\"", SpeechPronunciationFormatter.formatForSpeech("\"SAARTHI\""))
        assertEquals("Saarthi, open calculator.", SpeechPronunciationFormatter.formatForSpeech("SAARTHI, open calculator."))
    }

    @Test
    fun unrelatedWords_areStrictlyPreserved() {
        val unrelatedSentences = listOf(
            "The earth rotates around the sun.",
            "Please restart the system.",
            "A smart assistant evaluates expressions safely.",
            "Subatomic particle detection.",
            "Listen to your heart.",
            "What is 25 plus 17?",
            "I sent the request to open Calculator.",
            "Today is Sunday, October 25, 2026."
        )

        for (sentence in unrelatedSentences) {
            val result = SpeechPronunciationFormatter.formatForSpeech(sentence)
            assertEquals("Unrelated sentence must not be modified: $sentence", sentence, result)
        }
    }

    @Test
    fun multipleOccurrences_allTransformed() {
        val input = "Hello SAARTHI, please tell me what SAARTHI can do."
        val expected = "Hello Saarthi, please tell me what Saarthi can do."
        assertEquals(expected, SpeechPronunciationFormatter.formatForSpeech(input))
    }

    @Test
    fun emptyAndBlankStrings_returnUnchanged() {
        assertEquals("", SpeechPronunciationFormatter.formatForSpeech(""))
        assertEquals("   ", SpeechPronunciationFormatter.formatForSpeech("   "))
    }
}
