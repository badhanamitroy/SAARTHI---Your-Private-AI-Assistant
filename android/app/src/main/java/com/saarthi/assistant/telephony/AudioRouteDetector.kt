package com.saarthi.assistant.telephony

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/**
 * Interface for detecting current audio routing hardware (earbuds, headphones, speaker).
 */
interface AudioRouteDetector {
    fun getCurrentAudioRoute(): AudioRoute

    fun isEarbudOrHeadsetConnected(): Boolean {
        val route = getCurrentAudioRoute()
        return route == AudioRoute.WIRED_HEADSET || route == AudioRoute.BLUETOOTH_EARBUD
    }
}

/**
 * Production implementation using Android AudioManager and AudioDeviceInfo.
 */
class AndroidAudioRouteDetector(private val context: Context) : AudioRouteDetector {

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    override fun getCurrentAudioRoute(): AudioRoute {
        val am = audioManager ?: return AudioRoute.SPEAKER

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var hasBluetooth = false
            var hasWired = false

            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        hasBluetooth = true
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        hasWired = true
                    }
                }
            }

            // Check BLE Headset on API 31+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                for (device in devices) {
                    if (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                    ) {
                        hasBluetooth = true
                    }
                }
            }

            if (hasBluetooth) return AudioRoute.BLUETOOTH_EARBUD
            if (hasWired) return AudioRoute.WIRED_HEADSET
        }

        // Fallback for older APIs / devices
        @Suppress("DEPRECATION")
        if (am.isBluetoothA2dpOn || am.isBluetoothScoOn) {
            return AudioRoute.BLUETOOTH_EARBUD
        }

        @Suppress("DEPRECATION")
        if (am.isWiredHeadsetOn) {
            return AudioRoute.WIRED_HEADSET
        }

        return AudioRoute.SPEAKER
    }
}

/**
 * Test fake for unit testing.
 */
class FakeAudioRouteDetector(
    var activeRoute: AudioRoute = AudioRoute.SPEAKER
) : AudioRouteDetector {
    override fun getCurrentAudioRoute(): AudioRoute = activeRoute
}

