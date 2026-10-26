package com.saarthi.assistant.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

sealed class CalculatorLaunchResult {
    data class Success(val packageName: String, val appLabel: String) : CalculatorLaunchResult()
    data class NotFound(val message: String) : CalculatorLaunchResult()
    data class LaunchFailed(val message: String) : CalculatorLaunchResult()
}

/**
 * Backward-compatible delegating launcher for calculator applications.
 * Delegates discovery and launch execution to unified AppLauncher.
 */
object CalculatorLauncher {

    fun findCalculatorIntent(packageManager: PackageManager): Pair<Intent, String>? {
        return AppLauncher.findAppIntent(packageManager, TargetApp.CALCULATOR)
    }

    fun launch(context: Context): CalculatorLaunchResult {
        return when (val result = AppLauncher.launch(context, TargetApp.CALCULATOR)) {
            is AppLaunchResult.Success -> CalculatorLaunchResult.Success(result.packageName, result.appLabel)
            is AppLaunchResult.NotFound -> CalculatorLaunchResult.NotFound(result.message)
            is AppLaunchResult.LaunchFailed -> CalculatorLaunchResult.LaunchFailed(result.message)
        }
    }
}
