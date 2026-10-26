package com.saarthi.assistant.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Structured state of device battery.
 */
data class BatteryStatusResult(
    val percentage: Int?,
    val chargingState: BatteryChargingState,
    val pluggedSource: PluggedSource = PluggedSource.NONE
)

enum class BatteryChargingState {
    CHARGING,
    DISCHARGING,
    FULL,
    NOT_CHARGING,
    UNKNOWN
}

enum class PluggedSource {
    AC,
    USB,
    WIRELESS,
    NONE
}

/**
 * Decoupled interface for querying battery metrics.
 * Allows pure unit testing without Android hardware.
 */
interface BatteryInfoProvider {
    fun getBatteryStatus(): BatteryStatusResult
}

/**
 * Decoupled interface for controlling camera flashlight torch.
 * Allows pure unit testing without physical camera hardware.
 */
interface FlashlightController {
    val isFlashAvailable: Boolean
    val isFlashlightOn: Boolean
    fun setFlashlight(enabled: Boolean): ExecutionContract
    fun toggleFlashlight(): ExecutionContract
}

/**
 * Android framework implementation of BatteryInfoProvider.
 * Uses sticky Intent.ACTION_BATTERY_CHANGED broadcast and BatteryManager property fallback.
 */
class AndroidBatteryInfoProvider(private val context: Context) : BatteryInfoProvider {

    override fun getBatteryStatus(): BatteryStatusResult {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter)

            if (intent != null) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val percentage = if (level >= 0 && scale > 0) {
                    ((level.toFloat() / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
                } else {
                    null
                }

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val chargingState = when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> BatteryChargingState.CHARGING
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryChargingState.DISCHARGING
                    BatteryManager.BATTERY_STATUS_FULL -> BatteryChargingState.FULL
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryChargingState.NOT_CHARGING
                    else -> BatteryChargingState.UNKNOWN
                }

                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val pluggedSource = when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_AC -> PluggedSource.AC
                    BatteryManager.BATTERY_PLUGGED_USB -> PluggedSource.USB
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> PluggedSource.WIRELESS
                    else -> PluggedSource.NONE
                }

                BatteryStatusResult(
                    percentage = percentage,
                    chargingState = chargingState,
                    pluggedSource = pluggedSource
                )
            } else {
                // Fallback to BatteryManager capacity property if available
                queryBatteryManagerFallback()
            }
        } catch (e: Exception) {
            queryBatteryManagerFallback()
        }
    }

    private fun queryBatteryManagerFallback(): BatteryStatusResult {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val capacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val validPercentage = if (capacity != null && capacity in 0..100) capacity else null

            val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
            } else {
                false
            }

            BatteryStatusResult(
                percentage = validPercentage,
                chargingState = if (isCharging) BatteryChargingState.CHARGING else BatteryChargingState.UNKNOWN,
                pluggedSource = PluggedSource.NONE
            )
        } catch (e: Exception) {
            BatteryStatusResult(
                percentage = null,
                chargingState = BatteryChargingState.UNKNOWN,
                pluggedSource = PluggedSource.NONE
            )
        }
    }
}

/**
 * Android framework implementation of FlashlightController using CameraManager.
 * Does not require CAMERA permission for setTorchMode on API 23+.
 */
class AndroidFlashlightController(private val context: Context) : FlashlightController {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val hasFlashFeature: Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

    private var targetCameraId: String? = null
    private var _isFlashlightOn: Boolean = false

    override val isFlashAvailable: Boolean
        get() = hasFlashFeature && cameraManager != null && targetCameraId != null

    override val isFlashlightOn: Boolean
        get() = _isFlashlightOn

    init {
        findCameraWithFlash()
        registerTorchCallback()
    }

