package com.example.vapeon_movil.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VapeOnRed,
    onPrimary = VapeOnTextPrimary,
    primaryContainer = VapeOnRedHover,
    onPrimaryContainer = VapeOnTextPrimary,
    secondary = VapeOnGold,
    onSecondary = VapeOnTextPrimary,
    tertiary = VapeOnGoldVariant,
    background = VapeOnBackground,
    onBackground = VapeOnTextPrimary,
    surface = VapeOnSurface,
    onSurface = VapeOnTextPrimary,
    surfaceVariant = VapeOnSurfaceVariant,
    onSurfaceVariant = VapeOnTextSecondary,
    outline = VapeOnBorder,
    error = VapeOnError,
)

private val LightColorScheme = darkColorScheme(
    primary = VapeOnRed,
    onPrimary = VapeOnTextPrimary,
    primaryContainer = VapeOnRedHover,
    onPrimaryContainer = VapeOnTextPrimary,
    secondary = VapeOnGold,
    onSecondary = VapeOnTextPrimary,
    tertiary = VapeOnGoldVariant,
    background = VapeOnBackground,
    onBackground = VapeOnTextPrimary,
    surface = VapeOnSurface,
    onSurface = VapeOnTextPrimary,
    surfaceVariant = VapeOnSurfaceVariant,
    onSurfaceVariant = VapeOnTextSecondary,
    outline = VapeOnBorder,
    error = VapeOnError,
)

@Composable
fun VapeON_MovilTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = VapeOnBackground.toArgb()
                it.navigationBarColor = VapeOnBackground.toArgb()
                val controller = WindowCompat.getInsetsController(it, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
