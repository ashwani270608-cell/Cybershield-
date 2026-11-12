package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CyberColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = CyberBg,
    primaryContainer = CyberSurfaceElevated,
    onPrimaryContainer = CyberCyan,
    secondary = CyberBlue,
    onSecondary = CyberBg,
    secondaryContainer = CyberSurfaceCard,
    onSecondaryContainer = CyberBlue,
    tertiary = CyberIndigo,
    onTertiary = CyberBg,
    background = CyberBg,
    onBackground = CyberTextPrimary,
    surface = CyberSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberSurfaceElevated,
    onSurfaceVariant = CyberTextSecondary,
    outline = CyberBorder,
    outlineVariant = CyberBorderGlow,
    error = SeverityCritical,
    onError = CyberTextPrimary,
    errorContainer = SeverityCriticalBg,
    onErrorContainer = SeverityCritical
)

@Composable
fun CyberShieldTheme(
    darkTheme: Boolean = true, // Default to cyber dark theme for security dashboard
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CyberBg.toArgb()
            window.navigationBarColor = CyberBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
