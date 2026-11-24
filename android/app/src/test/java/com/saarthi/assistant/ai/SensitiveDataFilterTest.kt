package com.saarthi.assistant.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveDataFilterTest {

    @Test
    fun safeGeneralQuestions_areNotFlagged() {
        val safeQueries = listOf(
            "What is the capital of Bangladesh?",
            "Explain quantum computing in simple terms.",
            "How do airplanes stay in the air?",
            "What is photosynthesis?",
            "Why is the sky blue?",
            "Tell me a short poem about rain.",
            "Who wrote Hamlet?",
            "Calculate 25 multiplied by 4."
        )

        for (q in safeQueries) {
            val result = SensitiveDataFilter.inspect(q)
            assertFalse("Expected safe for '$q', got: ${result.reason}", result.isSensitive)
        }
    }

    @Test
    fun apiKeysAndTokens_areFlaggedAsCredentials() {
        val keyQueries = listOf(
            "Here is my API key sk-proj-1234567890abcdef1234567890abcdef",
            "Use ghp_1234567890abcdef1234567890abcdef to push",
            "My token is AIzaSyD1234567890abcdef1234567890abcde",
            "Authorization: Bearer my_secret_token_abcdef123456789"
        )

        for (q in keyQueries) {
            val result = SensitiveDataFilter.inspect(q)
            assertTrue("Expected sensitive for '$q'", result.isSensitive)
            assertEquals(SensitiveCategory.CREDENTIALS, result.category)
        }
    }

    @Test
    fun passwordAndPinDisclosures_areFlaggedAsCredentials() {
        val pwdQueries = listOf(
            "My password is SecretPassword123!",
            "password: SuperSecretPassword",
            "My pin is 4321",
            "password='my_database_password'"
        )

        for (q in pwdQueries) {
            val result = SensitiveDataFilter.inspect(q)
            assertTrue("Expected sensitive for '$q'", result.isSensitive)
            assertEquals(SensitiveCategory.CREDENTIALS, result.category)
        }
    }

    @Test
    fun creditCardNumbers_areFlaggedAsPaymentCards() {
        // Valid Luhn test card numbers
        val validCards = listOf(
            "My card number is 4532015112830366",      // Valid Luhn Visa (16 digits)
            "Charge 4532 0151 1283 0366 for this",     // Valid Luhn Visa with spaces
            "Card: 5412751234123452"                   // Valid Luhn MasterCard
        )

        for (q in validCards) {
            val result = SensitiveDataFilter.inspect(q)
            assertTrue("Expected payment card detected for '$q'", result.isSensitive)
            assertEquals(SensitiveCategory.PAYMENT_CARD, result.category)
        }
    }

    @Test
    fun nonTelephonyNumbers_areNotFlaggedAsSensitive() {
        val safeNumbers = listOf(
            "The population is 170000000 people",
            "Order number 9876543210",
            "There are 365 days in a normal year",
            "Model number is 4096"
        )

        for (q in safeNumbers) {
            val result = SensitiveDataFilter.inspect(q)
            assertFalse("Expected non-sensitive for '$q', but was flagged as: ${result.category}", result.isSensitive)
        }
    }

    @Test
    fun phoneNumbers_areFlaggedAsPhoneOrContact() {
        val phoneQueries = listOf(
            "My phone number is +8801712345678",
            "Call 01711223344",
            "Her contact number is +1 (555) 123-4567",
            "Mobile: 01812345678"
        )

        for (q in phoneQueries) {
            val result = SensitiveDataFilter.inspect(q)
            assertTrue("Expected sensitive phone number detected for '$q'", result.isSensitive)
            assertEquals(SensitiveCategory.PHONE_OR_CONTACT, result.category)
        }
    }

    @Test
    fun telephonyQueriesAndContactRecords_areFlaggedAsPhoneOrContact() {
        val telephonyQueries = listOf(
            "Who called me earlier today?",
            "What was that incoming call?",
            "Show my missed call list",
            "Get contact record for Sarah",
            "কার ফোন এসেছিল?",
            "ke phone diyechilo?",
            "Show recent call log"
        )

        for (q in telephonyQueries) {
            val result = SensitiveDataFilter.inspect(q)
            assertTrue("Expected sensitive telephony query for '$q'", result.isSensitive)
            assertEquals(SensitiveCategory.PHONE_OR_CONTACT, result.category)
        }
    }

    @Test
    fun nationalIdNumbers_areFlaggedAsPersonalIdentifier() {
        val ssnQuery = "My SSN is 123-45-6789"
        val result = SensitiveDataFilter.inspect(ssnQuery)
        assertTrue(result.isSensitive)
        assertEquals(SensitiveCategory.PERSONAL_IDENTIFIER, result.category)
    }
}
