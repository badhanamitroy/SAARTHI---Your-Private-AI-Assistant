package com.saarthi.assistant.core

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings

sealed class AppLaunchResult {
    data class Success(val app: TargetApp, val packageName: String, val appLabel: String) : AppLaunchResult()
    data class NotFound(val app: TargetApp, val message: String) : AppLaunchResult()
    data class LaunchFailed(val app: TargetApp, val message: String, val errorType: ExecutionFailureType = ExecutionFailureType.UNKNOWN) : AppLaunchResult()
}

/**
 * Privacy-preserving, dynamic launcher for native Android applications.
 * Supports Calculator, Clock, Calendar, Camera, and Settings.
 * Uses Android canonical selectors, standard intent actions, and vendor package fallbacks.
 *
 * Implements an honest execution contract: verifies intent resolution prior to launch,
 * explicitly catches ActivityNotFoundException, SecurityException, and returns
 * verified execution contracts without claiming certainty when the OS handles launch asynchronously.
 */
object AppLauncher {

    // Known common OEM & AOSP packages for each target app
    val KNOWN_CALCULATOR_PACKAGES = listOf(
        "com.sec.android.app.popupcalculator",  // Samsung One UI
        "com.google.android.calculator",        // Google Pixel / Stock
        "com.android.calculator2",              // AOSP Generic Android
        "com.miui.calculator",                  // Xiaomi / MIUI / HyperOS
        "com.coloros.calculator",               // Oppo / Realme / ColorOS
        "com.oneplus.calculator",               // OnePlus / OxygenOS
        "com.transsion.calculator",             // Tecno / Infinix / Itel
        "com.vivo.calculator",                  // Vivo / FuntouchOS
        "com.asus.calculator",                  // Asus ZenUI
        "com.motorola.calculator",              // Motorola
        "com.huawei.calculator",                // Huawei / EMUI
        "com.hihonor.calculator",               // Honor / MagicOS
        "com.nothing.calculator"                // Nothing OS
    )

    val KNOWN_CLOCK_PACKAGES = listOf(
        "com.sec.android.app.clockpackage",    // Samsung One UI
        "com.google.android.deskclock",        // Google Clock
        "com.android.deskclock",               // AOSP / Xiaomi
        "com.coloros.alarmclock",              // Oppo / Realme
        "com.oneplus.deskclock",               // OnePlus
        "com.transsion.deskclock",             // Tecno / Infinix / Itel
        "com.vivo.upslide",                    // Vivo
        "com.android.BBKClock",                // Vivo BBK
        "com.motorola.blur.alarmclock",        // Motorola
        "com.asus.deskclock"                   // Asus
    )

    val KNOWN_CALENDAR_PACKAGES = listOf(
        "com.google.android.calendar",         // Google Calendar
        "com.samsung.android.calendar",        // Samsung Calendar
        "com.android.calendar",                // AOSP / Xiaomi / Motorola
        "com.coloros.calendar",                // Oppo / Realme
        "com.oneplus.calendar",                // OnePlus
        "com.transsion.calendar",              // Tecno / Infinix / Itel
        "com.bbk.calendar",                    // Vivo
        "com.huawei.calendar"                  // Huawei
    )

    val KNOWN_CAMERA_PACKAGES = listOf(
        "com.sec.android.app.camera",          // Samsung Camera
        "com.google.android.GoogleCamera",     // Google Pixel Camera
        "com.android.camera",                  // AOSP / Xiaomi
        "com.android.camera2",                 // AOSP Camera2
        "com.oplus.camera",                    // Oppo / Realme
        "com.oneplus.camera",                  // OnePlus
        "com.transsion.camera",                // Tecno / Infinix / Itel
        "com.vivo.camera",                     // Vivo
        "com.motorola.camera2",                // Motorola
        "com.huawei.camera"                    // Huawei
    )

    val KNOWN_SETTINGS_PACKAGES = listOf(
        "com.android.settings"                 // Universal AOSP / OEM Settings
    )

    fun findAppIntent(packageManager: PackageManager, app: TargetApp): Pair<Intent, String>? {
        return when (app) {
            TargetApp.CALCULATOR -> findCalculatorIntent(packageManager)
            TargetApp.CLOCK -> findClockIntent(packageManager)
            TargetApp.CALENDAR -> findCalendarIntent(packageManager)
            TargetApp.CAMERA -> findCameraIntent(packageManager)
            TargetApp.SETTINGS -> findSettingsIntent(packageManager)
        }
    }

