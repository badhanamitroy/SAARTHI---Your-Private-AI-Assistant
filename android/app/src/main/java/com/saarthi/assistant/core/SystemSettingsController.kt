package com.saarthi.assistant.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Types of safe device settings shortcuts supported by SAARTHI.
 */
enum class SystemSettingType(val displayName: String) {
    WIFI("Wi-Fi Settings"),
    BLUETOOTH("Bluetooth Settings"),
    MOBILE_DATA("Mobile Network Settings"),
    SOUND("Sound & Volume Settings"),
    BRIGHTNESS("Display & Brightness Settings")
}

/**
 * Decoupled interface for opening system settings.
 * Allows pure unit testing without Android hardware.
 */
interface SystemSettingsLauncher {
    fun openSetting(type: SystemSettingType): ExecutionContract
}

/**
 * Android framework implementation of system settings launcher.
 * Uses official Android System Panels (Settings.Panel.*) on API 29+ with fallback
 * to standard Settings intents, respecting Android platform restrictions with zero permissions.
 */
class AndroidSystemSettingsLauncher(private val context: Context) : SystemSettingsLauncher {

    override fun openSetting(type: SystemSettingType): ExecutionContract {
        val intent = buildIntentForSetting(type)

        // Ensure NEW_TASK flag is present when launching from non-Activity or Service contexts
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            val pm = context.packageManager
            val resolved = intent.resolveActivity(pm)
            if (resolved == null) {
                // If specific panel failed, try generic fallback
                val fallbackIntent = buildFallbackIntent(type)
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (fallbackIntent.resolveActivity(pm) != null) {
                    context.startActivity(fallbackIntent)
                    return buildSuccessContract(type)
                }

                return ExecutionContract.Failed(
                    spokenFeedback = "Could not open ${type.displayName} on this device.",
                    displayFeedback = "${type.displayName} not supported",
                    errorType = ExecutionFailureType.ACTIVITY_NOT_FOUND
                )
            }

            context.startActivity(intent)
            buildSuccessContract(type)
        } catch (e: ActivityNotFoundException) {
            ExecutionContract.Failed(
                spokenFeedback = "No settings application was found to handle ${type.displayName}.",
                displayFeedback = "Activity not found: ${type.displayName}",
                errorType = ExecutionFailureType.ACTIVITY_NOT_FOUND,
                exception = e
            )
        } catch (e: SecurityException) {
            ExecutionContract.Failed(
                spokenFeedback = "Access to ${type.displayName} was restricted by device policy.",
                displayFeedback = "Security restriction: ${type.displayName}",
                errorType = ExecutionFailureType.SECURITY_RESTRICTION,
                exception = e
            )
        } catch (e: Exception) {
            ExecutionContract.Failed(
                spokenFeedback = "Failed to open ${type.displayName}: ${e.localizedMessage ?: "system error"}.",
                displayFeedback = "Launch error: ${type.displayName}",
                errorType = ExecutionFailureType.UNKNOWN,
                exception = e
            )
        }
    }

    companion object {
        fun buildIntentForSetting(type: SystemSettingType, sdkInt: Int = Build.VERSION.SDK_INT): Intent {
            return when (type) {
                SystemSettingType.WIFI -> {
                    if (sdkInt >= Build.VERSION_CODES.Q) {
                        Intent(Settings.Panel.ACTION_WIFI)
                    } else {
                        Intent(Settings.ACTION_WIFI_SETTINGS)
                    }
                }

                SystemSettingType.BLUETOOTH -> {
                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                }

                SystemSettingType.MOBILE_DATA -> {
                    if (sdkInt >= Build.VERSION_CODES.Q) {
                        Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                    } else {
                        Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
                    }
                }

                SystemSettingType.SOUND -> {
                    if (sdkInt >= Build.VERSION_CODES.Q) {
                        Intent(Settings.Panel.ACTION_VOLUME)
                    } else {
                        Intent(Settings.ACTION_SOUND_SETTINGS)
                    }
                }

                SystemSettingType.BRIGHTNESS -> {
                    Intent(Settings.ACTION_DISPLAY_SETTINGS)
                }
            }
        }

        fun buildFallbackIntent(type: SystemSettingType): Intent {
            return when (type) {
                SystemSettingType.WIFI -> Intent(Settings.ACTION_WIFI_SETTINGS)
                SystemSettingType.BLUETOOTH -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                SystemSettingType.MOBILE_DATA -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
                SystemSettingType.SOUND -> Intent(Settings.ACTION_SOUND_SETTINGS)
                SystemSettingType.BRIGHTNESS -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            }
        }

        fun buildSuccessContract(type: SystemSettingType): ExecutionContract.Succeeded {
            val (spoken, display) = when (type) {
                SystemSettingType.WIFI -> Pair(
                    "Opening Wi-Fi settings. You can switch Wi-Fi networks there.",
                    "Wi-Fi Settings opened"
                )
                SystemSettingType.BLUETOOTH -> Pair(
                    "Opening Bluetooth settings. You can manage connected devices there.",
                    "Bluetooth Settings opened"
                )
                SystemSettingType.MOBILE_DATA -> Pair(
                    "Opening mobile network settings. Please adjust mobile data there.",
                    "Mobile Network opened"
                )
                SystemSettingType.SOUND -> Pair(
                    "Opening volume and sound settings.",
                    "Volume Settings opened"
                )
                SystemSettingType.BRIGHTNESS -> Pair(
                    "Opening display and brightness settings.",
                    "Display Settings opened"
                )
            }
            return ExecutionContract.Succeeded(
                spokenFeedback = spoken,
                displayFeedback = display
            )
        }
    }
}