    private fun findCameraWithFlash() {
        if (!hasFlashFeature || cameraManager == null) {
            targetCameraId = null
            return
        }

        try {
            var fallbackId: String? = null
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (flashAvailable) {
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        targetCameraId = id
                        return
                    }
                    if (fallbackId == null) {
                        fallbackId = id
                    }
                }
            }
            targetCameraId = fallbackId
        } catch (e: Exception) {
            targetCameraId = null
        }
    }

    private fun registerTorchCallback() {
        val cm = cameraManager ?: return
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                super.onTorchModeChanged(cameraId, enabled)
                if (targetCameraId == null || cameraId == targetCameraId) {
                    _isFlashlightOn = enabled
                }
            }

            override fun onTorchModeUnavailable(cameraId: String) {
                super.onTorchModeUnavailable(cameraId)
                if (targetCameraId == null || cameraId == targetCameraId) {
                    _isFlashlightOn = false
                }
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cm.registerTorchCallback(context.mainExecutor, callback)
            } else {
                cm.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
            }
        } catch (e: Exception) {
            // Non-critical: state tracking defaults to internal flag
        }
    }

    override fun setFlashlight(enabled: Boolean): ExecutionContract {
        if (!hasFlashFeature || cameraManager == null) {
            return ExecutionContract.Failed(
                spokenFeedback = "This device does not have a flashlight.",
                displayFeedback = "No flashlight hardware",
                errorType = ExecutionFailureType.HARDWARE_UNAVAILABLE
            )
        }

        val id = targetCameraId ?: return ExecutionContract.Failed(
            spokenFeedback = "No camera with flashlight support could be found.",
            displayFeedback = "Flashlight unavailable",
            errorType = ExecutionFailureType.HARDWARE_UNAVAILABLE
        )

        return try {
            cameraManager.setTorchMode(id, enabled)
            _isFlashlightOn = enabled
            val spoken = if (enabled) "Flashlight turned on." else "Flashlight turned off."
            val display = if (enabled) "Flashlight ON" else "Flashlight OFF"
            ExecutionContract.Succeeded(
                spokenFeedback = spoken,
                displayFeedback = display
            )
        } catch (e: CameraAccessException) {
            val (errorType, spoken, display) = when (e.reason) {
                CameraAccessException.CAMERA_IN_USE,
                CameraAccessException.MAX_CAMERAS_IN_USE -> Triple(
                    ExecutionFailureType.HARDWARE_BUSY,
                    "Flashlight is unavailable because the camera is currently in use.",
                    "Camera in use"
                )
                CameraAccessException.CAMERA_DISABLED -> Triple(
                    ExecutionFailureType.SECURITY_RESTRICTION,
                    "Flashlight cannot be turned on due to device policy.",
                    "Camera disabled"
                )
                else -> Triple(
                    ExecutionFailureType.HARDWARE_UNAVAILABLE,
                    "Could not control flashlight: ${e.localizedMessage ?: "camera access error"}.",
                    "Camera error"
                )
            }
            ExecutionContract.Failed(spoken, display, errorType, e)
        } catch (e: SecurityException) {
            ExecutionContract.Failed(
                spokenFeedback = "Permission restriction prevented controlling the flashlight.",
                displayFeedback = "Security restriction: torch",
                errorType = ExecutionFailureType.SECURITY_RESTRICTION,
                exception = e
            )
        } catch (e: IllegalArgumentException) {
            ExecutionContract.Failed(
                spokenFeedback = "Camera ID not recognized for flashlight.",
                displayFeedback = "Invalid camera ID",
                errorType = ExecutionFailureType.HARDWARE_UNAVAILABLE,
                exception = e
            )
        } catch (e: IllegalStateException) {
            ExecutionContract.Failed(
                spokenFeedback = "Flashlight is in an invalid state.",
                displayFeedback = "Torch state error",
                errorType = ExecutionFailureType.HARDWARE_UNAVAILABLE,
                exception = e
            )
        } catch (e: Exception) {
            ExecutionContract.Failed(
                spokenFeedback = "Unexpected error controlling flashlight: ${e.localizedMessage ?: "system error"}.",
                displayFeedback = "Flashlight error",
                errorType = ExecutionFailureType.UNKNOWN,
                exception = e
            )
        }
    }

    override fun toggleFlashlight(): ExecutionContract {
        return setFlashlight(!_isFlashlightOn)
    }
}

