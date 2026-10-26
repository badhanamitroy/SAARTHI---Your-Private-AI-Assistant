package com.saarthi.assistant.core

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceHardwareControllerTest {

    @Before
    fun setUp() {
        DeviceHardwareController.batteryProvider = null
        DeviceHardwareController.flashlightController = null
    }

    @After
    fun tearDown() {
        DeviceHardwareController.batteryProvider = null
        DeviceHardwareController.flashlightController = null
    }

    // --- Battery Contract & State Tests ---

    @Test
    fun batteryContract_chargingStateWithPercentage_returnsHonestSucceededContract() {
        val status = BatteryStatusResult(
            percentage = 84,
            chargingState = BatteryChargingState.CHARGING,
            pluggedSource = PluggedSource.AC
        )
        val contract = DeviceHardwareController.formatBatteryContract(status)

        assertTrue(contract is ExecutionContract.Succeeded)
        val succeeded = contract as ExecutionContract.Succeeded
        assertTrue(succeeded.spokenFeedback.contains("84%"))
        assertTrue(succeeded.spokenFeedback.contains("charging"))
        assertTrue(succeeded.displayFeedback.contains("84%"))
        assertTrue(succeeded.displayFeedback.contains("Charging (AC)"))
    }

    @Test
    fun batteryContract_fullyCharged_reportsFull() {
        val status = BatteryStatusResult(
            percentage = 100,
            chargingState = BatteryChargingState.FULL,
            pluggedSource = PluggedSource.USB
        )
        val contract = DeviceHardwareController.formatBatteryContract(status)

        assertTrue(contract is ExecutionContract.Succeeded)
        val succeeded = contract as ExecutionContract.Succeeded
        assertTrue(succeeded.spokenFeedback.contains("fully charged at 100%"))
        assertTrue(succeeded.displayFeedback.contains("100% (Full)"))
    }

    @Test
    fun batteryContract_discharging_reportsPercentageOnly() {
        val status = BatteryStatusResult(
            percentage = 52,
            chargingState = BatteryChargingState.DISCHARGING,
            pluggedSource = PluggedSource.NONE
        )
        val contract = DeviceHardwareController.formatBatteryContract(status)

        assertTrue(contract is ExecutionContract.Succeeded)
        val succeeded = contract as ExecutionContract.Succeeded
        assertEquals("Your battery is at 52%.", succeeded.spokenFeedback)
        assertEquals("Battery: 52%", succeeded.displayFeedback)
    }

    @Test
    fun batteryContract_notCharging_reportsNotCharging() {
        val status = BatteryStatusResult(
            percentage = 75,
            chargingState = BatteryChargingState.NOT_CHARGING,
            pluggedSource = PluggedSource.USB
        )
        val contract = DeviceHardwareController.formatBatteryContract(status)

        assertTrue(contract is ExecutionContract.Succeeded)
        val succeeded = contract as ExecutionContract.Succeeded
        assertTrue(succeeded.spokenFeedback.contains("75% and not charging"))
        assertTrue(succeeded.displayFeedback.contains("Not charging"))
    }

    @Test
    fun batteryContract_indeterminatePercentageWhileCharging_honestFeedback() {
        val status = BatteryStatusResult(
            percentage = null,
            chargingState = BatteryChargingState.CHARGING,
            pluggedSource = PluggedSource.USB
        )
        val contract = DeviceHardwareController.formatBatteryContract(status)

        assertTrue(contract is ExecutionContract.Succeeded)
        val succeeded = contract as ExecutionContract.Succeeded
        assertTrue(succeeded.spokenFeedback.contains("currently charging"))
        assertTrue(succeeded.spokenFeedback.contains("percentage could not be determined"))
        assertTrue(succeeded.displayFeedback.contains("Percentage unknown"))
    }

    @Test
    fun batteryContract_unknownPercentageAndStatus_returnsHardwareUnavailable() {
        val status = BatteryStatusResult(
            percentage = null,
            chargingState = BatteryChargingState.UNKNOWN,
            pluggedSource = PluggedSource.NONE
        )
        val contract = DeviceHardwareController.formatBatteryContract(status)

        assertTrue(contract is ExecutionContract.Failed)
        val failed = contract as ExecutionContract.Failed
        assertEquals(ExecutionFailureType.HARDWARE_UNAVAILABLE, failed.errorType)
        assertTrue(failed.spokenFeedback.contains("unavailable"))
        assertEquals("Battery status unavailable", failed.displayFeedback)
    }

    @Test
    fun batteryContract_validPercentageWithUnknownStatus_reportsPercentage() {
        val status = BatteryStatusResult(
            percentage = 68,
            chargingState = BatteryChargingState.UNKNOWN,
            pluggedSource = PluggedSource.NONE
        )
        val contract = DeviceHardwareController.formatBatteryContract(status)

        assertTrue(contract is ExecutionContract.Succeeded)
        val succeeded = contract as ExecutionContract.Succeeded
        assertEquals("Your battery is at 68%.", succeeded.spokenFeedback)
    }

    // --- Flashlight Hardware & Control Tests ---

    private class FakeFlashlightController(
        override var isFlashAvailable: Boolean = true,
        override var isFlashlightOn: Boolean = false,
        var throwExceptionOnAction: Exception? = null
    ) : FlashlightController {

        override fun setFlashlight(enabled: Boolean): ExecutionContract {
            if (!isFlashAvailable) {
                return ExecutionContract.Failed(
                    spokenFeedback = "This device does not have a flashlight.",
                    displayFeedback = "No flashlight hardware",
                    errorType = ExecutionFailureType.HARDWARE_UNAVAILABLE
                )
            }

            throwExceptionOnAction?.let { ex ->
                return when {
                    ex.message?.contains("in use") == true -> {
                        ExecutionContract.Failed(
                            spokenFeedback = "Flashlight is unavailable because the camera is currently in use.",
                            displayFeedback = "Camera in use",
                            errorType = ExecutionFailureType.HARDWARE_BUSY,
                            exception = ex
                        )
                    }
                    ex is SecurityException -> {
                        ExecutionContract.Failed(
                            spokenFeedback = "Permission restriction prevented controlling the flashlight.",
                            displayFeedback = "Security restriction: torch",
                            errorType = ExecutionFailureType.SECURITY_RESTRICTION,
                            exception = ex
                        )
                    }
                    else -> {
                        ExecutionContract.Failed(
                            spokenFeedback = "Could not control flashlight: ${ex.message}",
                            displayFeedback = "Flashlight error",
                            errorType = ExecutionFailureType.HARDWARE_UNAVAILABLE,
                            exception = ex
                        )
                    }
                }
            }

            isFlashlightOn = enabled
            val spoken = if (enabled) "Flashlight turned on." else "Flashlight turned off."
            val display = if (enabled) "Flashlight ON" else "Flashlight OFF"
            return ExecutionContract.Succeeded(
                spokenFeedback = spoken,
                displayFeedback = display
            )
        }

        override fun toggleFlashlight(): ExecutionContract {
            return setFlashlight(!isFlashlightOn)
        }
    }

    @Test
    fun flashlight_hardwareAbsent_returnsHardwareUnavailable() {
        val fake = FakeFlashlightController(isFlashAvailable = false)
        DeviceHardwareController.flashlightController = fake

        val contract = fake.setFlashlight(enabled = true)

        assertTrue(contract is ExecutionContract.Failed)
        val failed = contract as ExecutionContract.Failed
        assertEquals(ExecutionFailureType.HARDWARE_UNAVAILABLE, failed.errorType)
        assertTrue(failed.spokenFeedback.contains("does not have a flashlight"))
        assertFalse("Flashlight state must remain off when hardware absent", fake.isFlashlightOn)
    }

    @Test
    fun flashlight_turnOn_succeedsAndUpdatesState() {
        val fake = FakeFlashlightController(isFlashAvailable = true, isFlashlightOn = false)
        DeviceHardwareController.flashlightController = fake

        val contract = fake.setFlashlight(enabled = true)

        assertTrue(contract is ExecutionContract.Succeeded)
        val succeeded = contract as ExecutionContract.Succeeded
        assertEquals("Flashlight turned on.", succeeded.spokenFeedback)
        assertEquals("Flashlight ON", succeeded.displayFeedback)
        assertTrue(fake.isFlashlightOn)
    }

    @Test
    fun flashlight_turnOff_succeedsAndUpdatesState() {
        val fake = FakeFlashlightController(isFlashAvailable = true, isFlashlightOn = true)
        DeviceHardwareController.flashlightController = fake

        val contract = fake.setFlashlight(enabled = false)

        assertTrue(contract is ExecutionContract.Succeeded)
        val succeeded = contract as ExecutionContract.Succeeded
        assertEquals("Flashlight turned off.", succeeded.spokenFeedback)
        assertEquals("Flashlight OFF", succeeded.displayFeedback)
        assertFalse(fake.isFlashlightOn)
    }

    @Test
    fun flashlight_toggle_invertsActiveState() {
        val fake = FakeFlashlightController(isFlashAvailable = true, isFlashlightOn = false)
        DeviceHardwareController.flashlightController = fake

        // Toggle 1: OFF -> ON
        val contract1 = fake.toggleFlashlight()
        assertTrue(contract1 is ExecutionContract.Succeeded)
        assertTrue(fake.isFlashlightOn)

        // Toggle 2: ON -> OFF
        val contract2 = fake.toggleFlashlight()
        assertTrue(contract2 is ExecutionContract.Succeeded)
        assertFalse(fake.isFlashlightOn)
    }

    @Test
    fun flashlight_cameraContention_returnsHardwareBusy() {
        val fake = FakeFlashlightController(
            isFlashAvailable = true,
            isFlashlightOn = false,
            throwExceptionOnAction = RuntimeException("Camera is in use by another app")
        )
        DeviceHardwareController.flashlightController = fake

        val contract = fake.setFlashlight(enabled = true)

        assertTrue(contract is ExecutionContract.Failed)
        val failed = contract as ExecutionContract.Failed
        assertEquals(ExecutionFailureType.HARDWARE_BUSY, failed.errorType)
        assertTrue(failed.spokenFeedback.contains("camera is currently in use"))
        assertFalse("Flashlight state must not be true when action fails", fake.isFlashlightOn)
    }

    @Test
    fun flashlight_securityException_returnsSecurityRestriction() {
        val fake = FakeFlashlightController(
            isFlashAvailable = true,
            isFlashlightOn = false,
            throwExceptionOnAction = SecurityException("Torch policy disabled")
        )
        DeviceHardwareController.flashlightController = fake

        val contract = fake.setFlashlight(enabled = true)

        assertTrue(contract is ExecutionContract.Failed)
        val failed = contract as ExecutionContract.Failed
        assertEquals(ExecutionFailureType.SECURITY_RESTRICTION, failed.errorType)
        assertTrue(failed.spokenFeedback.contains("Permission restriction"))
        assertFalse(fake.isFlashlightOn)
    }

    @Test
    fun flashlight_neverClaimsSuccessWhenFailureOccurs() {
        val fake = FakeFlashlightController(
            isFlashAvailable = true,
            isFlashlightOn = false,
            throwExceptionOnAction = RuntimeException("Internal camera error")
        )

        val contract = fake.setFlashlight(enabled = true)

        assertFalse(contract is ExecutionContract.Succeeded)
        assertTrue(contract is ExecutionContract.Failed)
        assertFalse(fake.isFlashlightOn)
    }
}
