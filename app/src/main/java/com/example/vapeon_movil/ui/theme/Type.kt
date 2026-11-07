package com.example.vapeon_movil.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Configuración Tipográfica Estándar de VapeON según Diseño
val Typography = Typography(
    // Títulos Principales / Marcas ("BIENVENIDO A VAPEON", "LifePod", "Oxbar", "VAPEON")
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),

    // Subtítulos de Sección o Modales
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),

    // Nombres de Productos y Texto de Botones ("Kit LifePod", "INICIAR SESIÓN")
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Texto de Cuerpo e Inputs ("Correo o Email", "Password", "Buscar Productos")
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),

    // Texto de Botones de Chips ("Todos", "Nuevos", "Promociones")
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Enlaces Secundarios y Ayudas ("Ver todos", "¿Olvidaste tu contraseña?", "Crea un Usuario")
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),

    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Variante tipográfica usada por el modo "Texto en negrita". Eleva todos los
 * pesos a negrita sin cambiar tamaños ni interlineados.
 */
fun Typography.enNegrita(): Typography = Typography(
    displayLarge = displayLarge.copy(fontWeight = FontWeight.Bold),
    displayMedium = displayMedium.copy(fontWeight = FontWeight.Bold),
    displaySmall = displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = titleLarge.copy(fontWeight = FontWeight.ExtraBold),
    titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = titleSmall.copy(fontWeight = FontWeight.Bold),
    bodyLarge = bodyLarge.copy(fontWeight = FontWeight.Bold),
    bodyMedium = bodyMedium.copy(fontWeight = FontWeight.Bold),
    bodySmall = bodySmall.copy(fontWeight = FontWeight.Bold),
    labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold),
    labelMedium = labelMedium.copy(fontWeight = FontWeight.Bold),
    labelSmall = labelSmall.copy(fontWeight = FontWeight.Bold)
)