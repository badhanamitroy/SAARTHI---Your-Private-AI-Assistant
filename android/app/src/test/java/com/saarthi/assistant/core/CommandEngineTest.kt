package com.saarthi.assistant.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandEngineTest {

    private val commandEngine = CommandEngine()

    // ---------------------------------------------------------
    // 1. Baseline tests (Preserved 100% backward compatible)
    // ---------------------------------------------------------
    @Test
    fun greetingCommand_returnsGreeting() {
        val result = commandEngine.process("hello")
        assertEquals(
            CommandResult.Reply("Hello! I am SAARTHI. How can I help you?"),
            result
        )
    }

    @Test
    fun nameCommand_returnsAssistantName() {
        val result = commandEngine.process("what is your name")
        assertEquals(
            CommandResult.Reply("My name is SAARTHI, your private AI assistant."),
            result
        )
    }

    @Test
    fun timeCommand_returnsReply() {
        val result = commandEngine.process("what time is it")
        assertTrue(result is CommandResult.Reply)
        val text = (result as CommandResult.Reply).text
        assertTrue(text.startsWith("The current time is "))
    }

    @Test
    fun stopCommand_returnsStopSpeaking() {
        val result = commandEngine.process("stop speaking")
        assertEquals(CommandResult.StopSpeaking, result)
    }

    @Test
    fun unknownCommand_requiresOnlineAI() {
        val result = commandEngine.process(
            "tell me something about quantum physics"
        )
        assertEquals(CommandResult.NeedsOnlineAI, result)
    }

    // ---------------------------------------------------------
    // 2. Offline Greeting Variations & Wake Words
    // ---------------------------------------------------------
    @Test
    fun greetingVariants_returnGreeting() {
        val greetings = listOf(
            "hi", "hey", "hello saarthi", "hi saarthi", "hey saarthi",
            "good morning", "good afternoon", "good evening", "greetings",
            "HELLO", "Hi!", "hey?", "saarthi", "ok saarthi"
        )
        for (g in greetings) {
            val result = commandEngine.process(g)
            assertEquals(
                "Failed on greeting variation: $g",
                CommandResult.Reply("Hello! I am SAARTHI. How can I help you?"),
                result
            )
        }
    }

    // ---------------------------------------------------------
    // 3. Offline Identity Variations
    // ---------------------------------------------------------
    @Test
    fun identityVariants_returnAssistantName() {
        val identities = listOf(
            "who are you", "what are you", "what's your name",
            "your name", "tell me about yourself", "WHO ARE YOU?"
        )
        for (id in identities) {
            val result = commandEngine.process(id)
            assertEquals(
                "Failed on identity variation: $id",
                CommandResult.Reply("My name is SAARTHI, your private AI assistant."),
                result
            )
        }
    }

    // ---------------------------------------------------------
    // 4. Offline Time Variations
    // ---------------------------------------------------------
    @Test
    fun timeVariants_returnReply() {
        val times = listOf(
            "time", "what time", "current time", "tell me the time",
            "what's the time", "what is the time", "time please", "TIME?"
        )
        for (t in times) {
            val result = commandEngine.process(t)
            assertTrue("Failed on time variation: $t", result is CommandResult.Reply)
            assertTrue((result as CommandResult.Reply).text.startsWith("The current time is "))
        }
    }

    // ---------------------------------------------------------
    // 5. Offline Date Variations
    // ---------------------------------------------------------
    @Test
    fun dateVariants_returnCurrentDate() {
        val dates = listOf(
            "what is today's date", "what's today's date", "today's date",
            "what is the date", "what date is it", "current date", "date please",
            "tell me the date", "what day is it", "what day is today"
        )
        for (d in dates) {
            val result = commandEngine.process(d)
            assertTrue("Failed on date variation: $d", result is CommandResult.Reply)
            assertTrue(
                "Expected 'Today is ...' for input: $d",
                (result as CommandResult.Reply).text.startsWith("Today is ")
            )
        }
    }

    // ---------------------------------------------------------
    // 6. Native App Launch: Calculator (English & Hinglish)
    // ---------------------------------------------------------
    @Test
    fun openCalculator_exactPhase2ARequirements_returnsOpenCalculator() {
        // Explicitly mandated in Phase 2A prompt:
        // - “Open calculator”
        // - “Calculator kholo”
        // - “Hey SAARTHI, open my calculator”
        assertEquals(CommandResult.OpenCalculator, commandEngine.process("Open calculator"))
        assertEquals(CommandResult.OpenCalculator, commandEngine.process("Calculator kholo"))
        assertEquals(CommandResult.OpenCalculator, commandEngine.process("Hey SAARTHI, open my calculator"))
    }

    @Test
    fun openCalculator_englishVariants_returnsOpenCalculator() {
        val englishLaunches = listOf(
            "Hey SAARTHI, open calculator.",
            "open calculator",
            "Open calculator",
            "Open my calculator",
            "open the calculator",
            "launch calculator",
            "launch the calculator",
            "start calculator",
            "show calculator",
            "Hey SAARTHI, open my calculator",
            "hey saarthi open my calculator",
            "open calculator app",
            "open the calculator app",
            "open my calculator app",
            "launch calculator app",
            "open calc",
            "please open calculator"
        )
        for (cmd in englishLaunches) {
            val result = commandEngine.process(cmd)
            assertEquals("Failed on English calculator launch query: $cmd", CommandResult.OpenCalculator, result)
        }
    }

    @Test
    fun openCalculator_hinglishVariants_returnsOpenCalculator() {
        val hinglishLaunches = listOf(
            "SAARTHI, calculator kholo.",
            "calculator kholo",
            "Calculator kholo",
            "calculator kholna",
            "calculator khol do",
            "calculator open karo",
            "calculator open kijiye",
            "calculator chalu karo",
            "calculator chalao",
            "calculator start karo",
            "kholo calculator",
            "calculator app kholo",
            "calculator app open karo",
            "calculator app chalu karo",
            "calc kholo",
            "calc open karo"
        )
        for (cmd in hinglishLaunches) {
            val result = commandEngine.process(cmd)
            assertEquals("Failed on Hinglish calculator launch query: $cmd", CommandResult.OpenCalculator, result)
        }
    }

    @Test
    fun openCalculator_doesNotTriggerArithmeticEvaluation() {
        // Must return OpenCalculator, never mathematical evaluation
        val result1 = commandEngine.process("Hey SAARTHI, open calculator.")
        assertEquals(CommandResult.OpenCalculator, result1)

        val result2 = commandEngine.process("SAARTHI, calculator kholo.")
        assertEquals(CommandResult.OpenCalculator, result2)

        val result3 = commandEngine.process("Open my calculator.")
        assertEquals(CommandResult.OpenCalculator, result3)

        val result4 = commandEngine.process("Open calculator")
        assertEquals(CommandResult.OpenCalculator, result4)

        val result5 = commandEngine.process("Calculator kholo")
        assertEquals(CommandResult.OpenCalculator, result5)
    }

    // ---------------------------------------------------------
    // 6B. General App Launch: Clock, Calendar, Camera, Settings
    // ---------------------------------------------------------
    @Test
    fun openClock_englishAndHinglish_returnsOpenAppClock() {
        val clockQueries = listOf(
            "open clock", "Open Clock", "open my clock", "open the clock",
            "launch clock", "start clock", "open clock app",
            "open alarm", "open alarms", "open alarm clock", "open phone clock",
            "Hey SAARTHI, open clock", "clock kholo", "ghadi kholo", "alarm kholo",
            "clock open karo", "ghadi open karo", "kholo clock"
        )
        for (cmd in clockQueries) {
            val result = commandEngine.process(cmd)
            assertEquals("Failed on clock query: $cmd", CommandResult.OpenApp(TargetApp.CLOCK), result)
        }
    }

    @Test
    fun openCalendar_englishAndHinglish_returnsOpenAppCalendar() {
        val calendarQueries = listOf(
            "open calendar", "Open Calendar", "open my calendar", "open the calendar",
            "launch calendar", "start calendar", "open calendar app", "show calendar",
            "show my calendar", "Hey SAARTHI, open calendar",
            "calendar kholo", "calendar kholna", "calendar open karo", "kholo calendar",
            "calendar chalu karo", "calendar app kholo"
        )
        for (cmd in calendarQueries) {
            val result = commandEngine.process(cmd)
            assertEquals("Failed on calendar query: $cmd", CommandResult.OpenApp(TargetApp.CALENDAR), result)
        }
    }

    @Test
    fun openCamera_englishAndHinglish_returnsOpenAppCamera() {
        val cameraQueries = listOf(
            "open camera", "Open Camera", "open my camera", "open the camera",
            "launch camera", "start camera", "open camera app",
            "Hey SAARTHI, open camera", "camera kholo", "camera kholna",
            "camera open karo", "camera chalu karo", "kholo camera", "camera app kholo"
        )
        for (cmd in cameraQueries) {
            val result = commandEngine.process(cmd)
            assertEquals("Failed on camera query: $cmd", CommandResult.OpenApp(TargetApp.CAMERA), result)
        }
    }

    @Test
    fun openSettings_englishAndHinglish_returnsOpenAppSettings() {
        val settingsQueries = listOf(
            "open settings", "Open Settings", "open my settings", "open the settings",
            "launch settings", "start settings", "open settings app",
            "open phone settings", "open system settings",
            "Hey SAARTHI, open settings", "settings kholo", "setting kholo",
            "settings open karo", "setting open karo", "settings chalu karo", "kholo settings"
        )
        for (cmd in settingsQueries) {
            val result = commandEngine.process(cmd)
            assertEquals("Failed on settings query: $cmd", CommandResult.OpenApp(TargetApp.SETTINGS), result)
        }
    }

    @Test
    fun appLaunching_separatedFromTimeDateMath() {
        // "what time is it" must return time, NOT open Clock app
        val timeResult = commandEngine.process("what time is it")
        assertTrue(timeResult is CommandResult.Reply)
        assertTrue((timeResult as CommandResult.Reply).text.startsWith("The current time is "))

        // "what is today's date" must return date, NOT open Calendar app
        val dateResult = commandEngine.process("what is today's date")
        assertTrue(dateResult is CommandResult.Reply)
        assertTrue((dateResult as CommandResult.Reply).text.startsWith("Today is "))

        // "calculate 5 + 5" must evaluate math, NOT open Calculator app
        val mathResult = commandEngine.process("calculate 5 + 5")
        assertEquals(CommandResult.Reply("The answer is 10."), mathResult)
    }

    // ---------------------------------------------------------
    // 7. Wake Word Prefix Stripping For Other Commands
    // ---------------------------------------------------------
    @Test
    fun wakeWordPrefix_stripsCorrectlyForGeneralCommands() {
        val timeResult = commandEngine.process("Hey SAARTHI, what time is it?")
        assertTrue(timeResult is CommandResult.Reply)
        assertTrue((timeResult as CommandResult.Reply).text.startsWith("The current time is "))

        val dateResult = commandEngine.process("SAARTHI: what is today's date?")
        assertTrue(dateResult is CommandResult.Reply)
        assertTrue((dateResult as CommandResult.Reply).text.startsWith("Today is "))

        val calcResult = commandEngine.process("Hey SAARTHI calculate 10 + 20")
        assertEquals(CommandResult.Reply("The answer is 30."), calcResult)

        val helpResult = commandEngine.process("Ok SAARTHI, what can you do?")
        assertTrue(helpResult is CommandResult.Reply)
        assertTrue((helpResult as CommandResult.Reply).text.contains("Here is what I can do offline"))
    }

    // ---------------------------------------------------------
    // 8. Offline Stop Speaking & Stop Listening Variations
    // ---------------------------------------------------------
    @Test
    fun stopSpeakingVariants_returnStopSpeaking() {
        val stops = listOf(
            "stop", "stop talking", "shut up", "be quiet",
            "quiet", "pause", "silence", "STOP!", "stop audio"
        )
        for (s in stops) {
            val result = commandEngine.process(s)
            assertEquals("Failed on stop speaking variation: $s", CommandResult.StopSpeaking, result)
        }
    }

    @Test
    fun stopListeningVariants_returnStopListening() {
        val stopListenings = listOf(
            "stop listening", "stop voice", "cancel listening", "cancel voice"
        )
        for (sl in stopListenings) {
            val result = commandEngine.process(sl)
            assertEquals("Failed on stop listening variation: $sl", CommandResult.StopListening, result)
        }
    }

    // ---------------------------------------------------------
    // 9. Help & Command Discovery
    // ---------------------------------------------------------
    @Test
    fun helpVariants_returnSupportedCommands() {
        val helpQueries = listOf("help", "help me", "what can you do", "commands", "supported commands", "how to use", "options")
        for (h in helpQueries) {
            val result = commandEngine.process(h)
            assertTrue("Failed on help query: $h", result is CommandResult.Reply)
            val text = (result as CommandResult.Reply).text
            assertTrue(text.contains("Here is what I can do offline"))
        }
    }

    // ---------------------------------------------------------
    // 10. Basic Calculator & Operator Precedence
    // ---------------------------------------------------------
    @Test
    fun calculatorArithmetic_evaluatesCorrectly() {
        val calculations = mapOf(
            "calculate 2 + 2" to "The answer is 4.",
            "calculate 25 plus 17" to "The answer is 42.",
            "calculate 10 - 4" to "The answer is 6.",
            "compute 5 times 8" to "The answer is 40.",
            "what is 100 divided by 5" to "The answer is 20.",
            "what is 10 minus 3" to "The answer is 7.",
            "calculate 2.5 * 4" to "The answer is 10.",
            "calculate 10 / 4" to "The answer is 2.5.",
            "calculate 2 ^ 3" to "The answer is 8.",
            "calculate 10 % 3" to "The answer is 1."
        )

        for ((input, expected) in calculations) {
            val result = commandEngine.process(input)
            assertEquals("Failed on math input: $input", CommandResult.Reply(expected), result)
        }
    }

    @Test
    fun calculatorPrecedence_respectsPEMDAS() {
        // Multiplication before addition: 2 + 3 * 4 = 14 (not 20)
        val result1 = commandEngine.process("calculate 2 + 3 * 4")
        assertEquals(CommandResult.Reply("The answer is 14."), result1)

        // Parentheses override: (2 + 3) * 4 = 20
        val result2 = commandEngine.process("calculate (2 + 3) * 4")
        assertEquals(CommandResult.Reply("The answer is 20."), result2)

        // Exponent before multiplication: 2 * 3 ^ 2 = 18
        val result3 = commandEngine.process("calculate 2 * 3 ^ 2")
        assertEquals(CommandResult.Reply("The answer is 18."), result3)
    }

    @Test
    fun calculatorDivisionByZero_rejectsGracefully() {
        val divZeroInputs = listOf(
            "calculate 10 / 0",
            "what is 5 divided by 0",
            "calculate 100 % 0"
        )
        for (input in divZeroInputs) {
            val result = commandEngine.process(input)
            assertEquals(
                "Failed on div-by-zero input: $input",
                CommandResult.Reply("Division by zero is not allowed."),
                result
            )
        }
    }

    @Test
    fun calculatorInvalidInput_rejectsGracefully() {
        val invalidInputs = listOf(
            "calculate 2 +",
            "calculate (2 + 3",
            "calculate 2 ++ 3",
            "calculate abc + 5"
        )
        for (input in invalidInputs) {
            val result = commandEngine.process(input)
            assertTrue("Expected reply for invalid math: $input", result is CommandResult.Reply)
            val text = (result as CommandResult.Reply).text
            assertTrue("Expected error message for $input, got: $text", text.contains("expression") || text.contains("syntax") || text.contains("Invalid") || text.contains("Mismatched") || text.contains("Incomplete") || text.contains("Unsupported"))
        }
    }

    // ---------------------------------------------------------
    // 11. Ambiguous Commands Handling
    // ---------------------------------------------------------
    @Test
    fun ambiguousCommands_askForClarification() {
        val ambiguousCases = mapOf(
            "date or time" to "Did you want to check the current date or the current time?",
            "time or date" to "Did you want to check the current date or the current time?",
            "calculator" to "Did you want to open the calculator app, or calculate an arithmetic expression?",
            "calculator app" to "Did you want to open the calculator app, or calculate an arithmetic expression?",
            "calc" to "Did you want to open the calculator app, or calculate an arithmetic expression?",
            "clock" to "Did you want to check the current time, or open the clock app?",
            "clock app" to "Did you want to check the current time, or open the clock app?",
            "alarm" to "Did you want to check the current time, or open the clock app?",
            "calendar" to "Did you want to check today's date, or open the calendar app?",
            "calendar app" to "Did you want to check today's date, or open the calendar app?",
            "camera" to "Did you want to open the camera app?",
            "camera app" to "Did you want to open the camera app?",
            "settings" to "Did you want to open device settings?",
            "setting" to "Did you want to open device settings?",
            "settings app" to "Did you want to open device settings?",
            "phone settings" to "Did you want to open device settings?",
            "calculate" to "What arithmetic expression would you like me to calculate?",
            "math" to "What math problem would you like me to solve?",
            "stop speaking or listening" to "Would you like me to stop speaking or stop listening?"
        )

        for ((input, expectedQuestion) in ambiguousCases) {
            val result = commandEngine.process(input)
            assertEquals("Failed for ambiguous query: $input", CommandResult.Clarification(expectedQuestion), result)
        }
    }

    // ---------------------------------------------------------
    // 12. Multiple Intents in One Utterance
    // ---------------------------------------------------------
    @Test
    fun combinedIntents_executeInSequence() {
        // Time and Date
        val timeAndDate = commandEngine.process("what time is it and what is today's date")
        assertTrue(timeAndDate is CommandResult.Reply)
        val tdText = (timeAndDate as CommandResult.Reply).text
        assertTrue("Expected both time and date in: $tdText", tdText.contains("The current time is ") && tdText.contains("Today is "))

        // Greeting and Time
        val greetingAndTime = commandEngine.process("hello and what is the time")
        assertTrue(greetingAndTime is CommandResult.Reply)
        val gtText = (greetingAndTime as CommandResult.Reply).text
        assertTrue("Expected greeting and time in: $gtText", gtText.startsWith("Hello! I am SAARTHI.") && gtText.contains("The current time is "))

        // Identity and Help
        val idAndHelp = commandEngine.process("who are you also what can you do")
        assertTrue(idAndHelp is CommandResult.Reply)
        val ihText = (idAndHelp as CommandResult.Reply).text
        assertTrue("Expected identity and help in: $ihText", ihText.contains("My name is SAARTHI") && ihText.contains("Here is what I can do offline"))

        // Date and Calculator
        val dateAndCalc = commandEngine.process("what is today's date and calculate 5 plus 5")
        assertTrue(dateAndCalc is CommandResult.Reply)
        val dcText = (dateAndCalc as CommandResult.Reply).text
        assertTrue("Expected date and calc in: $dcText", dcText.contains("Today is ") && dcText.contains("The answer is 10."))
    }

    // ---------------------------------------------------------
    // 13. False-Positive Avoidance & Unrelated Queries
    // ---------------------------------------------------------
    @Test
    fun unrelatedWords_notMistakenForCommands() {
        val unrelated = listOf(
            "timetable for morning train",
            "timeout occurred",
            "update my profile",
            "dates are sweet fruits",
            "stopwatch app",
            "helium balloon"
        )
        for (query in unrelated) {
            val result = commandEngine.process(query)
            assertEquals("Unrelated query triggered a false positive: $query", CommandResult.NeedsOnlineAI, result)
        }
    }

    // ---------------------------------------------------------
    // 14. Empty and Whitespace Handling
    // ---------------------------------------------------------
    @Test
    fun emptyOrWhitespaceInput_returnsPromptToSpeak() {
        val emptyInputs = listOf("", "   ", "\t", "\n")
        for (input in emptyInputs) {
            val result = commandEngine.process(input)
            assertTrue(result is CommandResult.Reply)
            assertEquals(
                "I didn't hear anything. Please type or say a command.",
                (result as CommandResult.Reply).text
            )
        }
    }

    // ---------------------------------------------------------
    // 15. Punctuation Stripping
    // ---------------------------------------------------------
    @Test
    fun punctuationHandling_preservesIntent() {
        val resultHello = commandEngine.process("hello...?!")
        assertEquals(
            CommandResult.Reply("Hello! I am SAARTHI. How can I help you?"),
            resultHello
        )

        val resultStop = commandEngine.process("stop speaking!!!")
        assertEquals(CommandResult.StopSpeaking, resultStop)

        val resultCalc = commandEngine.process("calculate 10 + 20???")
        assertEquals(CommandResult.Reply("The answer is 30."), resultCalc)
    }

    // ---------------------------------------------------------
    // 16. Battery Status Commands (English, Banglish, Bangla)
    // ---------------------------------------------------------
    @Test
    fun batteryCommands_english_routesToGetBatteryStatus() {
        val englishQueries = listOf(
            "what is my battery percentage",
            "what's my battery percentage",
            "what is my battery level",
            "battery",
            "battery status",
            "battery level",
            "check battery",
            "show battery",
            "how much battery do i have",
            "how much battery is left",
            "is my phone charging",
            "is phone charging",
            "is it charging"
        )
        for (query in englishQueries) {
            val result = commandEngine.process(query)
            assertEquals("Failed on English battery query: $query", CommandResult.GetBatteryStatus, result)
        }
    }

    @Test
    fun batteryCommands_banglish_routesToGetBatteryStatus() {
        val banglishQueries = listOf(
            "Battery koto ache?",
            "battery koto ache",
            "Charge koto ache?",
            "charge koto ache",
            "Charge koto ?",
            "charge koto?",
            "Amar phone-er battery koto?",
            "amar phone er battery koto",
            "amar phone er charge koto",
            "phone charge hocche ki",
            "amar phone charge hocche",
            "phone er battery koto",
            "phone e charge koto ache"
        )
        for (query in banglishQueries) {
            val result = commandEngine.process(query)
            assertEquals("Failed on Banglish battery query: $query", CommandResult.GetBatteryStatus, result)
        }
    }

    @Test
    fun batteryCommands_bangla_routesToGetBatteryStatus() {
        val banglaQueries = listOf(
            "ব্যাটারি কত",
            "ব্যাটারি কত আছে",
            "চার্জ কত",
            "চার্জ কত আছে",
            "চার্জ কত আছে?",
            "আমার ফোন চার্জ হচ্ছে?",
            "আমার ফোন চার্জ হচ্ছে",
            "আমার ফোনের ব্যাটারি কত",
            "ফোনে চার্জ কত আছে",
            "ফোন চার্জ হচ্ছে কি",
            "Charge কত আছে?",
            "Charge কত আছে"
        )
        for (query in banglaQueries) {
            val result = commandEngine.process(query)
            assertEquals("Failed on Bangla battery query: $query", CommandResult.GetBatteryStatus, result)
        }
    }

    // ---------------------------------------------------------
    // 17. Flashlight Commands (ON / OFF - English, Banglish, Bangla)
    // ---------------------------------------------------------
    @Test
    fun flashlightOnCommands_routesToSetFlashlightTrue() {
        val onQueries = listOf(
            "Turn on flashlight.",
            "turn on flashlight",
            "turn on the flashlight",
            "flashlight on",
            "turn flashlight on",
            "torch on",
            "turn on torch",
            "switch on flashlight",
            "Torch on koro.",
            "torch on koro",
            "torch jalao",
            "flashlight on koro",
            "flashlight jalao",
            "ফ্ল্যাশলাইট জ্বালাও।",
            "ফ্ল্যাশলাইট জ্বালাও",
            "টর্চ জ্বালাও",
            "ফ্ল্যাশলাইট অন করো",
            "টর্চ অন করো"
        )
        for (query in onQueries) {
            val result = commandEngine.process(query)
            assertEquals("Failed on Flashlight ON query: $query", CommandResult.SetFlashlight(enabled = true), result)
        }
    }

    @Test
    fun flashlightOffCommands_routesToSetFlashlightFalse() {
        val offQueries = listOf(
            "Flashlight off.",
            "flashlight off",
            "turn off flashlight",
            "turn off the flashlight",
            "turn flashlight off",
            "torch off",
            "turn off torch",
            "switch off flashlight",
            "Torch off koro.",
            "torch off koro",
            "torch bondho koro",
            "torch nibhao",
            "flashlight off koro",
            "flashlight bondho koro",
            "ফ্ল্যাশলাইট বন্ধ করো।",
            "ফ্ল্যাশলাইট বন্ধ করো",
            "টর্চ বন্ধ করো",
            "ফ্ল্যাশলাইট অফ করো",
            "টর্চ অফ করো"
        )
        for (query in offQueries) {
            val result = commandEngine.process(query)
            assertEquals("Failed on Flashlight OFF query: $query", CommandResult.SetFlashlight(enabled = false), result)
        }
    }

    @Test
    fun flashlightAmbiguousQuery_asksForClarification() {
        val ambiguousQueries = listOf(
            "flashlight",
            "torch",
            "flash light",
            "ফ্ল্যাশলাইট",
            "টর্চ"
        )
        for (query in ambiguousQueries) {
            val result = commandEngine.process(query)
            assertTrue("Expected clarification for ambiguous query: $query", result is CommandResult.Clarification)
            val question = (result as CommandResult.Clarification).question
            assertEquals(
                "Did you want to turn on or turn off the flashlight?",
                question
            )
        }
    }

    // ---------------------------------------------------------
    // 18. System Settings Commands (Wi-Fi, Bluetooth, Mobile Data, Sound, Brightness)
    // ---------------------------------------------------------

    @Test
    fun wifiSettingsCommands_routesToOpenSystemSettingsWifi() {
        val queries = listOf(
            "open wifi settings",
            "open wi-fi settings",
            "wifi settings",
            "wi-fi settings kholo",
            "wifi settings kholo",
            "ওয়াইফাই সেটিংস খোলো",
            "ওয়াইফাই সেটিংস"
        )
        for (query in queries) {
            val result = commandEngine.process(query)
            assertEquals("Failed on Wi-Fi settings query: $query", CommandResult.OpenSystemSettings(SystemSettingType.WIFI), result)
        }
    }

    @Test
    fun bluetoothSettingsCommands_routesToOpenSystemSettingsBluetooth() {
        val queries = listOf(
            "open bluetooth settings",
            "bluetooth settings",
            "bluetooth settings kholo",
            "ব্লুটুথ সেটিংস খোলো",
            "ব্লুটুথ সেটিংস"
        )
        for (query in queries) {
            val result = commandEngine.process(query)
            assertEquals("Failed on Bluetooth settings query: $query", CommandResult.OpenSystemSettings(SystemSettingType.BLUETOOTH), result)
        }
    }

    @Test
    fun mobileDataSettingsCommands_routesToOpenSystemSettingsMobileData() {
        val queries = listOf(
            "open mobile network settings",
            "mobile data settings",
            "data roaming settings",
            "cellular settings",
            "mobile network settings kholo",
            "মোবাইল নেটওয়ার্ক সেটিংস খোলো",
            "মোবাইল ডাটা সেটিংস"
        )
        for (query in queries) {
            val result = commandEngine.process(query)
            assertEquals("Failed on Mobile Data settings query: $query", CommandResult.OpenSystemSettings(SystemSettingType.MOBILE_DATA), result)
        }
    }

    @Test
    fun soundSettingsCommands_routesToOpenSystemSettingsSound() {
        val queries = listOf(
            "open sound settings",
            "volume settings",
            "audio settings",
            "volume settings kholo",
            "sound settings kholo",
            "ভলিউম সেটিংস খোলো",
            "শব্দ সেটিংস খোলো"
        )
        for (query in queries) {
            val result = commandEngine.process(query)
            assertEquals("Failed on Sound settings query: $query", CommandResult.OpenSystemSettings(SystemSettingType.SOUND), result)
        }
    }

    @Test
    fun brightnessSettingsCommands_routesToOpenSystemSettingsBrightness() {
        val queries = listOf(
            "open brightness settings",
            "display settings",
            "screen settings",
            "brightness settings kholo",
            "উজ্জ্বলতা সেটিংস খোলো",
            "ডিসপ্লে সেটিংস খোলো"
        )
        for (query in queries) {
            val result = commandEngine.process(query)
            assertEquals("Failed on Brightness settings query: $query", CommandResult.OpenSystemSettings(SystemSettingType.BRIGHTNESS), result)
        }
    }

    // ---------------------------------------------------------
    // 19. System Settings Direct Toggle Ambiguity & Safety Checks
    // ---------------------------------------------------------

    @Test
    fun directToggleRequests_askForClarificationAndExplainPolicy() {
        val toggleWifiQueries = listOf("turn on wifi", "turn off wifi", "wifi on koro", "ওয়াইফাই অন করো")
        for (q in toggleWifiQueries) {
            val result = commandEngine.process(q)
            assertTrue("Expected clarification for Wi-Fi toggle request: $q", result is CommandResult.Clarification)
            val question = (result as CommandResult.Clarification).question
            assertTrue("Expected explanation about opening settings in: $question", question.contains("settings"))
        }

        val toggleBtQueries = listOf("turn on bluetooth", "turn off bluetooth", "bluetooth off koro", "ব্লুটুথ বন্ধ করো")
        for (q in toggleBtQueries) {
            val result = commandEngine.process(q)
            assertTrue("Expected clarification for Bluetooth toggle request: $q", result is CommandResult.Clarification)
            val question = (result as CommandResult.Clarification).question
            assertTrue("Expected explanation about opening settings in: $question", question.contains("settings"))
        }

        val toggleDataQueries = listOf("turn on mobile data", "turn off data", "mobile data on koro")
        for (q in toggleDataQueries) {
            val result = commandEngine.process(q)
            assertTrue("Expected clarification for Data toggle request: $q", result is CommandResult.Clarification)
            val question = (result as CommandResult.Clarification).question
            assertTrue("Expected explanation about opening settings in: $question", question.contains("settings"))
        }
    }

    // ---------------------------------------------------------
    // 20. Timer Commands (Set, Pause, Resume, Cancel, Status)
    // ---------------------------------------------------------

    @Test
    fun setTimer_englishQueries_routesToSetTimerWithAccurateSeconds() {
        val q1 = commandEngine.process("Set a timer for 2 minutes")
        assertTrue(q1 is CommandResult.SetTimer)
        assertEquals(120L, (q1 as CommandResult.SetTimer).durationSeconds)

        val q2 = commandEngine.process("Set timer for 30 seconds")
        assertTrue(q2 is CommandResult.SetTimer)
        assertEquals(30L, (q2 as CommandResult.SetTimer).durationSeconds)

        val q3 = commandEngine.process("timer for 1 hour")
        assertTrue(q3 is CommandResult.SetTimer)
        assertEquals(3600L, (q3 as CommandResult.SetTimer).durationSeconds)

        val q4 = commandEngine.process("start a 1 minute 30 second timer")
        assertTrue(q4 is CommandResult.SetTimer)
        assertEquals(90L, (q4 as CommandResult.SetTimer).durationSeconds)
    }

    @Test
    fun setTimer_banglishQueries_routesToSetTimerWithAccurateSeconds() {
        val q1 = commandEngine.process("2 minute timer dao")
        assertTrue(q1 is CommandResult.SetTimer)
        assertEquals(120L, (q1 as CommandResult.SetTimer).durationSeconds)

        val q2 = commandEngine.process("30 second er timer dao")
        assertTrue(q2 is CommandResult.SetTimer)
        assertEquals(30L, (q2 as CommandResult.SetTimer).durationSeconds)

        val q3 = commandEngine.process("5 minute timer lagao")
        assertTrue(q3 is CommandResult.SetTimer)
        assertEquals(300L, (q3 as CommandResult.SetTimer).durationSeconds)

        val q4 = commandEngine.process("1 min timer")
        assertTrue(q4 is CommandResult.SetTimer)
        assertEquals(60L, (q4 as CommandResult.SetTimer).durationSeconds)
    }

    @Test
    fun setTimer_banglaQueriesWithBengaliNumerals_routesToSetTimerWithAccurateSeconds() {
        val q1 = commandEngine.process("২ মিনিটের টাইমার দাও")
        assertTrue(q1 is CommandResult.SetTimer)
        assertEquals(120L, (q1 as CommandResult.SetTimer).durationSeconds)

        val q2 = commandEngine.process("৩০ সেকেন্ড এর টাইমার দাও")
        assertTrue(q2 is CommandResult.SetTimer)
        assertEquals(30L, (q2 as CommandResult.SetTimer).durationSeconds)

        val q3 = commandEngine.process("১ মিনিট এর টাইমার সেট করো")
        assertTrue(q3 is CommandResult.SetTimer)
        assertEquals(60L, (q3 as CommandResult.SetTimer).durationSeconds)

        val q4 = commandEngine.process("১ ঘণ্টার টাইমার দাও")
        assertTrue(q4 is CommandResult.SetTimer)
        assertEquals(3600L, (q4 as CommandResult.SetTimer).durationSeconds)
    }

    @Test
    fun timerControls_pauseResumeCancelStatus_routesCorrectly() {
        // Pause
        assertEquals(CommandResult.PauseTimer, commandEngine.process("pause timer"))
        assertEquals(CommandResult.PauseTimer, commandEngine.process("timer thamao"))
        assertEquals(CommandResult.PauseTimer, commandEngine.process("টাইমার থামাও"))

        // Resume
        assertEquals(CommandResult.ResumeTimer, commandEngine.process("resume timer"))
        assertEquals(CommandResult.ResumeTimer, commandEngine.process("timer chalu koro"))
        assertEquals(CommandResult.ResumeTimer, commandEngine.process("টাইমার আবার চালু করো"))

        // Cancel
        assertEquals(CommandResult.CancelTimer, commandEngine.process("cancel timer"))
        assertEquals(CommandResult.CancelTimer, commandEngine.process("stop timer"))
        assertEquals(CommandResult.CancelTimer, commandEngine.process("timer bondho koro"))
        assertEquals(CommandResult.CancelTimer, commandEngine.process("টাইমার বাতিল করো"))

        // Status
        assertEquals(CommandResult.GetTimerStatus, commandEngine.process("timer status"))
        assertEquals(CommandResult.GetTimerStatus, commandEngine.process("how much time left on timer"))
        assertEquals(CommandResult.GetTimerStatus, commandEngine.process("timer koto baki ache"))
        assertEquals(CommandResult.GetTimerStatus, commandEngine.process("টাইমারে কত সময় বাকি আছে"))
    }

    @Test
    fun timerAmbiguousWithoutDuration_requestsClarification() {
        val ambiguousQueries = listOf("timer", "set timer", "start timer", "টাইমার দাও", "টাইমার সেট করো")
        for (q in ambiguousQueries) {
            val result = commandEngine.process(q)
            assertTrue("Expected clarification for timer query without duration: $q", result is CommandResult.Clarification)
            val question = (result as CommandResult.Clarification).question
            assertTrue(question.contains("How long"))
        }
    }

    // ---------------------------------------------------------
    // 21. Help Commands & Topic Help
    // ---------------------------------------------------------

    @Test
    fun generalHelp_includesBatteryFlashlightSettingsAndTimer() {
        val helpQueries = listOf("help", "what can you do", "sahajjo", "সাহায্য", "commands")
        for (q in helpQueries) {
            val result = commandEngine.process(q)
            assertTrue("Expected Reply for help: $q", result is CommandResult.Reply)
            val text = (result as CommandResult.Reply).text
            assertTrue("Help should mention Battery: $text", text.contains("Battery", ignoreCase = true))
            assertTrue("Help should mention Flashlight: $text", text.contains("Flashlight", ignoreCase = true))
            assertTrue("Help should mention Settings: $text", text.contains("Settings", ignoreCase = true))
            assertTrue("Help should mention Timer: $text", text.contains("Timer", ignoreCase = true))
        }
    }

    @Test
    fun topicSpecificHelp_returnsDetailedExamples() {
        val batteryHelp = commandEngine.process("help battery")
        assertTrue(batteryHelp is CommandResult.Reply)
        val bText = (batteryHelp as CommandResult.Reply).text
        assertTrue(bText.contains("Battery status", ignoreCase = true))
        assertTrue(bText.contains("Battery koto ache?"))

        val flashHelp = commandEngine.process("help torch")
        assertTrue(flashHelp is CommandResult.Reply)
        val fText = (flashHelp as CommandResult.Reply).text
        assertTrue(fText.contains("Flashlight"))
        assertTrue(fText.contains("Torch on koro"))

        val timerHelp = commandEngine.process("help timer")
        assertTrue(timerHelp is CommandResult.Reply)
        val tText = (timerHelp as CommandResult.Reply).text
        assertTrue(tText.contains("Timer"))
        assertTrue(tText.contains("2 minute timer dao"))

        val settingsHelp = commandEngine.process("help settings")
        assertTrue(settingsHelp is CommandResult.Reply)
        val sText = (settingsHelp as CommandResult.Reply).text
        assertTrue(sText.contains("Settings"))
        assertTrue(sText.contains("Wi-Fi settings kholo"))
    }
}