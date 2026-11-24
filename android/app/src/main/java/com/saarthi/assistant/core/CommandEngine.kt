package com.saarthi.assistant.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommandEngine {

    fun process(command: String, conversationContext: ConversationContext? = null): CommandResult {
        val rawInput = command.trim()
        if (rawInput.isBlank()) {
            return CommandResult.Reply("I didn't hear anything. Please type or say a command.")
        }

        // Step 1: Normalize recognized speech (including Bengali numerals and number words)
        val normalized = normalizeInput(rawInput)

        // Step 2: Resolve a pending follow-up question, if one exists
        if (conversationContext != null && conversationContext.hasActiveFollowUp()) {
            val followUpResult = resolveFollowUp(rawInput, normalized, conversationContext)
            if (followUpResult != null) {
                return followUpResult
            }
        }

        // Step 3: Check for ambiguous single-word / phrase prompts that need clarification
        val ambiguityClarification = checkAmbiguity(normalized)
        if (ambiguityClarification != null) {
            return CommandResult.Clarification(ambiguityClarification)
        }

        // Step 4: Check for compound intents (e.g. "what time is it and what is today's date")
        val compoundResult = processCompoundIntents(normalized)
        if (compoundResult != null) {
            return compoundResult
        }

        return evaluateSingleIntent(normalized, rawInput)
    }

    private fun resolveFollowUp(
        rawInput: String,
        normalized: String,
        context: ConversationContext
    ): CommandResult? {
        when (val pending = context.pendingIntent) {
            is PendingIntent.TimerDuration -> {
                // Cancellation check
                if (isCancellation(normalized)) {
                    context.clear()
                    return CommandResult.CancelTimer
                }

                // Check for duration parameter
                val durationSeconds = extractTimerSeconds(normalized)
                if (durationSeconds != null && durationSeconds > 0L) {
                    context.clear()
                    if (durationSeconds > TimerController.MAX_TIMER_SECONDS) {
                        return CommandResult.Reply("Timer duration cannot exceed 24 hours.")
                    }
                    return CommandResult.SetTimer(durationSeconds, rawInput)
                }

                // Check if user issued an unrelated known command
                if (isUnrelatedKnownCommand(normalized)) {
                    context.clear()
                    return null // Falls through to standard command execution
                }

                // Unrecognized response for timer duration
                return CommandResult.Clarification("Please specify how long, like 5 minutes, or say cancel to abort.")
            }

            is PendingIntent.Confirmation -> {
                if (isAffirmativeConfirmation(normalized)) {
                    context.clear()
                    return CommandResult.Reply("Action confirmed.")
                }
                if (isNegativeConfirmation(normalized) || isCancellation(normalized)) {
                    context.clear()
                    return CommandResult.Reply("Action cancelled.")
                }
                if (isUnrelatedKnownCommand(normalized)) {
                    context.clear()
                    return null
                }
                return CommandResult.Clarification("Please say yes to confirm or cancel to abort.")
            }

            is PendingIntent.None -> return null
        }
    }

    private fun isCancellation(text: String): Boolean {
        val norm = text.trim().lowercase(Locale.ROOT)
        return norm in listOf(
            "cancel", "stop", "nevermind", "abort", "dismiss",
            "বাদ দাও", "থাক", "দরকার নেই", "বন্ধ করো", "ক্যান্সেল", "বাতিল",
            "bad dao", "thak", "dorkar nei", "cancel koro", "lagbe na"
        )
    }

    private fun isUnrelatedKnownCommand(text: String): Boolean {
        return isAnswerCallCommand(text) ||
               isRejectCallCommand(text) ||
               isBatteryCommand(text) ||
               isFlashlightOnCommand(text) ||
               isFlashlightOffCommand(text) ||
               isWifiSettingsCommand(text) ||
               isWifiToggleCommand(text) ||
               isBluetoothSettingsCommand(text) ||
               isBluetoothToggleCommand(text) ||
               isMobileDataSettingsCommand(text) ||
               isMobileDataToggleCommand(text) ||
               isSoundSettingsCommand(text) ||
               isBrightnessSettingsCommand(text) ||
               isOpenCalculatorCommand(text) ||
               isOpenOtherAppCommand(text) ||
               isTimeCommand(text) ||
               isDateCommand(text) ||
               isGreetingCommand(text) ||
               isIdentityCommand(text) ||
               isHelpCommand(text) ||
               isCalculatorCommand(text)
    }

    private fun normalizeInput(input: String): String {
        var text = input
            .trim()
            .lowercase(Locale.ROOT)
            .trimEnd('.', '?', '!', ',', ';', ':', '"', '\'', '।')

        // Normalize hyphenated wi-fi and spaced wi fi to wifi
        text = text.replace("wi-fi", "wifi")
            .replace("wi fi", "wifi")
            .replace("ওয়াই-ফাই", "ওয়াইফাই")

        // Normalize Bengali numerals to digits
        text = text.map { ch ->
            when (ch) {
                '০' -> '0'; '১' -> '1'; '২' -> '2'; '৩' -> '3'; '৪' -> '4'
                '৫' -> '5'; '৬' -> '6'; '৭' -> '7'; '৮' -> '8'; '৯' -> '9'
                else -> ch
            }
        }.joinToString("")

        // Normalize number words to digits (Bengali, English, Banglish)
        text = normalizeNumberWords(text)

        // Strip wake-phrase prefixes if followed by a command
        val wakePrefixes = listOf(
            "hey saarthi", "ok saarthi", "okay saarthi", "namaste saarthi", "saarthi"
        )
        for (wp in wakePrefixes) {
            if (text == wp) {
                // Wake word alone triggers greeting / identity recognition
                break
            }
            if (text.startsWith("$wp, ") || text.startsWith("$wp: ") || text.startsWith("$wp ")) {
                val remainder = text.removePrefix(wp).trimStart(' ', ',', ':', '-').trim()
                if (remainder.isNotBlank()) {
                    text = remainder
                    break
                }
            }
        }

        return text
    }

    private fun normalizeNumberWords(input: String): String {
        var text = input

        // Bengali number words
        val banglaWords = listOf(
            "এক" to "1", "দুই" to "2", "তিন" to "3", "চার" to "4", "পাঁচ" to "5",
            "ছয়" to "6", "ছয়" to "6", "সাত" to "7", "আট" to "8", "নয়" to "9", "নয়" to "9",
            "দশ" to "10", "এগারো" to "11", "বারো" to "12", "তেরো" to "13", "চৌদ্দ" to "14",
            "পনেরো" to "15", "পনের" to "15", "বিশ" to "20", "কুড়ি" to "20", "কুড়ি" to "20",
            "পঁচিশ" to "25", "ত্রিশ" to "30", "চল্লিশ" to "40", "পঞ্চাশ" to "50", "ষাট" to "60"
        )
        for ((word, digit) in banglaWords) {
            text = text.replace(Regex("(?<=^|\\s)$word(?=$|\\s|[.,!?])"), digit)
        }

        // English number words
        val englishWords = listOf(
            "one" to "1", "two" to "2", "three" to "3", "four" to "4", "five" to "5",
            "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10",
            "eleven" to "11", "twelve" to "12", "thirteen" to "13", "fourteen" to "14",
            "fifteen" to "15", "twenty" to "20", "twenty-five" to "25", "twenty five" to "25",
            "thirty" to "30", "forty" to "40", "forty-five" to "45", "fifty" to "50", "sixty" to "60"
        )
        for ((word, digit) in englishWords) {
            text = text.replace(Regex("\\b$word\\b"), digit)
        }

        // Banglish number words
        val banglishWords = listOf(
            "ek" to "1", "dui" to "2", "tin" to "3", "char" to "4", "panch" to "5", "pach" to "5",
            "choy" to "6", "saat" to "7", "aat" to "8", "noy" to "9", "dosh" to "10",
            "ponero" to "15", "bish" to "20", "trish" to "30"
        )
        for ((word, digit) in banglishWords) {
            text = text.replace(Regex("\\b$word\\b"), digit)
        }

        return text
    }

    private fun checkAmbiguity(input: String): String? {
        val stripped = input.replace(Regex("[^a-z0-9 ]"), " ").trim()

        if (stripped == "date or time" || stripped == "time or date" || stripped == "time and date or which") {
            return "Did you want to check the current date or the current time?"
        }

        if (stripped == "calculator" || stripped == "calculator app" || stripped == "calc") {
            return "Did you want to open the calculator app, or calculate an arithmetic expression?"
        }

        if (stripped == "clock" || stripped == "clock app" || stripped == "alarm" || stripped == "alarms" || stripped == "time or clock") {
            return "Did you want to check the current time, or open the clock app?"
        }

        if (stripped == "calendar" || stripped == "calendar app" || stripped == "date or calendar") {
            return "Did you want to check today's date, or open the calendar app?"
        }

        if (stripped == "camera" || stripped == "camera app") {
            return "Did you want to open the camera app?"
        }

        if (stripped == "settings" || stripped == "setting" || stripped == "settings app" || stripped == "phone settings") {
            return "Did you want to open device settings?"
        }

        if (stripped == "calculate" || stripped == "solve") {
            return "What arithmetic expression would you like me to calculate?"
        }

        if (stripped == "math" || stripped == "do math") {
            return "What math problem would you like me to solve?"
        }

        if (stripped == "stop speaking or listening" || stripped == "stop speaking or stop listening") {
            return "Would you like me to stop speaking or stop listening?"
        }

        if (stripped == "flashlight" || stripped == "torch" || stripped == "flash light" ||
            input == "ফ্ল্যাশলাইট" || input == "টর্চ") {
            return "Did you want to turn on or turn off the flashlight?"
        }

        // Timer ambiguity without duration
        if (stripped in listOf("timer", "set timer", "start timer", "create timer", "new timer", "timer lagao", "timer dao") ||
            input in listOf("টাইমার", "টাইমার দাও", "টাইমার সেট করো", "টাইমার লাগাও")) {
            return "How long would you like to set the timer for? For example: '2 minutes'."
        }

        if (stripped == "wifi" || stripped == "wi fi" || input == "ওয়াইফাই") {
            return "Did you want to open Wi-Fi settings?"
        }

        if (stripped == "bluetooth" || input == "ব্লুটুথ") {
            return "Did you want to open Bluetooth settings?"
        }

        if (stripped == "volume" || stripped == "sound" || input == "ভলিউম" || input == "সাউন্ড") {
            return "Did you want to open volume and sound settings?"
        }

        if (stripped == "brightness" || stripped == "display" || input == "ব্রাইটনেস" || input == "ডিসপ্লে") {
            return "Did you want to open display and brightness settings?"
        }

        return null
    }

    private fun processCompoundIntents(input: String): CommandResult? {
        val conjunctionRegex = Regex("\\s+(?:and|also)\\s+")
        val parts = input.split(conjunctionRegex).map { it.trim() }.filter { it.isNotBlank() }

        if (parts.size <= 1) {
            return null
        }

        val replies = mutableListOf<String>()

        for (part in parts) {
            val partResult = evaluateSingleIntent(part)
            when (partResult) {
                is CommandResult.Reply -> {
                    // If an arithmetic evaluation error occurred in a part, fail compound intent
                    if (partResult.text.startsWith("Division by zero") ||
                        partResult.text.contains("expression") ||
                        partResult.text.contains("syntax")) {
                        return null
                    }
                    replies.add(partResult.text)
                }
                is CommandResult.Clarification -> return partResult
                is CommandResult.StopSpeaking -> return CommandResult.StopSpeaking
                is CommandResult.StopListening -> return CommandResult.StopListening
                is CommandResult.OpenCalculator -> return CommandResult.OpenCalculator
                is CommandResult.OpenApp -> return partResult
                is CommandResult.SensitiveConfirmation -> return partResult
                is CommandResult.GetBatteryStatus -> return partResult
                is CommandResult.SetFlashlight -> return partResult
                is CommandResult.OpenSystemSettings -> return partResult
                is CommandResult.SetTimer -> return partResult
                is CommandResult.PauseTimer -> return partResult
                is CommandResult.ResumeTimer -> return partResult
                is CommandResult.CancelTimer -> return partResult
                is CommandResult.GetTimerStatus -> return partResult
                is CommandResult.AnswerCall -> return partResult
                is CommandResult.RejectCall -> return partResult
                is CommandResult.WakeWordAcknowledged -> return partResult
                is CommandResult.Ignore -> return null
                is CommandResult.NeedsOnlineAI -> {
                    // Any unresolvable segment means this is not a valid compound offline intent
                    return null
                }
            }
        }

        if (replies.size == parts.size) {
            return CommandResult.Reply(replies.joinToString(" "))
        }

        return null
    }

    private fun evaluateSingleIntent(cleaned: String, rawInput: String = cleaned): CommandResult {
        // Direct Settings Toggle Requests (must execute panel launch with truthful feedback)
        if (isWifiToggleCommand(cleaned)) {
            return CommandResult.OpenSystemSettings(SystemSettingType.WIFI, isDirectToggleAttempt = true)
        }
        if (isBluetoothToggleCommand(cleaned)) {
            return CommandResult.OpenSystemSettings(SystemSettingType.BLUETOOTH, isDirectToggleAttempt = true)
        }
        if (isMobileDataToggleCommand(cleaned)) {
            return CommandResult.OpenSystemSettings(SystemSettingType.MOBILE_DATA, isDirectToggleAttempt = true)
        }

        val intent = detectIntent(cleaned)

        return when (intent) {
            IntentType.GREETING -> {
                CommandResult.Reply("Hello! I am SAARTHI. How can I help you?")
            }

            IntentType.GET_ASSISTANT_NAME -> {
                CommandResult.Reply("My name is SAARTHI, your private AI assistant.")
            }

            IntentType.GET_CURRENT_TIME -> {
                val time = SimpleDateFormat(
                    "hh:mm a",
                    Locale.getDefault()
                ).format(Date())

                CommandResult.Reply("The current time is $time.")
            }

            IntentType.GET_CURRENT_DATE -> {
                val date = SimpleDateFormat(
                    "EEEE, MMMM d, yyyy",
                    Locale.getDefault()
                ).format(Date())

                CommandResult.Reply("Today is $date.")
            }

            IntentType.OPEN_CALCULATOR -> {
                CommandResult.OpenCalculator
            }

            IntentType.OPEN_APP -> {
                evaluateAppLaunch(cleaned)
            }

            IntentType.HELP -> {
                evaluateHelp(cleaned)
            }

            IntentType.CALCULATOR -> {
                evaluateCalculator(cleaned)
            }

            IntentType.STOP_SPEAKING -> {
                CommandResult.StopSpeaking
            }

            IntentType.STOP_LISTENING -> {
                CommandResult.StopListening
            }

            IntentType.AMBIGUOUS -> {
                CommandResult.Clarification("I am not sure what you mean. Could you please clarify?")
            }

            IntentType.SENSITIVE_CALL -> {
                val recipient = extractCallRecipient(cleaned)
                CommandResult.SensitiveConfirmation(
                    action = SensitiveAction.PlacePhoneCall(recipient),
                    confirmationPrompt = "You asked to call $recipient. Are you sure you want to place this call? Say yes to confirm or cancel to abort."
                )
            }

            IntentType.SENSITIVE_CLEAR -> {
                CommandResult.SensitiveConfirmation(
                    action = SensitiveAction.ClearData("history"),
                    confirmationPrompt = "Are you sure you want to clear your data? Say yes to confirm or cancel to abort."
                )
            }

            IntentType.CONFIRMATION_AFFIRMATIVE -> {
                CommandResult.Reply("Action confirmed.")
            }

            IntentType.CONFIRMATION_NEGATIVE -> {
                CommandResult.Reply("Action cancelled.")
            }

            IntentType.WAKE_WORD_ONLY -> {
                CommandResult.WakeWordAcknowledged("I'm listening.")
            }

            IntentType.BATTERY_STATUS -> {
                CommandResult.GetBatteryStatus
            }

            IntentType.FLASHLIGHT_ON -> {
                CommandResult.SetFlashlight(enabled = true)
            }

            IntentType.FLASHLIGHT_OFF -> {
                CommandResult.SetFlashlight(enabled = false)
            }

            IntentType.SYSTEM_SETTINGS_WIFI -> {
                CommandResult.OpenSystemSettings(SystemSettingType.WIFI)
            }

            IntentType.SYSTEM_SETTINGS_BLUETOOTH -> {
                CommandResult.OpenSystemSettings(SystemSettingType.BLUETOOTH)
            }

            IntentType.SYSTEM_SETTINGS_MOBILE_DATA -> {
                CommandResult.OpenSystemSettings(SystemSettingType.MOBILE_DATA)
            }

            IntentType.SYSTEM_SETTINGS_SOUND -> {
                CommandResult.OpenSystemSettings(SystemSettingType.SOUND)
            }

            IntentType.SYSTEM_SETTINGS_BRIGHTNESS -> {
                CommandResult.OpenSystemSettings(SystemSettingType.BRIGHTNESS)
            }

            IntentType.TIMER_SET -> {
                evaluateTimerSet(cleaned)
            }

            IntentType.TIMER_PAUSE -> {
                CommandResult.PauseTimer
            }

            IntentType.TIMER_RESUME -> {
                CommandResult.ResumeTimer
            }

            IntentType.TIMER_CANCEL -> {
                CommandResult.CancelTimer
            }

            IntentType.TIMER_STATUS -> {
                CommandResult.GetTimerStatus
            }

            IntentType.CALL_ANSWER -> {
                CommandResult.AnswerCall
            }

            IntentType.CALL_REJECT -> {
                CommandResult.RejectCall
            }

            IntentType.UNKNOWN -> {
                CommandResult.NeedsOnlineAI
            }
        }
    }

    private fun evaluateCalculator(input: String): CommandResult {
        var expr = input

        // Strip command prefixes
        val prefixes = listOf(
            "calculate", "compute", "solve", "what is", "what's", "tell me what is", "how much is"
        )
        for (prefix in prefixes) {
            if (expr.startsWith(prefix)) {
                expr = expr.removePrefix(prefix).trim()
                break
            }
        }

        if (expr.isBlank()) {
            return CommandResult.Clarification("What arithmetic expression would you like me to calculate?")
        }

        return when (val mathResult = SafeMathParser.evaluate(expr)) {
            is MathEvaluationResult.Success -> {
                CommandResult.Reply("The answer is ${mathResult.formatted}.")
            }
            is MathEvaluationResult.Error -> {
                CommandResult.Reply(mathResult.message)
            }
        }
    }

    private fun evaluateHelp(input: String): CommandResult {
        val topic = extractHelpTopic(input)
        if (topic != null) {
            val specific = HelpRegistry.getTopicHelp(topic)
            if (specific != null) {
                return CommandResult.Reply(specific)
            }
        }
        return CommandResult.Reply(HelpRegistry.getOverviewSpoken())
    }

    private fun extractHelpTopic(input: String): String? {
        val lower = input.lowercase().trim()
        val generalHelpQueries = listOf(
            "help", "help me", "commands", "what can you do", "supported commands",
            "show commands", "how to use", "options", "sahajjo", "shahajjo", "সাহায্য"
        )
        if (generalHelpQueries.contains(lower)) return null

        val prefixes = listOf("help with ", "help on ", "help for ", "help ", "commands for ")
        for (p in prefixes) {
            if (lower.startsWith(p)) {
                val candidate = lower.removePrefix(p).trim()
                if (candidate in listOf("me", "please", "koro", "kor", "")) return null
                return candidate
            }
        }
        val suffixes = listOf(" help", " er help", " commands")
        for (s in suffixes) {
            if (lower.endsWith(s)) {
                val candidate = lower.removeSuffix(s).trim()
                if (candidate.isNotBlank() && candidate !in listOf("supported", "show", "all")) return candidate
            }
        }
        return null
    }

    private fun evaluateTimerSet(input: String): CommandResult {
        val duration = extractTimerSeconds(input)
        if (duration == null) {
            return CommandResult.Clarification("How long would you like to set the timer for? For example: '2 minutes'.")
        }
        if (duration <= 0L) {
            return CommandResult.Reply("Timer duration must be greater than zero.")
        }
        if (duration > TimerController.MAX_TIMER_SECONDS) {
            return CommandResult.Reply("Timer duration cannot exceed 24 hours.")
        }
        return CommandResult.SetTimer(duration, input)
    }

    private fun extractTimerSeconds(input: String): Long? {
        val normalizedNumbers = normalizeNumberWords(input)
        val text = normalizedNumbers.map { ch ->
            when (ch) {
                '০' -> '0'; '১' -> '1'; '২' -> '2'; '৩' -> '3'; '৪' -> '4'
                '৫' -> '5'; '৬' -> '6'; '৭' -> '7'; '৮' -> '8'; '৯' -> '9'
                else -> ch
            }
        }.joinToString("").lowercase()

        var totalSeconds = 0L
        var foundAny = false

        // Match hours
        val hourRegex = Regex("(\\d+)\\s*(?:hours?|hrs?|ghonta|ঘণ্টা|ঘন্টা)")
        hourRegex.find(text)?.let {
            val hours = it.groupValues[1].toLongOrNull() ?: 0L
            totalSeconds += hours * 3600
            foundAny = true
        }

        // Match minutes
        val minRegex = Regex("(\\d+)\\s*(?:minutes?|mins?|minute|মিনিট|min)")
        minRegex.find(text)?.let {
            val mins = it.groupValues[1].toLongOrNull() ?: 0L
            totalSeconds += mins * 60
            foundAny = true
        }

        // Match seconds
        val secRegex = Regex("(\\d+)\\s*(?:seconds?|secs?|second|সেকেন্ড|sec)")
        secRegex.find(text)?.let {
            val secs = it.groupValues[1].toLongOrNull() ?: 0L
            totalSeconds += secs
            foundAny = true
        }

        // Special word forms: "half an hour" -> 30 mins
        if (!foundAny && (text.contains("half an hour") || text.contains("half hour") ||
            text.contains("adha ghonta") || text.contains("আধা ঘণ্টা") || text.contains("আধা ঘন্টা"))) {
            totalSeconds = 1800L
            foundAny = true
        }

        if (!foundAny) {
            // Check for pattern like: "2 er timer" or "10 timer"
            val fallbackRegex = Regex("(\\d+)\\s*(?:er\\s*)?timer")
            val fallbackMatch = fallbackRegex.find(text)
            if (fallbackMatch != null) {
                val mins = fallbackMatch.groupValues[1].toLongOrNull() ?: 0L
                totalSeconds = mins * 60
                foundAny = true
            }
        }

        if (!foundAny) {
            // Standalone digit like "5" or "10" (common short reply to follow-up)
            val standaloneRegex = Regex("^(\\d+)$")
            standaloneRegex.find(text.trim())?.let {
                val mins = it.groupValues[1].toLongOrNull() ?: 0L
                if (mins > 0) {
                    totalSeconds = mins * 60
                    foundAny = true
                }
            }
        }

        return if (foundAny) totalSeconds else null
    }

    private fun detectIntent(input: String): IntentType {
        val cleaned = input.trimEnd('.', '?', '!', ',', ';', ':', '"', '\'', '।')

        return when {
            // Stop listening (check before general stop)
            isStopListeningCommand(cleaned) -> {
                IntentType.STOP_LISTENING
            }

            // Stop speaking
            isStopSpeakingCommand(cleaned) -> {
                IntentType.STOP_SPEAKING
            }

            // Voice Call Answering & Rejection (takes priority over general outgoing calls)
            isAnswerCallCommand(cleaned) -> {
                IntentType.CALL_ANSWER
            }

            isRejectCallCommand(cleaned) -> {
                IntentType.CALL_REJECT
            }

            // Sensitive actions requiring explicit confirmation (e.g. calls, data wipe)
            isCallCommand(cleaned) -> {
                IntentType.SENSITIVE_CALL
            }

            isClearDataCommand(cleaned) -> {
                IntentType.SENSITIVE_CLEAR
            }

            // Explicit confirmation affirmations/negatives
            isAffirmativeConfirmation(cleaned) -> {
                IntentType.CONFIRMATION_AFFIRMATIVE
            }

            isNegativeConfirmation(cleaned) -> {
                IntentType.CONFIRMATION_NEGATIVE
            }

            // Help & Command Discovery (check before specific device actions so 'help torch', 'help timer' route to Help)
            isHelpCommand(cleaned) -> {
                IntentType.HELP
            }

            // Flashlight ON/OFF (explicit actions before general app launch or calculations)
            isFlashlightOnCommand(cleaned) -> {
                IntentType.FLASHLIGHT_ON
            }

            isFlashlightOffCommand(cleaned) -> {
                IntentType.FLASHLIGHT_OFF
            }

            // Battery Status & Charging
            isBatteryCommand(cleaned) -> {
                IntentType.BATTERY_STATUS
            }

            // Specific System Settings Panels (check before generic settings app launch)
            isWifiToggleCommand(cleaned) || isWifiSettingsCommand(cleaned) -> {
                IntentType.SYSTEM_SETTINGS_WIFI
            }

            isBluetoothToggleCommand(cleaned) || isBluetoothSettingsCommand(cleaned) -> {
                IntentType.SYSTEM_SETTINGS_BLUETOOTH
            }

            isMobileDataToggleCommand(cleaned) || isMobileDataSettingsCommand(cleaned) -> {
                IntentType.SYSTEM_SETTINGS_MOBILE_DATA
            }

            isSoundSettingsCommand(cleaned) -> {
                IntentType.SYSTEM_SETTINGS_SOUND
            }

            isBrightnessSettingsCommand(cleaned) -> {
                IntentType.SYSTEM_SETTINGS_BRIGHTNESS
            }

            // Local Timer controls
            isTimerPauseCommand(cleaned) -> {
                IntentType.TIMER_PAUSE
            }

            isTimerResumeCommand(cleaned) -> {
                IntentType.TIMER_RESUME
            }

            isTimerCancelCommand(cleaned) -> {
                IntentType.TIMER_CANCEL
            }

            isTimerStatusCommand(cleaned) -> {
                IntentType.TIMER_STATUS
            }

            isTimerSetCommand(cleaned) -> {
                IntentType.TIMER_SET
            }

            // Open Calculator App (check BEFORE arithmetic evaluation)
            isOpenCalculatorCommand(cleaned) -> {
                IntentType.OPEN_CALCULATOR
            }

            // Open Other Native Apps (Clock, Calendar, Camera, Settings)
            isOpenOtherAppCommand(cleaned) -> {
                IntentType.OPEN_APP
            }

            // Greeting
            isGreetingCommand(cleaned) -> {
                IntentType.GREETING
            }

            // Identity
            isIdentityCommand(cleaned) -> {
                IntentType.GET_ASSISTANT_NAME
            }

            // Date
            isDateCommand(cleaned) -> {
                IntentType.GET_CURRENT_DATE
            }

            // Time
            isTimeCommand(cleaned) -> {
                IntentType.GET_CURRENT_TIME
            }

            // Calculator Arithmetic
            isCalculatorCommand(cleaned) -> {
                IntentType.CALCULATOR
            }

            else -> {
                IntentType.UNKNOWN
            }
        }
    }

    private fun isOpenCalculatorCommand(text: String): Boolean {
        val normalized = text.trim()

        val englishMatches = listOf(
            "open calculator", "open the calculator", "open my calculator",
            "launch calculator", "launch the calculator", "launch my calculator",
            "start calculator", "start the calculator",
            "show calculator", "show me calculator", "show me the calculator",
            "open calculator app", "open the calculator app", "open my calculator app",
            "launch calculator app", "launch the calculator app", "launch my calculator app",
            "start calculator app", "start the calculator app",
            "open calc", "open phone calculator", "open native calculator"
        )
        if (englishMatches.contains(normalized)) return true

        val hinglishMatches = listOf(
            "calculator kholo", "calculator kholna", "calculator khol do",
            "calculator open karo", "calculator open kijiye", "calculator open kar",
            "calculator chalu karo", "calculator chalao", "calculator start karo",
            "kholo calculator", "open karo calculator", "chalu karo calculator",
            "calculator app kholo", "calculator app open karo", "calculator app open kijiye",
            "calculator app chalu karo", "calculator app khol do", "calc kholo", "calc open karo"
        )
        if (hinglishMatches.contains(normalized)) return true

        val banglaMatches = listOf(
            "ক্যালকুলেটর খোলো", "ক্যালকুলেটর চালু করো", "ক্যালকুলেটর ওপেন করো", "ক্যালকুলেটর"
        )
        if (banglaMatches.contains(normalized)) return true

        val patterns = listOf(
            Regex("^(please )?(open|launch|start|show)( the| my)? (phone |native )?calculator( app)?( please)?$"),
            Regex("^(please )?calculator( app)? (kholo|kholna|khol do|open karo|open kijiye|open kar|chalu karo|start karo)( please)?$"),
            Regex("^(kholo|open karo|chalu karo) (calculator|calc)( app)?$")
        )
        return patterns.any { it.matches(normalized) }
    }

    private fun isOpenOtherAppCommand(text: String): Boolean {
        return isOpenClockCommand(text) ||
                isOpenCalendarCommand(text) ||
                isOpenCameraCommand(text) ||
                isOpenSettingsCommand(text)
    }

    private fun evaluateAppLaunch(text: String): CommandResult {
        return when {
            isOpenClockCommand(text) -> CommandResult.OpenApp(TargetApp.CLOCK)
            isOpenCalendarCommand(text) -> CommandResult.OpenApp(TargetApp.CALENDAR)
            isOpenCameraCommand(text) -> CommandResult.OpenApp(TargetApp.CAMERA)
            isOpenSettingsCommand(text) -> CommandResult.OpenApp(TargetApp.SETTINGS)
            else -> CommandResult.NeedsOnlineAI
        }
    }

    private fun isOpenClockCommand(text: String): Boolean {
        val normalized = text.trim()

        val englishMatches = listOf(
            "open clock", "open the clock", "open my clock",
            "launch clock", "launch the clock", "launch my clock",
            "start clock", "start the clock",
            "show clock", "show me clock", "show me the clock",
            "open clock app", "open the clock app", "open my clock app",
            "launch clock app", "start clock app",
            "open alarm", "open alarms", "open alarm clock", "open the alarm clock",
            "show alarm", "show alarms", "open phone clock"
        )
        if (englishMatches.contains(normalized)) return true

        val hinglishMatches = listOf(
            "clock kholo", "clock kholna", "clock khol do",
            "clock open karo", "clock open kijiye", "clock open kar",
            "clock chalu karo", "clock chalao", "clock start karo",
            "kholo clock", "open karo clock", "chalu karo clock",
            "clock app kholo", "clock app open karo",
            "ghadi kholo", "ghadi open karo", "ghadi chalu karo",
            "alarm kholo", "alarm open karo", "alarm chalu karo"
        )
        if (hinglishMatches.contains(normalized)) return true

        val banglaMatches = listOf(
            "ঘড়ি খোলো", "ঘড়ি খোলো", "অ্যালার্ম খোলো", "ঘড়ি", "ghori kholo", "alarm kholo"
        )
        if (banglaMatches.contains(normalized)) return true

        val patterns = listOf(
            Regex("^(please )?(open|launch|start|show)( the| my)? (phone |native )?(clock|alarm clock|alarm|alarms)( app)?( please)?$"),
            Regex("^(please )?(clock|ghadi|alarm)( app)? (kholo|kholna|khol do|open karo|open kijiye|open kar|chalu karo|start karo)( please)?$"),
            Regex("^(kholo|open karo|chalu karo) (clock|ghadi|alarm)( app)?$")
        )
        return patterns.any { it.matches(normalized) }
    }

    private fun isOpenCalendarCommand(text: String): Boolean {
        val normalized = text.trim()

        val englishMatches = listOf(
            "open calendar", "open the calendar", "open my calendar",
            "launch calendar", "launch the calendar", "launch my calendar",
            "start calendar", "start the calendar",
            "show calendar", "show me calendar", "show me the calendar",
            "open calendar app", "open the calendar app", "open my calendar app",
            "launch calendar app", "start calendar app",
            "show my calendar", "open phone calendar"
        )
        if (englishMatches.contains(normalized)) return true

        val hinglishMatches = listOf(
            "calendar kholo", "calendar kholna", "calendar khol do",
            "calendar open karo", "calendar open kijiye", "calendar open kar",
            "calendar chalu karo", "calendar chalao", "calendar start karo",
            "kholo calendar", "open karo calendar", "chalu karo calendar",
            "calendar app kholo", "calendar app open karo"
        )
        if (hinglishMatches.contains(normalized)) return true

        val banglaMatches = listOf(
            "ক্যালেন্ডার খোলো", "ক্যালেন্ডার", "calendar kholo"
        )
        if (banglaMatches.contains(normalized)) return true

        val patterns = listOf(
            Regex("^(please )?(open|launch|start|show)( the| my)? (phone |native )?calendar( app)?( please)?$"),
            Regex("^(please )?calendar( app)? (kholo|kholna|khol do|open karo|open kijiye|open kar|chalu karo|start karo)( please)?$"),
            Regex("^(kholo|open karo|chalu karo) calendar( app)?$")
        )
        return patterns.any { it.matches(normalized) }
    }

    private fun isOpenCameraCommand(text: String): Boolean {
        val normalized = text.trim()

        val englishMatches = listOf(
            "open camera", "open the camera", "open my camera",
            "launch camera", "launch the camera", "launch my camera",
            "start camera", "start the camera",
            "show camera", "show me camera", "show me the camera",
            "open camera app", "open the camera app", "open my camera app",
            "launch camera app", "start camera app",
            "open phone camera"
        )
        if (englishMatches.contains(normalized)) return true

        val hinglishMatches = listOf(
            "camera kholo", "camera kholna", "camera khol do",
            "camera open karo", "camera open kijiye", "camera open kar",
            "camera chalu karo", "camera chalao", "camera start karo",
            "kholo camera", "open karo camera", "chalu karo camera",
            "camera app kholo", "camera app open karo"
        )
        if (hinglishMatches.contains(normalized)) return true

        val banglaMatches = listOf(
            "ক্যামেরা খোলো", "ক্যামেরা চালু করো", "ক্যামেরা", "camera kholo"
        )
        if (banglaMatches.contains(normalized)) return true

        val patterns = listOf(
            Regex("^(please )?(open|launch|start|show)( the| my)? (phone |native )?camera( app)?( please)?$"),
            Regex("^(please )?camera( app)? (kholo|kholna|khol do|open karo|open kijiye|open kar|chalu karo|start karo)( please)?$"),
            Regex("^(kholo|open karo|chalu karo) camera( app)?$")
        )
        return patterns.any { it.matches(normalized) }
    }

    private fun isOpenSettingsCommand(text: String): Boolean {
        val normalized = text.trim()

        val englishMatches = listOf(
            "open settings", "open the settings", "open my settings",
            "launch settings", "launch the settings", "launch my settings",
            "start settings", "start the settings",
            "show settings", "show me settings", "show me the settings",
            "open settings app", "open the settings app", "open my settings app",
            "launch settings app", "start settings app",
            "open phone settings", "open system settings", "show phone settings"
        )
        if (englishMatches.contains(normalized)) return true

        val hinglishMatches = listOf(
            "settings kholo", "settings kholna", "settings khol do",
            "settings open karo", "settings open kijiye", "settings open kar",
            "settings chalu karo", "settings chalao", "settings start karo",
            "setting kholo", "setting open karo", "setting chalu karo",
            "kholo settings", "open karo settings", "chalu karo settings",
            "settings app kholo", "settings app open karo"
        )
        if (hinglishMatches.contains(normalized)) return true

        val banglaMatches = listOf(
            "সেটিংস খোলো", "সেটিংস", "settings kholo"
        )
        if (banglaMatches.contains(normalized)) return true

        val patterns = listOf(
            Regex("^(please )?(open|launch|start|show)( the| my)? (phone |system )?settings?( app)?( please)?$"),
            Regex("^(please )?settings?( app)? (kholo|kholna|khol do|open karo|open kijiye|open kar|chalu karo|start karo)( please)?$"),
            Regex("^(kholo|open karo|chalu karo) settings?( app)?$")
        )
        return patterns.any { it.matches(normalized) }
    }

    private fun isGreetingCommand(text: String): Boolean {
        return text == "hello" ||
                text == "hi" ||
                text == "hey" ||
                text == "hello saarthi" ||
                text == "hi saarthi" ||
                text == "hey saarthi" ||
                text == "good morning" ||
                text == "good afternoon" ||
                text == "good evening" ||
                text == "greetings" ||
                text.startsWith("hello ") ||
                text.startsWith("hey ") ||
                text == "saarthi" ||
                text == "ok saarthi" ||
                text == "নমস্কার" ||
                text == "সালাম" ||
                text == "কেমন আছো" ||
                text == "হ্যালো" ||
                text == "kemon acho" ||
                text == "nomoshkar" ||
                text == "assalamu alaikum"
    }

    private fun isIdentityCommand(text: String): Boolean {
        return text.contains("what is your name") ||
                text.contains("what's your name") ||
                text.contains("your name") ||
                text == "who are you" ||
                text == "what are you" ||
                text.contains("tell me about yourself") ||
                text == "তুমি কে" ||
                text.contains("তোমার নাম") ||
                text == "tumi ke" ||
                text.contains("tomar nam") ||
                text.contains("tomar naam")
    }

    private fun isTimeCommand(text: String): Boolean {
        // Guard against unrelated queries containing "time" such as "timetable", "time out", "timeout"
        if (text == "time" || text == "time please" || text == "tell me the time") return true

        val timePatterns = listOf(
            Regex("^what('s| is)? (the )?(current )?time(\\s+is\\s+it)?$"),
            Regex("^current time$"),
            Regex("^tell me (the )?time$"),
            Regex("^(what )?time is it$")
        )

        return timePatterns.any { it.matches(text) } ||
                text == "what time" ||
                text.contains("what time") ||
                text.contains("current time") ||
                text.contains("tell me the time") ||
                text.contains("what's the time") ||
                text.contains("what is the time") ||
                text == "সময় কত" ||
                text == "কয়টা বাজে" ||
                text.contains("shomoy koto") ||
                text.contains("somoy koto") ||
                text.contains("koita baje") ||
                text.contains("koyta baje")
    }

    private fun isDateCommand(text: String): Boolean {
        // Guard against unrelated words like "update", "dates"
        if (text == "date" || text == "date please" || text == "tell me the date") return true

        val datePatterns = listOf(
            Regex("^what('s| is)? (today'?s|the|current)? date$"),
            Regex("^what('s| is)? the date today$"),
            Regex("^what date is (it|today)$"),
            Regex("^today'?s date$"),
            Regex("^current date$"),
            Regex("^tell me (today'?s|the) date$"),
            Regex("^what day is (it|today)$")
        )

        return datePatterns.any { it.matches(text) } ||
                text == "what is today's date" ||
                text == "what is the date" ||
                text == "what's the date" ||
                text == "what date is it" ||
                text == "today's date" ||
                text == "আজকের তারিখ কি" ||
                text == "আজ কত তারিখ" ||
                text.contains("ajker date") ||
                text.contains("aj koto tarikh")
    }

    private fun isHelpCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val exact = listOf(
            "help", "help me", "what can you do", "commands", "supported commands",
            "show commands", "how to use", "what are your features", "features", "options",
            "সাহায্য", "সাহায্য করো", "sahajjo", "shahajjo"
        )
        if (exact.contains(lower)) return true

        if (lower.startsWith("help ") || lower.endsWith(" help") || lower.startsWith("how to ") ||
            lower.endsWith(" commands") || lower.startsWith("commands for ")) {
            return true
        }

        return false
    }

    private fun isStopSpeakingCommand(text: String): Boolean {
        return text == "stop" ||
                text == "stop speaking" ||
                text == "stop talking" ||
                text == "shut up" ||
                text == "be quiet" ||
                text == "quiet" ||
                text == "pause" ||
                text == "silence" ||
                text == "stop audio" ||
                text == "থামো" ||
                text == "চুপ করো" ||
                text == "thamo" ||
                text == "chup koro"
    }

    private fun isStopListeningCommand(text: String): Boolean {
        return text == "stop listening" ||
                text == "stop voice" ||
                text == "cancel listening" ||
                text == "cancel voice"
    }

    private fun isCalculatorCommand(text: String): Boolean {
        if (text.startsWith("calculate") || text.startsWith("compute") || text.startsWith("solve")) {
            return true
        }

        if (text.startsWith("what is ") || text.startsWith("what's ") || text.startsWith("how much is ")) {
            // Check if what follows has digits or math keywords
            val rest = text.substringAfter(" ").substringAfter(" ")
            if (rest.any { it.isDigit() } && (rest.contains("+") || rest.contains("-") || rest.contains("*") ||
                            rest.contains("/") || rest.contains("plus") || rest.contains("minus") ||
                            rest.contains("times") || rest.contains("divided") || rest.contains("power") ||
                            rest.contains("mod") || rest.contains("x") || rest.contains("×") || rest.contains("÷"))) {
                return true
            }
        }

        // Direct math expression like "2 + 2", "15 * 4", "(10 + 5) * 2"
        val hasDigits = text.any { it.isDigit() }
        val hasMathOps = text.contains('+') || text.contains('-') || text.contains('*') ||
                text.contains('/') || text.contains('^') || text.contains('%') ||
                text.contains("plus") || text.contains("minus") || text.contains("times") ||
                text.contains("divided")

        if (hasDigits && hasMathOps) {
            // Verify it doesn't look like an English sentence that happens to have a number
            val validMathChars = text.replace(Regex("[0-9+\\-*/%^(). x×÷]|plus|minus|times|divided|by|over|to|the|power|of|mod"), "").trim()
            if (validMathChars.isEmpty()) {
                return true
            }
        }

        return false
    }

    fun extractCallRecipient(text: String): String {
        val trimmed = text.trim()
        val prefixes = listOf("please call", "make a call to", "phone call to", "call to", "call", "dial")
        for (prefix in prefixes) {
            if (trimmed.startsWith("$prefix ")) {
                return trimmed.removePrefix(prefix).trim().trimEnd('.', '?', '!')
            }
        }
        return ""
    }

    private fun isCallCommand(text: String): Boolean {
        val recipient = extractCallRecipient(text)
        if (recipient.isBlank()) return false
        val lower = recipient.lowercase()
        val invalidRecipients = listOf(
            "this", "it", "that", "me", "what", "back", "off", "out", "away", "up", "down", "today", "yesterday", "tomorrow"
        )
        if (invalidRecipients.contains(lower) || lower.startsWith("off ") || lower.startsWith("out ")) {
            return false
        }
        if (lower.contains("what") || lower.contains("why") || lower.contains("how") || lower.contains("when")) {
            return false
        }
        return true
    }

    private fun isClearDataCommand(text: String): Boolean {
        val normalized = text.trim().lowercase()
        return normalized in listOf(
            "clear data", "clear history", "delete history", "delete data",
            "wipe data", "reset data", "clear all data", "erase history"
        )
    }

    private fun isAffirmativeConfirmation(text: String): Boolean {
        val normalized = text.trim().lowercase()
        return normalized in listOf(
            "yes", "yeah", "yep", "sure", "confirm", "proceed", "haan", "ha", "yes please", "do it"
        )
    }

    private fun isNegativeConfirmation(text: String): Boolean {
        val normalized = text.trim().lowercase()
        return normalized in listOf(
            "no", "nope", "cancel", "abort", "don't", "stop", "na", "nahi", "no thanks", "don't do it"
        )
    }

    private fun isBatteryCommand(text: String): Boolean {
        val normalized = text.trim()

        val englishMatches = listOf(
            "battery", "battery status", "battery percentage", "battery level", "battery life",
            "check battery", "show battery", "phone battery", "how is my battery",
            "how much battery", "what is my battery", "what's my battery",
            "what is my battery percentage", "what's my battery percentage",
            "what is my battery level", "what's my battery level",
            "how much battery do i have", "how much battery is left",
            "is my phone charging", "is the phone charging", "is phone charging", "is it charging",
            "am i charging", "check charge", "charging status"
        )
        if (englishMatches.contains(normalized)) return true

        val banglishMatches = listOf(
            "battery koto", "battery koto ache", "battery percentage koto", "battery level koto",
            "charge koto", "charge koto ache", "charge koto ache?", "charge koto ?", "charge koto?",
            "amar phone er battery koto", "amar phone-er battery koto",
            "amar phone er charge koto", "amar phone-er charge koto",
            "phone er battery koto", "phone er charge koto",
            "phone e charge koto ache", "phone e charge koto",
            "phone charge hocche", "phone charge hocche ki",
            "phone ki charge hocche", "charge hocche ki", "charge hocche",
            "amar phone charge hocche", "amar phone charge hocche ki"
        )
        if (banglishMatches.contains(normalized)) return true

        val banglaMatches = listOf(
            "ব্যাটারি কত", "ব্যাটারি কত আছে", "চার্জ কত", "চার্জ কত আছে",
            "চার্জ কত আছে?", "ব্যাটারি পার্সেন্টেজ কত", "আমার ফোনের ব্যাটারি কত",
            "আমার ফোনের চার্জ কত", "ফোনে চার্জ কত আছে", "আমার ফোন চার্জ হচ্ছে",
            "আমার ফোন চার্জ হচ্ছে?", "ফোন চার্জ হচ্ছে কি", "চার্জ হচ্ছে কি",
            "চার্জ কত ?", "ব্যাটারি স্ট্যাটাস", "ব্যাটারি লেভেল",
            "charge কত আছে", "charge কত আছে?"
        )
        if (banglaMatches.contains(normalized)) return true

        val patterns = listOf(
            Regex("^(please )?(what('s| is)? (my |the )?(current )?battery( percentage| level| status)?)( please)?$"),
            Regex("^(please )?(how much )?battery( is)?( left)?( do i have)?( please)?$"),
            Regex("^(please )?(is )?(my |the )?phone charging( right now)?( please)?$"),
            Regex("^(amar )?phone(-er| er)? (battery|charge) koto( ache)?$"),
            Regex("^(phone|charge) (koto|koto ache)$"),
            Regex("^(amar )?phone (ki )?charge hocche( ki)?$"),
            Regex("^(আমার )?ফোন( কি)? চার্জ হচ্ছে( কি)?$"),
            Regex("^(আমার )?ফোনের? (ব্যাটারি|চার্জ) কত( আছে)?$")
        )
        return patterns.any { it.matches(normalized) }
    }

    private fun isFlashlightOnCommand(text: String): Boolean {
        val normalized = text.trim()

        val englishMatches = listOf(
            "turn on flashlight", "turn on the flashlight", "turn on my flashlight",
            "turn flashlight on", "flashlight on", "switch on flashlight", "switch on the flashlight",
            "enable flashlight", "start flashlight", "torch on", "turn on torch", "turn on the torch",
            "turn torch on", "switch on torch", "enable torch", "light on", "turn on light"
        )
        if (englishMatches.contains(normalized)) return true

        val banglishMatches = listOf(
            "torch on koro", "torch on", "torch jalao", "torch chalu koro", "torch on kor",
            "flashlight on koro", "flashlight on", "flashlight jalao", "flashlight chalu koro",
            "flashlight on kor", "light jalao", "light on koro"
        )
        if (banglishMatches.contains(normalized)) return true

        val banglaMatches = listOf(
            "ফ্ল্যাশলাইট জ্বালাও", "ফ্ল্যাশলাইট অন করো", "ফ্ল্যাশলাইট চালু করো", "ফ্ল্যাশলাইট অন",
            "টর্চ জ্বালাও", "টর্চ অন করো", "টর্চ চালু করো", "টর্চ অন", "লাইট জ্বালাও", "টর্চ জ্বালাও।"
        )
        if (banglaMatches.contains(normalized)) return true

        val patterns = listOf(
            Regex("^(please )?(turn |switch )?(on )?(the |my )?(flashlight|torch)( on)?( please)?$"),
            Regex("^(please )?(flashlight|torch) (on koro|jalao|chalu koro|on kor)( please)?$"),
            Regex("^(ফ্ল্যাশলাইট|টর্চ) (জ্বালাও|অন করো|চালু করো|অন)$")
        )
        return patterns.any { it.matches(normalized) }
    }

    private fun isFlashlightOffCommand(text: String): Boolean {
        val normalized = text.trim()

        val englishMatches = listOf(
            "turn off flashlight", "turn off the flashlight", "turn off my flashlight",
            "turn flashlight off", "flashlight off", "switch off flashlight", "switch off the flashlight",
            "disable flashlight", "stop flashlight", "torch off", "turn off torch", "turn off the torch",
            "turn torch off", "switch off torch", "disable torch", "light off", "turn off light"
        )
        if (englishMatches.contains(normalized)) return true

        val banglishMatches = listOf(
            "torch off koro", "torch off", "torch bondho koro", "torch nibhao", "torch off kor",
            "flashlight off koro", "flashlight off", "flashlight bondho koro", "flashlight nibhao",
            "flashlight off kor", "light bondho koro", "light off koro"
        )
        if (banglishMatches.contains(normalized)) return true

        val banglaMatches = listOf(
            "ফ্ল্যাশলাইট বন্ধ করো", "ফ্ল্যাশলাইট অফ করো", "ফ্ল্যাশলাইট নিভিয়ে দাও", "ফ্ল্যাশলাইট অফ",
            "টর্চ বন্ধ করো", "টর্চ অফ করো", "টর্চ নিভিয়ে দাও", "টর্চ নিভাও", "টর্চ অফ",
            "লাইট বন্ধ করো", "ফ্ল্যাশলাইট বন্ধ করো।"
        )
        if (banglaMatches.contains(normalized)) return true

        val patterns = listOf(
            Regex("^(please )?(turn |switch )?(off )?(the |my )?(flashlight|torch)( off)?( please)?$"),
            Regex("^(please )?(flashlight|torch) (off koro|bondho koro|nibhao|off kor)( please)?$"),
            Regex("^(ফ্ল্যাশলাইট|টর্চ) (বন্ধ করো|অফ করো|নিভিয়ে দাও|নিভাও|অফ)$")
        )
        return patterns.any { it.matches(normalized) }
    }

    private fun isWifiToggleCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val englishMatches = listOf(
            "turn off wifi", "turn wifi off", "turn on wifi", "turn wifi on",
            "switch off wifi", "switch on wifi", "disable wifi", "enable wifi",
            "wifi off", "wifi on", "toggle wifi", "turn off the wifi", "turn on the wifi",
            "switch off the wifi", "switch on the wifi"
        )
        if (englishMatches.contains(lower)) return true

        val banglishMatches = listOf(
            "wifi off koro", "wifi on koro", "wifi bondho koro", "wifi chalu koro",
            "wifi off kor", "wifi on kor", "wifi bandho koro", "wifi bandh koro"
        )
        if (banglishMatches.contains(lower)) return true

        val banglaMatches = listOf(
            "ওয়াইফাই বন্ধ করো", "ওয়াইফাই অন করো", "ওয়াইফাই অফ করো", "ওয়াইফাই চালু করো",
            "ওয়াইফাই বন্ধ কর", "ওয়াইফাই অফ", "ওয়াইফাই অন", "ওয়াইফাই বন্ধ"
        )
        if (banglaMatches.contains(lower)) return true

        val patterns = listOf(
            Regex("^(please )?(turn |switch )?(off |on )?(the )?wifi( off| on)?( please)?$"),
            Regex("^(please )?wifi (off|on|off koro|on koro|bondho koro|chalu koro)( please)?$"),
            Regex("^(ওয়াইফাই) (বন্ধ করো|অন করো|অফ করো|চালু করো|অফ|অন)$")
        )
        return patterns.any { it.matches(lower) }
    }

    private fun isWifiSettingsCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val englishMatches = listOf(
            "open wifi settings", "open wi-fi settings", "wifi settings", "wi-fi settings",
            "open wifi", "open wi-fi", "wifi panel", "internet panel", "show wifi settings",
            "show wi-fi settings", "open wifi panel", "show wifi", "open wi fi settings",
            "wifi", "wi-fi", "wi fi"
        )
        if (englishMatches.contains(lower)) return true

        val banglishMatches = listOf(
            "wifi settings kholo", "wi-fi settings kholo", "wifi kholo", "wi-fi kholo",
            "wifi open karo", "wifi settings open karo", "wifi setting kholo", "wi-fi setting kholo"
        )
        if (banglishMatches.contains(lower)) return true

        val banglaMatches = listOf(
            "ওয়াইফাই সেটিংস খোলো", "ওয়াইফাই খোলো", "ওয়াই-ফাই সেটিংস খোলো",
            "ওয়াইফাই সেটিংস", "ওয়াই-ফাই খোলো", "ওয়াইফাই", "ওয়াই-ফাই"
        )
        if (banglaMatches.contains(lower)) return true

        val patterns = listOf(
            Regex("^(please )?(open|show|launch)( the| my)? (wifi|internet)( settings?| panel)?( please)?$"),
            Regex("^(please )?(wifi|internet) (settings? )?(kholo|open karo|chalu karo)( please)?$"),
            Regex("^(ওয়াইফাই) (সেটিংস )?(খোলো|চালু করো)$")
        )
        return patterns.any { it.matches(lower) }
    }

    private fun isBluetoothToggleCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val englishMatches = listOf(
            "turn off bluetooth", "turn bluetooth off", "turn on bluetooth", "turn bluetooth on",
            "switch off bluetooth", "switch on bluetooth", "disable bluetooth", "enable bluetooth",
            "bluetooth off", "bluetooth on", "toggle bluetooth", "turn off the bluetooth", "turn on the bluetooth"
        )
        if (englishMatches.contains(lower)) return true

        val banglishMatches = listOf(
            "bluetooth off koro", "bluetooth on koro", "bluetooth bondho koro", "bluetooth chalu koro",
            "bluetooth off kor", "bluetooth on kor"
        )
        if (banglishMatches.contains(lower)) return true

        val banglaMatches = listOf(
            "ব্লুটুথ বন্ধ করো", "ব্লুটুথ অন করো", "ব্লুটুথ অফ করো", "ব্লুটুথ চালু করো",
            "ব্লুটুথ বন্ধ কর", "ব্লুটুথ অফ", "ব্লুটুথ অন"
        )
        if (banglaMatches.contains(lower)) return true

        val patterns = listOf(
            Regex("^(please )?(turn |switch )?(off |on )?(the )?bluetooth( off| on)?( please)?$"),
            Regex("^(please )?bluetooth (off|on|off koro|on koro|bondho koro|chalu koro)( please)?$"),
            Regex("^(ব্লুটুথ) (বন্ধ করো|অন করো|অফ করো|চালু করো|অফ|অন)$")
        )
        return patterns.any { it.matches(lower) }
    }

    private fun isBluetoothSettingsCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val englishMatches = listOf(
            "open bluetooth settings", "bluetooth settings", "open bluetooth", "bluetooth panel",
            "show bluetooth settings", "show bluetooth", "launch bluetooth settings", "bluetooth"
        )
        if (englishMatches.contains(lower)) return true

        val banglishMatches = listOf(
            "bluetooth settings kholo", "bluetooth kholo", "bluetooth open karo",
            "bluetooth settings open karo", "bluetooth setting kholo"
        )
        if (banglishMatches.contains(lower)) return true

        val banglaMatches = listOf(
            "ব্লুটুথ সেটিংস খোলো", "ব্লুটুথ খোলো", "ব্লুটুথ সেটিংস", "ব্লুটুথ"
        )
        if (banglaMatches.contains(lower)) return true

        val patterns = listOf(
            Regex("^(please )?(open|show|launch)( the| my)? bluetooth( settings?| panel)?( please)?$"),
            Regex("^(please )?bluetooth (settings? )?(kholo|open karo|chalu karo)( please)?$"),
            Regex("^(ব্লুটুথ) (সেটিংস )?(খোলো|চালু করো)$")
        )
        return patterns.any { it.matches(lower) }
    }

    private fun isMobileDataToggleCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val englishMatches = listOf(
            "turn off mobile data", "turn mobile data off", "turn on mobile data", "turn mobile data on",
            "turn off data", "turn data off", "turn on data", "turn data on",
            "mobile data off", "mobile data on", "data off", "data on",
            "disable mobile data", "enable mobile data"
        )
        if (englishMatches.contains(lower)) return true

        val banglishMatches = listOf(
            "mobile data off koro", "mobile data on koro", "data off koro", "data on koro",
            "mobile data bondho koro", "mobile data chalu koro"
        )
        if (banglishMatches.contains(lower)) return true

        val banglaMatches = listOf(
            "মোবাইল ডাটা বন্ধ করো", "মোবাইল ডাটা অন করো", "মোবাইল ডাটা অফ করো", "মোবাইল ডাটা চালু করো",
            "ডাটা বন্ধ করো", "ডাটা অন করো", "ডাটা অফ করো", "ডাটা চালু করো",
            "মোবাইল ডেটা বন্ধ করো", "মোবাইল ডেটা অন করো"
        )
        if (banglaMatches.contains(lower)) return true

        val patterns = listOf(
            Regex("^(please )?(turn |switch )?(off |on )?(the )?(mobile )?data( off| on)?( please)?$"),
            Regex("^(please )?(mobile )?data (off|on|off koro|on koro|bondho koro|chalu koro)( please)?$"),
            Regex("^(মোবাইল (ডাটা|ডেটা)|ডাটা|ডেটা) (বন্ধ করো|অন করো|অফ করো|চালু করো)$")
        )
        return patterns.any { it.matches(lower) }
    }

    private fun isMobileDataSettingsCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val englishMatches = listOf(
            "open mobile network settings", "mobile network settings", "open mobile data settings",
            "mobile data settings", "open mobile network", "open mobile data", "open cellular data",
            "cellular settings", "mobile data panel", "network settings", "show mobile data settings",
            "show mobile network settings", "open cellular settings", "data roaming settings",
            "cellular data settings", "data settings", "mobile data", "mobile network"
        )
        if (englishMatches.contains(lower)) return true

        val banglishMatches = listOf(
            "mobile network settings kholo", "mobile data settings kholo", "mobile data kholo",
            "mobile network kholo", "data settings kholo", "cellular settings kholo"
        )
        if (banglishMatches.contains(lower)) return true

        val banglaMatches = listOf(
            "মোবাইল নেটওয়ার্ক সেটিংস খোলো", "মোবাইল ডেটা সেটিংস খোলো", "মোবাইল ডাটা সেটিংস খোলো",
            "মোবাইল ডাটা খোলো", "মোবাইল নেটওয়ার্ক খোলো", "মোবাইল ডেটা খোলো",
            "মোবাইল ডাটা সেটিংস", "মোবাইল ডেটা সেটিংস", "মোবাইল নেটওয়ার্ক সেটিংস"
        )
        if (banglaMatches.contains(lower)) return true

        val patterns = listOf(
            Regex("^(please )?(open|show|launch)( the| my)? (mobile (network|data)|cellular|data roaming)( settings?| panel)?( please)?$"),
            Regex("^(please )?(mobile (network|data)|cellular) (settings? )?(kholo|open karo)( please)?$"),
            Regex("^(মোবাইল (নেটওয়ার্ক|ডাটা|ডেটা)) (সেটিংস )?(খোলো|চালু করো)$")
        )
        return patterns.any { it.matches(lower) }
    }

    private fun isSoundSettingsCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val englishMatches = listOf(
            "open volume settings", "volume settings", "open sound settings", "sound settings",
            "open volume panel", "volume panel", "sound panel", "open sound and volume",
            "sound and volume settings", "show volume settings", "show sound settings",
            "audio settings", "open audio settings"
        )
        if (englishMatches.contains(lower)) return true

        val banglishMatches = listOf(
            "volume settings kholo", "sound settings kholo", "volume kholo", "sound kholo",
            "volume setting kholo", "sound setting kholo", "awaj settings kholo"
        )
        if (banglishMatches.contains(lower)) return true

        val banglaMatches = listOf(
            "ভলিউম সেটিংস খোলো", "সাউন্ড সেটিংস খোলো", "ভলিউম খোলো", "সাউন্ড খোলো",
            "শব্দ সেটিংস খোলো", "ভলিউম সেটিংস", "সাউন্ড সেটিংস", "শব্দ সেটিংস"
        )
        if (banglaMatches.contains(lower)) return true

        val patterns = listOf(
            Regex("^(please )?(open|show|launch)( the| my)? (volume|sound|audio)( settings?| panel)?( please)?$"),
            Regex("^(please )?(volume|sound) (settings? )?(kholo|open karo)( please)?$"),
            Regex("^(ভলিউম|সাউন্ড|শব্দ) (সেটিংস )?(খোলো|চালু করো)$")
        )
        return patterns.any { it.matches(lower) }
    }

    private fun isBrightnessSettingsCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val englishMatches = listOf(
            "open brightness settings", "brightness settings", "open display settings",
            "display settings", "brightness panel", "display and brightness settings",
            "open brightness", "show brightness settings", "show display settings",
            "screen settings", "open screen settings"
        )
        if (englishMatches.contains(lower)) return true

        val banglishMatches = listOf(
            "brightness settings kholo", "display settings kholo", "brightness kholo",
            "display kholo", "brightness setting kholo", "alo settings kholo"
        )
        if (banglishMatches.contains(lower)) return true

        val banglaMatches = listOf(
            "ব্রাইটনেস সেটিংস খোলো", "ডিসপ্লে সেটিংস খোলো", "ব্রাইটনেস খোলো", "ডিসপ্লে খোলো",
            "আলো সেটিংস খোলো", "ব্রাইটনেস সেটিংস", "ডিসপ্লে সেটিংস", "উজ্জ্বলতা সেটিংস খোলো",
            "উজ্জ্বলতা সেটিংস", "উজ্জ্বলতা খোলো"
        )
        if (banglaMatches.contains(lower)) return true

        val patterns = listOf(
            Regex("^(please )?(open|show|launch)( the| my)? (brightness|display|screen)( settings?| panel)?( please)?$"),
            Regex("^(please )?(brightness|display) (settings? )?(kholo|open karo)( please)?$"),
            Regex("^(ব্রাইটনেস|ডিসপ্লে|উজ্জ্বলতা) (সেটিংস )?(খোলো|চালু করো)$")
        )
        return patterns.any { it.matches(lower) }
    }

    private fun isTimerPauseCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val matches = listOf(
            "pause timer", "pause the timer", "timer pause", "pause countdown",
            "timer pause koro", "timer thamao", "টাইমার পজ করো", "টাইমার থামাও",
            "pause my timer"
        )
        return matches.contains(lower)
    }

    private fun isTimerResumeCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val matches = listOf(
            "resume timer", "resume the timer", "timer resume", "continue timer",
            "resume countdown", "timer resume koro", "timer chalu koro", "টাইমার চালু করো",
            "টাইমার আবার চালাও", "টাইমার আবার চালু করো", "resume my timer"
        )
        return matches.contains(lower)
    }

    private fun isTimerCancelCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val matches = listOf(
            "cancel timer", "cancel the timer", "stop timer", "stop the timer", "timer cancel",
            "timer stop", "delete timer", "clear timer", "timer cancel koro", "timer bondho koro",
            "টাইমার বন্ধ করো", "টাইমার বাতিল করো", "টাইমার ক্যান্সেল করো", "cancel my timer", "stop my timer"
        )
        return matches.contains(lower)
    }

    private fun isTimerStatusCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val matches = listOf(
            "timer status", "timer remaining", "how much time left on timer", "how much time left",
            "check timer", "timer time left", "time left on timer", "is timer running",
            "timer koto baki", "timer koto baki ache", "timer er koto baki", "timer er time koto",
            "টাইমার কত বাকি", "টাইমার স্ট্যাটাস", "টাইমারে কত সময় বাকি আছে", "টাইমারে কত সময় বাকি আছে"
        )
        return matches.contains(lower)
    }

    private fun isTimerSetCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (isTimerPauseCommand(lower) || isTimerResumeCommand(lower) ||
            isTimerCancelCommand(lower) || isTimerStatusCommand(lower)) {
            return false
        }
        if (lower.startsWith("set a timer") || lower.startsWith("set timer") ||
            lower.startsWith("start a timer") || lower.startsWith("start timer") ||
            lower.startsWith("create a timer") || lower.startsWith("create timer") ||
            lower.contains("timer dao") || lower.contains("timer set") ||
            lower.contains("er timer") || lower.contains("মিনিটের টাইমার") ||
            lower.contains("সেকেন্ডের টাইমার") || lower.contains("ঘণ্টার টাইমার") ||
            lower.contains("ঘন্টার টাইমার") ||
            lower.contains("টাইমার দাও") || lower.contains("টাইমার সেট") ||
            Regex("^\\d+\\s*(?:minute|min|second|sec|hour|hr)s?\\s*timer$").matches(lower) ||
            Regex("^(please )?(timer|set timer|start timer)\\b").containsMatchIn(lower) ||
            Regex("\\b(?:timer|টাইমার)\\b").containsMatchIn(lower)
        ) {
            return true
        }
        return false
    }

    fun isAnswerCallCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val exactMatches = listOf(
            "answer", "answer call", "answer the call", "answer incoming call", "answer phone", "answer the phone",
            "receive", "receive call", "receive the call", "receive incoming call", "receive phone", "receive the phone",
            "pick up", "pick up call", "pick up the call", "pick up phone", "pick up the phone",
            "accept call", "accept the call", "take call", "take the call",
            "কল ধরো", "ফোন ধরো", "ধরো", "কলটা ধরো", "ফোনটা ধরো", "রিসিভ করো", "কল রিসিভ করো", "ফোন রিসিভ করো",
            "কলটি ধরুন", "ফোন ধরুন", "কল ধরুন",
            "call dhoro", "phone dhoro", "dhoro", "call receive koro", "phone receive koro", "receive koro",
            "phone ta dhoro", "call ta dhoro", "pick up koro", "call pick up koro"
        )
        if (exactMatches.contains(lower)) return true
        if (lower.startsWith("answer call") || lower.startsWith("receive call") ||
            lower.endsWith("call dhoro") || lower.endsWith("phone dhoro") ||
            lower.endsWith("কল ধরো") || lower.endsWith("ফোন ধরো")) {
            return true
        }
        return false
    }

    fun isRejectCallCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        val exactMatches = listOf(
            "reject", "reject call", "reject the call", "reject incoming call",
            "decline", "decline call", "decline the call", "decline incoming call",
            "ignore", "ignore call", "ignore the call", "ignore incoming call",
            "hang up", "hang up call", "hang up the phone",
            "cut call", "cut the call", "dismiss call",
            "কল কাটো", "ফোন কাটো", "কাটো", "কলটা কাটো", "ফোনটা কাটো", "কেটে দাও", "কল কেটে দাও", "ফোন কেটে দাও",
            "রিজেক্ট করো", "কল রিজেক্ট করো", "ফোন রিজেক্ট করো", "কলটি কাটুন", "কল কাটুন",
            "call kato", "phone kato", "kato", "kete dao", "call kete dao", "phone kete dao",
            "reject koro", "call reject koro", "phone reject koro", "decline koro", "call decline koro",
            "ignore koro", "call ignore koro", "call cut koro"
        )
        if (exactMatches.contains(lower)) return true
        if (lower.startsWith("reject call") || lower.startsWith("decline call") ||
            lower.endsWith("call kato") || lower.endsWith("phone kato") ||
            lower.endsWith("কল কাটো") || lower.endsWith("ফোন কাটো")) {
            return true
        }
        return false
    }
}