/**
 * Unified controller for launching device settings shortcuts.
 * Supports dependency injection for automated unit testing.
 */
object SystemSettingsController {

    var customLauncher: SystemSettingsLauncher? = null

    fun setLauncherForTesting(launcher: SystemSettingsLauncher?) {
        customLauncher = launcher
    }

    fun getActionForSetting(type: SystemSettingType, sdkInt: Int = Build.VERSION.SDK_INT): String {
        return when (type) {
            SystemSettingType.WIFI -> {
                if (sdkInt >= Build.VERSION_CODES.Q) {
                    "android.settings.panel.action.WIFI"
                } else {
                    Settings.ACTION_WIFI_SETTINGS
                }
            }
            SystemSettingType.BLUETOOTH -> {
                Settings.ACTION_BLUETOOTH_SETTINGS
            }
            SystemSettingType.MOBILE_DATA -> {
                if (sdkInt >= Build.VERSION_CODES.Q) {
                    "android.settings.panel.action.INTERNET_CONNECTIVITY"
                } else {
                    Settings.ACTION_DATA_ROAMING_SETTINGS
                }
            }
            SystemSettingType.SOUND -> {
                if (sdkInt >= Build.VERSION_CODES.Q) {
                    "android.settings.panel.action.VOLUME"
                } else {
                    Settings.ACTION_SOUND_SETTINGS
                }
            }
            SystemSettingType.BRIGHTNESS -> {
                Settings.ACTION_DISPLAY_SETTINGS
            }
        }
    }

    fun buildSettingsIntent(type: SystemSettingType, sdkInt: Int = Build.VERSION.SDK_INT): Intent {
        val intent = Intent(getActionForSetting(type, sdkInt))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    fun openSetting(context: Context?, type: SystemSettingType): ExecutionContract {
        val launcher = customLauncher ?: (context?.let { AndroidSystemSettingsLauncher(it) }
            ?: return ExecutionContract.Failed(
                spokenFeedback = "Context is required to launch settings.",
                displayFeedback = "Context missing",
                errorType = ExecutionFailureType.EVALUATION_ERROR
            ))
        return launcher.openSetting(type)
    }
}
