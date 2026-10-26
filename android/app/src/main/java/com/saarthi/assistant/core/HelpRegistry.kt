package com.saarthi.assistant.core

/**
 * Single, authoritative source of truth for SAARTHI's capabilities and command documentation.
 * Shared between CommandEngine (for verbal/text help queries) and the UI (for visual discovery).
 */
data class HelpCommandExample(
    val english: String,
    val banglish: String,
    val bangla: String,
    val description: String = ""
)

data class HelpCategory(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val examples: List<HelpCommandExample>
)

object HelpRegistry {

    val categories: List<HelpCategory> = listOf(
        HelpCategory(
            id = "battery",
            title = "Battery Status",
            description = "Check real-time device battery percentage and charging state offline with 0 permissions.",
            iconName = "Battery",
            examples = listOf(
                HelpCommandExample(
                    english = "What is my battery percentage?",
                    banglish = "Battery koto ache?",
                    bangla = "চার্জ কত আছে?",
                    description = "Reports exact battery level and charging status"
                ),
                HelpCommandExample(
                    english = "Is my phone charging?",
                    banglish = "Phone charge hocche ki?",
                    bangla = "ফোন কি চার্জ হচ্ছে?",
                    description = "Checks if connected to AC, USB, or wireless power"
                )
            )
        ),
        HelpCategory(
            id = "flashlight",
            title = "Flashlight Control",
            description = "Safely turn the physical camera torch LED on or off without opening the camera app.",
            iconName = "Flashlight",
            examples = listOf(
                HelpCommandExample(
                    english = "Turn on flashlight",
                    banglish = "Torch on koro",
                    bangla = "টর্চ জ্বালাও",
                    description = "Turns on camera flash LED"
                ),
                HelpCommandExample(
                    english = "Turn off flashlight",
                    banglish = "Flashlight off koro",
                    bangla = "ফ্ল্যাশলাইট বন্ধ করো",
                    description = "Turns off camera flash LED"
                )
            )
        ),
        HelpCategory(
            id = "timer",
            title = "Timer & Countdown",
            description = "Set, pause, resume, and cancel local countdown timers with audible completion alerts.",
            iconName = "Timer",
            examples = listOf(
                HelpCommandExample(
                    english = "Set a timer for 2 minutes",
                    banglish = "2 minute timer dao",
                    bangla = "২ মিনিটের টাইমার দাও",
                    description = "Starts a 2-minute countdown timer"
                ),
                HelpCommandExample(
                    english = "Set timer for 30 seconds",
                    banglish = "30 second er timer dao",
                    bangla = "৩০ সেকেন্ডের টাইমার দাও",
                    description = "Starts a 30-second countdown timer"
                ),
                HelpCommandExample(
                    english = "Cancel timer",
                    banglish = "Timer cancel koro",
                    bangla = "টাইমার বাতিল করো",
                    description = "Cancels active countdown timer"
                )
            )
        ),
        HelpCategory(
            id = "wifi",
            title = "Wi-Fi Settings",
            description = "Safe shortcut to the Android Wi-Fi panel or system connection settings.",
            iconName = "Wifi",
            examples = listOf(
                HelpCommandExample(
                    english = "Open Wi-Fi settings",
                    banglish = "Wi-Fi settings kholo",
                    bangla = "ওয়াইফাই সেটিংস খোলো",
                    description = "Opens system Wi-Fi connection panel"
                )
            )
        ),
        HelpCategory(
            id = "bluetooth",
            title = "Bluetooth Settings",
            description = "Safe shortcut to Android Bluetooth pairing and device management settings.",
            iconName = "Bluetooth",
            examples = listOf(
                HelpCommandExample(
                    english = "Open Bluetooth settings",
                    banglish = "Bluetooth settings kholo",
                    bangla = "ব্লুটুথ সেটিংস খোলো",
                    description = "Opens system Bluetooth management screen"
                )
            )
        ),
        HelpCategory(
            id = "mobile_data",
            title = "Mobile Network Settings",
            description = "Direct shortcut to mobile network and SIM data settings.",
            iconName = "MobileData",
            examples = listOf(
                HelpCommandExample(
                    english = "Open mobile network settings",
                    banglish = "Mobile network settings kholo",
                    bangla = "মোবাইল নেটওয়ার্ক সেটিংস খোলো",
                    description = "Opens mobile carrier & data screen"
                )
            )
        ),
        HelpCategory(
            id = "sound",
            title = "Sound & Volume",
            description = "Direct shortcut to Android sound, ringtone, and media volume panel.",
            iconName = "Volume",
            examples = listOf(
                HelpCommandExample(
                    english = "Open volume settings",
                    banglish = "Volume settings kholo",
                    bangla = "ভলিউম সেটিংস খোলো",
                    description = "Opens system sound & volume panel"
                )
            )
        ),
        HelpCategory(
            id = "brightness",
            title = "Display & Brightness",
            description = "Direct shortcut to Android display and brightness controls without invasive permissions.",
            iconName = "Brightness",
            examples = listOf(
                HelpCommandExample(
                    english = "Open brightness settings",
                    banglish = "Brightness settings kholo",
                    bangla = "ব্রাইটনেস সেটিংস খোলো",
                    description = "Opens display & brightness screen"
                )
            )
        ),
        HelpCategory(
            id = "calculator",
            title = "Calculator & Math",
            description = "Solve mathematical expressions offline or launch the native phone calculator.",
            iconName = "Calculator",
            examples = listOf(
                HelpCommandExample(
                    english = "Calculate 45 plus 17",
                    banglish = "45 plus 17 koto",
                    bangla = "১২ গুণ ৫ কত",
                    description = "Evaluates math using on-device recursive parser"
                ),
                HelpCommandExample(
                    english = "Open calculator",
                    banglish = "Calculator kholo",
                    bangla = "ক্যালকুলেটর খোলো",
                    description = "Launches the installed native calculator"
                )
            )
        ),
        HelpCategory(
            id = "time_date",
            title = "Time & Date",
            description = "Instant offline time and calendar date queries in English, Bangla, and Banglish.",
            iconName = "Clock",
            examples = listOf(
                HelpCommandExample(
                    english = "What time is it?",
                    banglish = "Shomoy koto?",
                    bangla = "কয়টা বাজে?",
                    description = "Announces current device time"
                ),
                HelpCommandExample(
                    english = "What is today's date?",
                    banglish = "Ajker date koto?",
                    bangla = "আজ কত তারিখ?",
                    description = "Announces current calendar date"
                )
            )
        ),
        HelpCategory(
            id = "apps",
            title = "Native App Launchers",
            description = "Multi-tier app launcher for Camera, Clock, Calendar, and Settings with OEM fallback.",
            iconName = "Apps",
            examples = listOf(
                HelpCommandExample(
                    english = "Open camera",
                    banglish = "Camera kholo",
                    bangla = "ক্যামেরা খোলো",
                    description = "Launches native camera viewfinder"
                ),
                HelpCommandExample(
                    english = "Open clock",
                    banglish = "Ghori kholo",
                    bangla = "ঘড়ি খোলো",
                    description = "Launches native clock / alarms app"
                ),
                HelpCommandExample(
                    english = "Open calendar",
                    banglish = "Calendar kholo",
                    bangla = "ক্যালেন্ডার খোলো",
                    description = "Launches system calendar"
                ),
                HelpCommandExample(
                    english = "Open settings",
                    banglish = "Settings kholo",
                    bangla = "সেটিংস খোলো",
                    description = "Launches device system settings"
                )
            )
        ),
        HelpCategory(
            id = "assistant",
            title = "Identity & Conversation Control",
            description = "Interact naturally with SAARTHI and control speech or microphone listening.",
            iconName = "Emblem",
            examples = listOf(
                HelpCommandExample(
                    english = "Who are you?",
                    banglish = "Tumi ke?",
                    bangla = "তুমি কে?",
                    description = "Assistant identity response"
                ),
                HelpCommandExample(
                    english = "Stop speaking",
                    banglish = "Thamo",
                    bangla = "থামো",
                    description = "Immediately stops TTS audio output"
                )
            )
        )
    )

