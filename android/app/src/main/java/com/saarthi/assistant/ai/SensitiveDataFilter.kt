package com.saarthi.assistant.ai

import java.util.Locale

/**
 * Categories of sensitive data detected locally before cloud transmission.
 */
enum class SensitiveCategory(val displayName: String) {
    PAYMENT_CARD("Financial / Payment Card"),
    CREDENTIALS("API Key / Password / Secret Token"),
    PERSONAL_IDENTIFIER("Private National / Personal Identifier"),
    PHONE_OR_CONTACT("Phone Number / Caller Identity / Contact Record")
}

/**
 * Result of local privacy inspection.
 */
data class SensitiveDataCheckResult(
    val isSensitive: Boolean,
    val category: SensitiveCategory? = null,
    val reason: String? = null
) {
    companion object {
        val SAFE = SensitiveDataCheckResult(isSensitive = false)
    }
}

/**
 * Local privacy guard that inspects prompts for sensitive credentials,
 * payment card numbers, and passwords BEFORE any cloud request is initiated.
 */
object SensitiveDataFilter {

    // Regex for credit card sequences (13 to 19 digits, with optional spaces or hyphens)
    private val CARD_PATTERN = Regex("\\b(?:\\d[ -]?){13,19}\\b")

    // Common API key and credential patterns
    private val API_KEY_PATTERNS = listOf(
        Regex("sk-[A-Za-z0-9_-]{10,}", RegexOption.IGNORE_CASE),
        Regex("ghp_[A-Za-z0-9]{10,}", RegexOption.IGNORE_CASE),
        Regex("AIza[0-9A-Za-z_-]{20,}"),
        Regex("xox[baprs]-[0-9A-Za-z-]{10,}", RegexOption.IGNORE_CASE),
        Regex("(?:api[_-]?key|secret[_-]?token|bearer)\\b", RegexOption.IGNORE_CASE)
    )

    // Password / PIN disclosure patterns
    private val PASSWORD_PATTERNS = listOf(
        Regex("(?:my\\s+)?(?:password|passcode|pin|credential)\\s*(?:is|:|=)\\s*[^\\s,.]+", RegexOption.IGNORE_CASE),
        Regex("(?:password|secret)\\s*=\\s*['\"][^'\"]+['\"]", RegexOption.IGNORE_CASE)
    )

    // National identification number patterns (e.g., US SSN format: 000-00-0000)
    private val ID_PATTERNS = listOf(
        Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b")
    )

    // Phone number patterns (international with +, Bangladesh 11-digit 01X, or standard formatted)
    private val PHONE_PATTERNS = listOf(
        Regex("(?:\\+|00)[0-9]{1,4}[ -]?(?:\\([0-9]{2,4}\\)[ -]?)?[0-9]{3,4}[ -]?[0-9]{3,4}"),
        Regex("\\b01[3-9]\\d{8}\\b"),
        Regex("\\b\\d{3}[-.\\s]\\d{3}[-.\\s]\\d{4}\\b"),
        Regex("(?:phone|mobile|cell|contact)\\s*(?:number|no|#)?\\s*(?:is|:|=)?\\s*\\+?[0-9 -]{7,15}", RegexOption.IGNORE_CASE)
    )

    // Telephony and contact queries (caller identity, call logs, phonebook records)
    private val TELEPHONY_PATTERNS = listOf(
        Regex("\\b(?:call|phone|dial|ring)\\s+(?:\\+?[0-9 -]{7,15})\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:who|whose)\\s+(?:called|is calling|was calling|call was that)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:incoming|outgoing|missed|recent)\\s+call(?:s|er)?\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:caller\\s+id|caller\\s+name|caller\\s+number)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:contact|phonebook|address\\s+book)\\s+(?:record|entry|number|details|list)\\b", RegexOption.IGNORE_CASE),
        Regex("(?:কার\\s+ফোন|কে\\s+ফোন\\s+দিয়েছিল|কল\\s+লগ|ফোন\\s+নম্বর|যোগাযোগ)"),
        Regex("\\b(?:kar\\s+phone|ke\\s+phone\\s+diyechilo|call\\s+log|phone\\s+number|jogajog)\\b", RegexOption.IGNORE_CASE)
    )

    /**
     * Inspects text locally. Returns a [SensitiveDataCheckResult] indicating whether
     * the prompt should be refused or redacted locally to protect user privacy.
     */
    fun inspect(text: String): SensitiveDataCheckResult {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return SensitiveDataCheckResult.SAFE
        }

        // 1. Check for API keys and tokens
        for (pattern in API_KEY_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                return SensitiveDataCheckResult(
                    isSensitive = true,
                    category = SensitiveCategory.CREDENTIALS,
                    reason = "Request appears to contain an API key, access token, or secret credentials."
                )
            }
        }

        // 2. Check for explicit password / PIN disclosures
        for (pattern in PASSWORD_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                return SensitiveDataCheckResult(
                    isSensitive = true,
                    category = SensitiveCategory.CREDENTIALS,
                    reason = "Request appears to contain a password or security PIN."
                )
            }
        }

        // 3. Check for payment cards (regex + optional Luhn algorithm validation)
        val cardMatches = CARD_PATTERN.findAll(trimmed)
        for (match in cardMatches) {
            val digitsOnly = match.value.replace(Regex("[^0-9]"), "")
            if (digitsOnly.length in 12..19 && passesLuhnCheck(digitsOnly)) {
                return SensitiveDataCheckResult(
                    isSensitive = true,
                    category = SensitiveCategory.PAYMENT_CARD,
                    reason = "Request appears to contain a credit or debit card number."
                )
            }
        }

        // 4. Check for personal identifiers
        for (pattern in ID_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                return SensitiveDataCheckResult(
                    isSensitive = true,
                    category = SensitiveCategory.PERSONAL_IDENTIFIER,
                    reason = "Request appears to contain a national identity number."
                )
            }
        }

        // 5. Check for phone numbers
        for (pattern in PHONE_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                return SensitiveDataCheckResult(
                    isSensitive = true,
                    category = SensitiveCategory.PHONE_OR_CONTACT,
                    reason = "Request appears to contain a personal phone number."
                )
            }
        }

        // 6. Check for telephony / contact queries
        for (pattern in TELEPHONY_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                return SensitiveDataCheckResult(
                    isSensitive = true,
                    category = SensitiveCategory.PHONE_OR_CONTACT,
                    reason = "Request appears to query caller identity, contact records, or call logs."
                )
            }
        }

        return SensitiveDataCheckResult.SAFE
    }

    /**
     * Performs standard Luhn algorithm check on numeric digits.
     */
    private fun passesLuhnCheck(digits: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in digits.length - 1 downTo 0) {
            var n = digits[i].digitToIntOrNull() ?: return false
            if (alternate) {
                n *= 2
                if (n > 9) {
                    n -= 9
                }
            }
            sum += n
            alternate = !alternate
        }
        return (sum % 10 == 0)
    }
}