    private fun findCalculatorIntent(pm: PackageManager): Pair<Intent, String>? {
        // Strategy 1A: Android canonical makeMainSelectorActivity
        try {
            val selectorIntent = Intent.makeMainSelectorActivity(
                Intent.ACTION_MAIN,
                Intent.CATEGORY_APP_CALCULATOR
            )
            val resolved = selectorIntent.resolveActivity(pm)
            if (resolved != null) {
                val pkg = resolved.packageName
                val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: selectorIntent
                val label = getPackageLabel(pm, pkg, "Calculator")
                return Pair(launchIntent, label)
            }
        } catch (_: Exception) {}

        // Strategy 1B: Standard Android CATEGORY_APP_CALCULATOR query
        val categoryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALCULATOR)
        }
        val categoryMatches = pm.queryIntentActivities(categoryIntent, 0)
        for (resolveInfo in categoryMatches) {
            val pkg = resolveInfo.activityInfo?.packageName
            if (!pkg.isNullOrBlank()) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: Intent(Intent.ACTION_MAIN).apply {
                    setClassName(pkg, resolveInfo.activityInfo.name)
                }
                val label = resolveInfo.loadLabel(pm).toString()
                return Pair(launchIntent, label)
            }
        }

        // Strategy 2: Check known vendor calculator packages
        val vendorMatch = probePackages(pm, KNOWN_CALCULATOR_PACKAGES, "Calculator")
        if (vendorMatch != null) return vendorMatch

        // Strategy 3: Dynamic launcher scan
        return scanLauncherApps(pm, listOf("calculator", "calc"), "Calculator")
    }

    private fun findClockIntent(pm: PackageManager): Pair<Intent, String>? {
        // Strategy 1: Standard AlarmClock.ACTION_SHOW_ALARMS
        try {
            val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            val resolved = clockIntent.resolveActivity(pm)
            if (resolved != null) {
                val pkg = resolved.packageName
                val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: clockIntent
                val label = getPackageLabel(pm, pkg, "Clock")
                return Pair(launchIntent, label)
            }
        } catch (_: Exception) {}

        // Strategy 2: Check known clock packages
        val vendorMatch = probePackages(pm, KNOWN_CLOCK_PACKAGES, "Clock")
        if (vendorMatch != null) return vendorMatch

        // Strategy 3: Dynamic launcher scan
        return scanLauncherApps(pm, listOf("clock", "deskclock", "alarm"), "Clock")
    }

    private fun findCalendarIntent(pm: PackageManager): Pair<Intent, String>? {
        // Strategy 1A: Canonical makeMainSelectorActivity
        try {
            val selectorIntent = Intent.makeMainSelectorActivity(
                Intent.ACTION_MAIN,
                Intent.CATEGORY_APP_CALENDAR
            )
            val resolved = selectorIntent.resolveActivity(pm)
            if (resolved != null) {
                val pkg = resolved.packageName
                val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: selectorIntent
                val label = getPackageLabel(pm, pkg, "Calendar")
                return Pair(launchIntent, label)
            }
        } catch (_: Exception) {}

        // Strategy 1B: Calendar Contract View URI
        try {
            val calendarUri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
            val uriIntent = Intent(Intent.ACTION_VIEW).setData(calendarUri)
            val resolved = uriIntent.resolveActivity(pm)
            if (resolved != null) {
                val pkg = resolved.packageName
                val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: uriIntent
                val label = getPackageLabel(pm, pkg, "Calendar")
                return Pair(launchIntent, label)
            }
        } catch (_: Exception) {}

        // Strategy 2: Known calendar packages
        val vendorMatch = probePackages(pm, KNOWN_CALENDAR_PACKAGES, "Calendar")
        if (vendorMatch != null) return vendorMatch

        // Strategy 3: Dynamic launcher scan
        return scanLauncherApps(pm, listOf("calendar"), "Calendar")
    }

    private fun findCameraIntent(pm: PackageManager): Pair<Intent, String>? {
        // Strategy 1: Canonical still image camera intent (viewfinder only, zero photo capture)
        try {
            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            val resolved = cameraIntent.resolveActivity(pm)
            if (resolved != null) {
                val pkg = resolved.packageName
                val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: cameraIntent
                val label = getPackageLabel(pm, pkg, "Camera")
                return Pair(launchIntent, label)
            }
        } catch (_: Exception) {}

        // Strategy 2: Known camera packages
        val vendorMatch = probePackages(pm, KNOWN_CAMERA_PACKAGES, "Camera")
        if (vendorMatch != null) return vendorMatch

        // Strategy 3: Dynamic launcher scan
        return scanLauncherApps(pm, listOf("camera"), "Camera")
    }

    private fun findSettingsIntent(pm: PackageManager): Pair<Intent, String>? {
        // Strategy 1: Canonical Settings action
        try {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS)
            val resolved = settingsIntent.resolveActivity(pm)
            if (resolved != null) {
                val pkg = resolved.packageName
                val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: settingsIntent
                val label = getPackageLabel(pm, pkg, "Settings")
                return Pair(launchIntent, label)
            }
        } catch (_: Exception) {}

        // Strategy 2: Known settings package
        val vendorMatch = probePackages(pm, KNOWN_SETTINGS_PACKAGES, "Settings")
        if (vendorMatch != null) return vendorMatch

        return null
    }

    private fun probePackages(pm: PackageManager, packages: List<String>, defaultLabel: String): Pair<Intent, String>? {
        for (pkg in packages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                val label = getPackageLabel(pm, pkg, defaultLabel)
                return Pair(launchIntent, label)
            }
        }
        return null
    }

    private fun scanLauncherApps(pm: PackageManager, keywords: List<String>, defaultLabel: String): Pair<Intent, String>? {
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launcherMatches = pm.queryIntentActivities(mainIntent, 0)
        for (resolveInfo in launcherMatches) {
            val pkg = resolveInfo.activityInfo?.packageName?.lowercase() ?: ""
            if (keywords.any { pkg.contains(it) }) {
                val originalPkg = resolveInfo.activityInfo.packageName
                val launchIntent = pm.getLaunchIntentForPackage(originalPkg)
                if (launchIntent != null) {
                    val label = resolveInfo.loadLabel(pm).toString()
                    return Pair(launchIntent, label)
                }
            }
        }
        return null
    }

    private fun getPackageLabel(pm: PackageManager, pkg: String, fallback: String): String {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(pkg, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            fallback
        }
    }

    fun getFallbackMessage(app: TargetApp): String {
        return when (app) {
            TargetApp.CALCULATOR ->
                "No compatible calculator app was found on your phone. You can say 'calculate' followed by your numbers to use the built-in calculator."
            TargetApp.CLOCK ->
                "No compatible clock app was found on your phone. You can ask 'what time is it' to check the current time."
            TargetApp.CALENDAR ->
                "No compatible calendar app was found on your phone. You can ask 'what is today's date' to check today's date."
            TargetApp.CAMERA ->
                "No compatible camera app was found on your phone."
            TargetApp.SETTINGS ->
                "No compatible settings app was found on your phone."
        }
    }

    /**
     * Executes app launch and returns a formal ExecutionContract.
     * Never falsely claims the app is visible on screen, as Android startActivity
     * is an asynchronous OS dispatch.
     */
    fun execute(context: Context, app: TargetApp): ExecutionContract {
        val packageManager = context.packageManager
        val target = findAppIntent(packageManager, app)

        if (target == null) {
            val fallback = getFallbackMessage(app)
            return ExecutionContract.Failed(
                spokenFeedback = fallback,
                displayFeedback = "${app.displayName} not found",
                errorType = ExecutionFailureType.APP_NOT_FOUND
            )
        }

        val (launchIntent, label) = target

        // Verify resolution
        val resolved = launchIntent.resolveActivity(packageManager)
        if (resolved == null) {
            return ExecutionContract.Failed(
                spokenFeedback = "Could not resolve a component to launch $label.",
                displayFeedback = "Unresolvable activity for $label",
                errorType = ExecutionFailureType.ACTIVITY_NOT_FOUND
            )
        }

        if (context !is Activity) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(launchIntent)
            val pkg = launchIntent.`package` ?: launchIntent.component?.packageName ?: app.displayName.lowercase()

            // Honest feedback contract: we dispatched the request to the OS
            ExecutionContract.Succeeded(
                spokenFeedback = "I sent the request to open $label.",
                displayFeedback = "Request sent: $label",
                targetApp = app,
                packageName = pkg
            )
        } catch (e: ActivityNotFoundException) {
            ExecutionContract.Failed(
                spokenFeedback = "No application activity was found to open $label.",
                displayFeedback = "Activity not found for $label",
                errorType = ExecutionFailureType.ACTIVITY_NOT_FOUND,
                exception = e
            )
        } catch (e: SecurityException) {
            ExecutionContract.Failed(
                spokenFeedback = "Permission restriction prevented opening $label.",
                displayFeedback = "Security restriction: $label",
                errorType = ExecutionFailureType.SECURITY_RESTRICTION,
                exception = e
            )
        } catch (e: Exception) {
            ExecutionContract.Failed(
                spokenFeedback = "Found $label, but could not launch it: ${e.localizedMessage ?: "system error"}.",
                displayFeedback = "Launch failed for $label",
                errorType = ExecutionFailureType.UNKNOWN,
                exception = e
            )
        }
    }

    /**
     * Backward-compatible launcher delegating to execution logic.
     */
    fun launch(context: Context, app: TargetApp): AppLaunchResult {
        return when (val contract = execute(context, app)) {
            is ExecutionContract.Succeeded -> {
                AppLaunchResult.Success(
                    app = contract.targetApp ?: app,
                    packageName = contract.packageName ?: app.displayName.lowercase(),
                    appLabel = contract.displayFeedback.removePrefix("Request sent: ")
                )
            }
            is ExecutionContract.Failed -> {
                if (contract.errorType == ExecutionFailureType.APP_NOT_FOUND) {
                    AppLaunchResult.NotFound(app, contract.spokenFeedback)
                } else {
                    AppLaunchResult.LaunchFailed(app, contract.spokenFeedback, contract.errorType)
                }
            }
            else -> {
                AppLaunchResult.LaunchFailed(app, "Unexpected launch state.")
            }
        }
    }
}
