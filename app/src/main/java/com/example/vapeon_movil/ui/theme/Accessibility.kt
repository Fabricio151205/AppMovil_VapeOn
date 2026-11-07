package com.example.vapeon_movil.ui.theme

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Tamaños de texto disponibles en el módulo de accesibilidad.
 * La escala se multiplica por la escala tipográfica del sistema.
 */
enum class TamanoTexto(val etiqueta: String, val escala: Float) {
    PEQUENO("Pequeño", 0.85f),
    MEDIANO("Mediano", 1f),
    GRANDE("Grande", 1.20f)
}

/** Preferencias de accesibilidad que se aplican a toda la interfaz. */
data class AjustesAccesibilidad(
    val tamanoTexto: TamanoTexto = TamanoTexto.MEDIANO,
    val textoNegrita: Boolean = false,
    val altoContraste: Boolean = false,
    val reducirTransparencia: Boolean = false
)

/**
 * Estado observable de la accesibilidad. Persiste cada cambio en
 * SharedPreferences para conservarlo entre sesiones y reinicios.
 */
class AjustesAccesibilidadEstado(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(ARCHIVO_PREFERENCIAS, Context.MODE_PRIVATE)

    var ajustes by mutableStateOf(cargar())
        private set

    private fun cargar(): AjustesAccesibilidad = AjustesAccesibilidad(
        tamanoTexto = runCatching {
            TamanoTexto.valueOf(
                prefs.getString(CLAVE_TAMANO, TamanoTexto.MEDIANO.name) ?: TamanoTexto.MEDIANO.name
            )
        }.getOrDefault(TamanoTexto.MEDIANO),
        textoNegrita = prefs.getBoolean(CLAVE_NEGRITA, false),
        altoContraste = prefs.getBoolean(CLAVE_CONTRASTE, false),
        reducirTransparencia = prefs.getBoolean(CLAVE_TRANSPARENCIA, false)
    )

    fun actualizar(nuevo: AjustesAccesibilidad) {
        ajustes = nuevo
        prefs.edit()
            .putString(CLAVE_TAMANO, nuevo.tamanoTexto.name)
            .putBoolean(CLAVE_NEGRITA, nuevo.textoNegrita)
            .putBoolean(CLAVE_CONTRASTE, nuevo.altoContraste)
            .putBoolean(CLAVE_TRANSPARENCIA, nuevo.reducirTransparencia)
            .apply()
    }

    fun restaurarPredeterminados() = actualizar(AjustesAccesibilidad())

    private companion object {
        const val ARCHIVO_PREFERENCIAS = "vapeon_accesibilidad"
        const val CLAVE_TAMANO = "tamano_texto"
        const val CLAVE_NEGRITA = "texto_negrita"
        const val CLAVE_CONTRASTE = "alto_contraste"
        const val CLAVE_TRANSPARENCIA = "reducir_transparencia"
    }
}

/** Ajustes vigentes, disponibles desde cualquier composable. */
val LocalAjustesAccesibilidad = staticCompositionLocalOf { AjustesAccesibilidad() }

/** Controlador global para modificar los ajustes desde la pantalla de accesibilidad. */
val LocalControladorAccesibilidad = staticCompositionLocalOf<AjustesAccesibilidadEstado?> { null }

/** Evita aplicar dos veces la escala o los efectos cuando hay temas anidados. */
internal val LocalAccesibilidadAplicada = staticCompositionLocalOf { false }

/**
 * Aplica los ajustes de accesibilidad a todo el árbol de composición:
 * escala tipográfica, texto en negrita, alto contraste y transparencias.
 */
@Composable
fun VapeONAccessibilityHost(
    ajustes: AjustesAccesibilidad,
    content: @Composable () -> Unit
) {
    if (LocalAccesibilidadAplicada.current) {
        content()
        return
    }

    val densidad = LocalDensity.current
    val escalaFuente = densidad.fontScale * ajustes.tamanoTexto.escala
    val estiloBase = LocalTextStyle.current
    val estilo = if (ajustes.textoNegrita) {
        estiloBase.copy(fontWeight = FontWeight.Bold)
    } else estiloBase

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAjustesAccesibilidad provides ajustes,
        LocalAccesibilidadAplicada provides true,
        LocalDensity provides Density(densidad.density, escalaFuente),
        LocalTextStyle provides estilo
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (ajustes.altoContraste) Modifier.altoContraste() else Modifier)
                .then(if (ajustes.textoNegrita) Modifier.negritaVisual() else Modifier)
        ) {
            content()
        }
    }
}

/**
 * Refuerza el trazo de toda la interfaz dibujando el contenido dos veces con
 * un desplazamiento subpíxel.
 *
 * Se usa porque `LocalTextStyle` no puede sobrescribir el `fontWeight` que cada
 * `Text` declara explícitamente; este efecto sí engrosa cualquier texto (y los
 * iconos) sin tener que modificar cada pantalla.
 */
private fun Modifier.negritaVisual(): Modifier = drawWithContent {
    drawContent()
    val desplazamiento = 0.35.dp.toPx()
    drawContext.canvas.save()
    drawContext.canvas.translate(desplazamiento, 0f)
    drawContent()
    drawContext.canvas.restore()
}

/**
 * Aumenta el contraste de toda la interfaz mediante una matriz de color
 * aplicada al contenido ya dibujado. Es un efecto global y no altera
 * los colores declarados pantalla por pantalla.
 */
private fun Modifier.altoContraste(): Modifier = drawWithContent {
    val contraste = 1.35f
    val desplazamiento = 127.5f * (1f - contraste)
    val matriz = ColorMatrix(
        floatArrayOf(
            contraste, 0f, 0f, 0f, desplazamiento,
            0f, contraste, 0f, 0f, desplazamiento,
            0f, 0f, contraste, 0f, desplazamiento,
            0f, 0f, 0f, 1f, 0f
        )
    )
    val pintura = Paint().apply { colorFilter = ColorMatrixColorFilter(matriz) }
    drawContext.canvas.saveLayer(Rect(Offset.Zero, size), pintura)
    drawContent()
    drawContext.canvas.restore()
}

/** Alfa efectiva: con "reducir transparencia" activo, los elementos quedan opacos. */
@Composable
fun alfaAccesible(alfa: Float): Float =
    if (LocalAjustesAccesibilidad.current.reducirTransparencia) 1f else alfa
