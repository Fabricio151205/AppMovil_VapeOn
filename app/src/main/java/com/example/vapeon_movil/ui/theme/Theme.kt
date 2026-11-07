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
import androidx.compose.ui.graphics.Color
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

/** Esquema de alto contraste: fondos puros y textos sin matices. */
private val HighContrastColorScheme = darkColorScheme(
    primary = VapeOnRed,
    onPrimary = Color.White,
    primaryContainer = VapeOnRedHover,
    onPrimaryContainer = Color.White,
    secondary = VapeOnGold,
    onSecondary = Color.Black,
    tertiary = VapeOnGoldVariant,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF050505),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = Color.White,
    outline = Color.White,
    error = VapeOnError,
)

@Composable
fun VapeON_MovilTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accessibility: AjustesAccesibilidad = LocalAjustesAccesibilidad.current,
    content: @Composable () -> Unit
) {
    // Si un host de accesibilidad ya se aplicó más arriba, el tema anidado no
    // vuelve a escalar la tipografía ni a duplicar el efecto de contraste.
    val ajustes = if (LocalAccesibilidadAplicada.current) {
        AjustesAccesibilidad()
    } else accessibility

    val colorScheme = when {
        ajustes.altoContraste -> HighContrastColorScheme
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

    VapeONAccessibilityHost(ajustes) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (accessibility.textoNegrita) Typography.enNegrita() else Typography,
            content = content
        )
    }
}
