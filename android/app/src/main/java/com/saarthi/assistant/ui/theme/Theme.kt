package com.saarthi.assistant.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = MidnightNavy,
    primaryContainer = ElevatedSurface,
    onPrimaryContainer = TextPrimaryDark,
    secondary = SoftViolet,
    onSecondary = MidnightNavy,
    secondaryContainer = ElevatedSurface,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = SuccessGreen,
    onTertiary = MidnightNavy,
    background = MidnightNavy,
    onBackground = TextPrimaryDark,
    surface = DarkBlueGrey,
    onSurface = TextPrimaryDark,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = TextSecondaryDark,
    outline = SurfaceBorderDark,
    outlineVariant = SurfaceBorderDark,
    error = ErrorRed,
    onError = MidnightNavy,
    errorContainer = ErrorRedContainer,
    onErrorContainer = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricCyanDark,
    onPrimary = SurfaceLight,
    primaryContainer = ElevatedLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = SoftVioletDark,
    onSecondary = SurfaceLight,
    secondaryContainer = ElevatedLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = SuccessGreen,
    onTertiary = SurfaceLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = ElevatedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = SurfaceBorderLight,
    outlineVariant = SurfaceBorderLight,
    error = ErrorRed,
    onError = SurfaceLight,
    errorContainer = ElevatedLight,
    onErrorContainer = ErrorRed
)

@Composable
fun SAARTHITheme(
    darkTheme: Boolean = true, // Default to Saarthi's Signature Midnight Navy dark theme
    dynamicColor: Boolean = false, // Keep consistent branding by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val window = (view.context as? Activity)?.window
                if (window != null) {
                    window.statusBarColor = colorScheme.background.toArgb()
                    window.navigationBarColor = colorScheme.background.toArgb()
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme
                }
            } catch (_: Throwable) {
                // Safeguard against OEM skins or detached views
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}