    /**
     * Concise overview string formatted for verbal or conversational text output.
     */
    fun getOverviewSpoken(): String {
        return "Here is what I can do offline: check battery level, control flashlight (on/off), " +
                "open settings shortcuts (Wi-Fi, Bluetooth, mobile network, volume, brightness), " +
                "set timers (e.g. '2 minute timer dao'), solve math ('45 plus 17'), " +
                "tell the time and date, open apps (camera, clock, calendar, calculator, settings), " +
                "and manage conversations. Try asking: 'battery koto', 'torch on', or 'wifi settings kholo'."
    }

    /**
     * Targeted help for a specific keyword/category (e.g., 'help battery', 'timer help').
     */
    fun getTopicHelp(topic: String): String? {
        val lower = topic.lowercase().trim()
        if (lower == "settings" || lower == "setting" || lower == "system" || lower == "controls") {
            return "Settings: SAARTHI provides safe shortcuts for Wi-Fi, Bluetooth, Mobile Network, Sound & Volume, and Display Brightness. Try saying: 'Wi-Fi settings kholo', 'Bluetooth settings kholo', or 'Volume settings kholo'."
        }
        val cat = categories.firstOrNull { c ->
            c.id == lower || c.title.lowercase().contains(lower) ||
                    (lower == "torch" && c.id == "flashlight") ||
                    (lower == "flashlight" && c.id == "flashlight") ||
                    (lower == "battery" && c.id == "battery") ||
                    (lower == "timer" && c.id == "timer") ||
                    (lower == "volume" && c.id == "sound") ||
                    (lower == "display" && c.id == "brightness") ||
                    (lower == "math" && c.id == "calculator") ||
                    (lower == "time" && c.id == "time_date") ||
                    (lower == "date" && c.id == "time_date")
        } ?: return null

        val sample = cat.examples.firstOrNull() ?: return null
        return "${cat.title}: ${cat.description} Try saying: '${sample.english}', '${sample.banglish}', or '${sample.bangla}'."
    }
}