/**
 * Unified device hardware controller for battery and flashlight features.
 * Provides clean dependency injection for unit testing and honest contract feedback.
 */
object DeviceHardwareController {

    var batteryProvider: BatteryInfoProvider? = null
    var flashlightController: FlashlightController? = null

    fun getBatteryStatus(context: Context): ExecutionContract {
        val provider = batteryProvider ?: AndroidBatteryInfoProvider(context.applicationContext)
        val status = provider.getBatteryStatus()
        return formatBatteryContract(status)
    }

    fun getRawBatteryStatus(context: Context): BatteryStatusResult {
        val provider = batteryProvider ?: AndroidBatteryInfoProvider(context.applicationContext)
        return provider.getBatteryStatus()
    }

    fun setFlashlight(context: Context, enabled: Boolean): ExecutionContract {
        val controller = flashlightController ?: AndroidFlashlightController(context.applicationContext)
        return controller.setFlashlight(enabled)
    }

    fun toggleFlashlight(context: Context): ExecutionContract {
        val controller = flashlightController ?: AndroidFlashlightController(context.applicationContext)
        return controller.toggleFlashlight()
    }

    fun isFlashlightOn(context: Context): Boolean {
        val controller = flashlightController ?: AndroidFlashlightController(context.applicationContext)
        return controller.isFlashlightOn
    }

    fun isFlashAvailable(context: Context): Boolean {
        val controller = flashlightController ?: AndroidFlashlightController(context.applicationContext)
        return controller.isFlashAvailable
    }

    /**
     * Formats structured battery status into an honest, user-facing ExecutionContract.
     */
    fun formatBatteryContract(status: BatteryStatusResult): ExecutionContract {
        val percentage = status.percentage
        val chargingState = status.chargingState

        if (percentage == null && chargingState == BatteryChargingState.UNKNOWN) {
            return ExecutionContract.Failed(
                spokenFeedback = "Battery information is currently unavailable.",
                displayFeedback = "Battery status unavailable",
                errorType = ExecutionFailureType.HARDWARE_UNAVAILABLE
            )
        }

        val (spoken, display) = when (chargingState) {
            BatteryChargingState.CHARGING -> {
                val sourceText = when (status.pluggedSource) {
                    PluggedSource.AC -> " (AC)"
                    PluggedSource.USB -> " (USB)"
                    PluggedSource.WIRELESS -> " (Wireless)"
                    PluggedSource.NONE -> ""
                }
                if (percentage != null) {
                    Pair(
                        "Your battery is at $percentage% and currently charging.",
                        "Battery: $percentage% (Charging$sourceText)"
                    )
                } else {
                    Pair(
                        "Your phone is currently charging, but the exact battery percentage could not be determined.",
                        "Charging$sourceText (Percentage unknown)"
                    )
                }
            }
            BatteryChargingState.FULL -> {
                val pct = percentage ?: 100
                Pair(
                    "Your battery is fully charged at $pct%.",
                    "Battery: $pct% (Full)"
                )
            }
            BatteryChargingState.DISCHARGING -> {
                if (percentage != null) {
                    Pair(
                        "Your battery is at $percentage%.",
                        "Battery: $percentage%"
                    )
                } else {
                    Pair(
                        "Your battery is discharging, but the percentage is unknown.",
                        "Discharging (Unknown %)"
                    )
                }
            }
            BatteryChargingState.NOT_CHARGING -> {
                if (percentage != null) {
                    Pair(
                        "Your battery is at $percentage% and not charging.",
                        "Battery: $percentage% (Not charging)"
                    )
                } else {
                    Pair(
                        "Your battery is not charging.",
                        "Not charging"
                    )
                }
            }
            BatteryChargingState.UNKNOWN -> {
                if (percentage != null) {
                    Pair(
                        "Your battery is at $percentage%.",
                        "Battery: $percentage%"
                    )
                } else {
                    Pair(
                        "Battery information is currently unavailable.",
                        "Battery: Unavailable"
                    )
                }
            }
        }

        return ExecutionContract.Succeeded(
            spokenFeedback = spoken,
            displayFeedback = display
        )
    }
}
