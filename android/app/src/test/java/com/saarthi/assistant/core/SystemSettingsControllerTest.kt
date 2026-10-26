package com.saarthi.assistant.core

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SystemSettingsControllerTest {

    @Before
    fun setUp() {
        SystemSettingsController.customLauncher = null
    }

    @After
    fun tearDown() {
        SystemSettingsController.customLauncher = null
    }

    // --- Action String Resolution Tests ---

    @Test
    fun getActionForSetting_wifi_onApi29Plus_usesPanel() {
        val action = SystemSettingsController.getActionForSetting(SystemSettingType.WIFI, sdkInt = 29)
        assertEquals("android.settings.panel.action.WIFI", action)
    }

    @Test
    fun getActionForSetting_wifi_belowApi29_usesStandardWifiSettings() {
        val action = SystemSettingsController.getActionForSetting(SystemSettingType.WIFI, sdkInt = 28)
        assertEquals(Settings.ACTION_WIFI_SETTINGS, action)
    }

    @Test
    fun getActionForSetting_bluetooth_usesBluetoothSettings() {
        val action = SystemSettingsController.getActionForSetting(SystemSettingType.BLUETOOTH, sdkInt = 34)
        assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS, action)
    }

    @Test
    fun getActionForSetting_mobileData_onApi29Plus_usesInternetConnectivityPanel() {
        val action = SystemSettingsController.getActionForSetting(SystemSettingType.MOBILE_DATA, sdkInt = 29)
        assertEquals("android.settings.panel.action.INTERNET_CONNECTIVITY", action)
    }

    @Test
    fun getActionForSetting_mobileData_belowApi29_usesDataRoamingSettings() {
        val action = SystemSettingsController.getActionForSetting(SystemSettingType.MOBILE_DATA, sdkInt = 28)
        assertEquals(Settings.ACTION_DATA_ROAMING_SETTINGS, action)
    }

    @Test
    fun getActionForSetting_sound_onApi29Plus_usesVolumePanel() {
        val action = SystemSettingsController.getActionForSetting(SystemSettingType.SOUND, sdkInt = 29)
        assertEquals("android.settings.panel.action.VOLUME", action)
    }

    @Test
    fun getActionForSetting_sound_belowApi29_usesSoundSettings() {
        val action = SystemSettingsController.getActionForSetting(SystemSettingType.SOUND, sdkInt = 28)
        assertEquals(Settings.ACTION_SOUND_SETTINGS, action)
    }

    @Test
    fun getActionForSetting_brightness_usesDisplaySettings() {
        val action = SystemSettingsController.getActionForSetting(SystemSettingType.BRIGHTNESS, sdkInt = 34)
        assertEquals(Settings.ACTION_DISPLAY_SETTINGS, action)
    }

    // --- Execution Contract Tests with Custom Launcher ---

    @Test
    fun openSetting_whenLaunchSucceeds_returnsHonestSucceededContract() {
        SystemSettingsController.customLauncher = object : SystemSettingsLauncher {
            override fun openSetting(type: SystemSettingType): ExecutionContract {
                return AndroidSystemSettingsLauncher.buildSuccessContract(type)
            }
        }

        val contract = SystemSettingsController.openSetting(null, SystemSettingType.WIFI)
        assertTrue(contract is ExecutionContract.Succeeded)
        val succeeded = contract as ExecutionContract.Succeeded
        assertTrue(succeeded.spokenFeedback.contains("Wi-Fi settings"))
        assertTrue(succeeded.displayFeedback.contains("Wi-Fi Settings opened"))
    }

    @Test
    fun openSetting_whenLaunchFailsWithNotFound_returnsHonestFailedContract() {
        SystemSettingsController.customLauncher = object : SystemSettingsLauncher {
            override fun openSetting(type: SystemSettingType): ExecutionContract {
                return ExecutionContract.Failed(
                    spokenFeedback = "No settings application was found to handle ${type.displayName}.",
                    displayFeedback = "Activity not found: ${type.displayName}",
                    errorType = ExecutionFailureType.ACTIVITY_NOT_FOUND,
                    exception = ActivityNotFoundException("Not found")
                )
            }
        }

        val contract = SystemSettingsController.openSetting(null, SystemSettingType.BLUETOOTH)
        assertTrue(contract is ExecutionContract.Failed)
        val failed = contract as ExecutionContract.Failed
        assertTrue(failed.spokenFeedback.contains("Bluetooth Settings"))
        assertEquals(ExecutionFailureType.ACTIVITY_NOT_FOUND, failed.errorType)
    }

    @Test
    fun openSetting_whenLaunchFailsWithSecurityRestriction_returnsHonestFailedContract() {
        SystemSettingsController.customLauncher = object : SystemSettingsLauncher {
            override fun openSetting(type: SystemSettingType): ExecutionContract {
                return ExecutionContract.Failed(
                    spokenFeedback = "Access to ${type.displayName} was restricted by device policy.",
                    displayFeedback = "Security restriction: ${type.displayName}",
                    errorType = ExecutionFailureType.SECURITY_RESTRICTION,
                    exception = SecurityException("Restricted")
                )
            }
        }

        val contract = SystemSettingsController.openSetting(null, SystemSettingType.MOBILE_DATA)
        assertTrue(contract is ExecutionContract.Failed)
        val failed = contract as ExecutionContract.Failed
        assertTrue(failed.spokenFeedback.contains("restricted by device policy"))
        assertEquals(ExecutionFailureType.SECURITY_RESTRICTION, failed.errorType)
    }

    @Test
    fun openSetting_withoutContextOrCustomLauncher_returnsFailedContract() {
        val contract = SystemSettingsController.openSetting(null, SystemSettingType.SOUND)
        assertTrue(contract is ExecutionContract.Failed)
        val failed = contract as ExecutionContract.Failed
        assertTrue(failed.spokenFeedback.contains("Context is required"))
    }

    @Test
    fun systemSettingType_allTypesHaveCleanDisplayNames() {
        SystemSettingType.values().forEach { type ->
            assertTrue(type.displayName.isNotBlank())
            assertTrue(type.name.isNotBlank())
        }
    }
}
