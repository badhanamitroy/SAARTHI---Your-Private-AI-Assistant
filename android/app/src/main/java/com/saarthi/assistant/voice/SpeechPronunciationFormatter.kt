package com.saarthi.assistant.voice

/**
 * Centralized formatter for spoken assistant responses.
 *
 * Root Cause Analysis:
 * Android Text-to-Speech engines (Google Speech Services, Samsung TTS, AOSP Pico) apply
 * text normalization heuristics where words in all-caps ("SAARTHI") that are not recognized
 * as common dictionary words are treated as acronyms or initialisms. Consequently, the
 * grapheme-to-phoneme (G2P) synthesizer spells them out letter-by-letter: "S-A-A-R-T-H-I".
 *
 * Solution & Contract:
 * 1. Preserves the official uppercase branding "SAARTHI" in all user-visible UI text,
 *    command processing, documentation, and persistent notifications.
 * 2. Transparently formats text immediately prior to TTS audio synthesis by substituting
 *    the assistant's name token with the natural title-case phonetic representation ("Saarthi").
 * 3. Title-cased "Saarthi" instructs Android TTS to pronounce it as a proper noun and single
 *    fluid word (/sɑːr.θiː/), preventing letter spelling.
 * 4. Strictly protects all unrelated words and command content from unintended modification.
 */
object SpeechPronunciationFormatter {

    /** Natural, pronunciation-safe spoken alias passed to Android Text-to-Speech. */
    const val SPOKEN_NAME = "Saarthi"

    // Matches standalone occurrences of SAARTHI across all casing styles
    private val ASSISTANT_NAME_PATTERN = Regex("(?i)\\bsaarthi\\b")

    /**
     * Formats text for Android TextToSpeech synthesis.
     * Replaces occurrences of the assistant name token with the pronunciation-safe spoken alias.
     */
    fun formatForSpeech(text: String): String {
        if (text.isBlank()) return text
        return ASSISTANT_NAME_PATTERN.replace(text) {
            SPOKEN_NAME
        }
    }
}
