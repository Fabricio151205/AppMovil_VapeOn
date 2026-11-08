package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.ui.theme.VapeOnBackground
import com.example.vapeon_movil.ui.theme.VapeOnError
import com.example.vapeon_movil.ui.theme.VapeOnGold
import com.example.vapeon_movil.ui.theme.VapeOnRed
import com.example.vapeon_movil.ui.theme.VapeOnTextPrimary
import com.example.vapeon_movil.ui.theme.VapeOnTextSecondary
import com.example.vapeon_movil.ui.theme.VapeON_MovilTheme

/** Estructura visual común para la verificación de un pago simulado. */
enum class EstadoVisualPago { ESPERANDO, EXITOSO, RECHAZADO }

/**
 * Pantalla de estado del pago: espera, éxito o rechazo.
 *
 * Mantiene el mismo lenguaje visual que el resto de VAPEON: fondo oscuro,
 * contenido centrado, iconos en dorado/rojo y acciones con forma de píldora.
 */
@Composable
fun EstadoPagoScreen(
    estado: EstadoVisualPago,
    titulo: String,
    mensaje: String = "",
    cargando: Boolean = false,
    progreso: Boolean = true,
    error: String? = null,
    textoAccionPrincipal: String? = null,
    onAccionPrincipal: () -> Unit = {},
    textoAccionSecundaria: String? = null,
    onAccionSecundaria: () -> Unit = {},
    onVolver: (() -> Unit)? = null,
    contenidoExtra: @Composable ColumnScope.() -> Unit = {}
) {
    if (onVolver != null) BackHandler { if (!cargando) onVolver() }

    VapeON_MovilTheme {
        Surface(color = VapeOnBackground, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            when (estado) {
                EstadoVisualPago.ESPERANDO -> {
                    Text(
                        text = titulo,
                        color = VapeOnTextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 33.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(34.dp))
                    if (progreso) {
                        CircularProgressIndicator(
                            color = VapeOnGold,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                    if (mensaje.isNotBlank()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = mensaje,
                            color = VapeOnTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                EstadoVisualPago.EXITOSO -> {
                    IconoEstado(icono = Icons.Default.Check, color = VapeOnGold)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = titulo,
                        color = VapeOnTextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (mensaje.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = mensaje,
                            color = VapeOnTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                EstadoVisualPago.RECHAZADO -> {
                    IconoEstado(icono = Icons.Default.Close, color = VapeOnRed)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = titulo,
                        color = VapeOnTextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (mensaje.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = mensaje,
                            color = VapeOnTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (estado == EstadoVisualPago.ESPERANDO) {
                contenidoExtra()
            }

            error?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = VapeOnError,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
            }

            if (cargando && estado != EstadoVisualPago.ESPERANDO) {
                CircularProgressIndicator(
                    color = VapeOnGold,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            textoAccionPrincipal?.let { texto ->
                Button(
                    onClick = onAccionPrincipal,
                    enabled = !cargando,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary,
                        disabledContainerColor = VapeOnRed.copy(alpha = 0.45f),
                        disabledContentColor = VapeOnTextPrimary.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = texto,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            textoAccionSecundaria?.let { texto ->
                TextButton(onClick = onAccionSecundaria, enabled = !cargando) {
                    Text(
                        text = texto,
                        color = VapeOnTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun IconoEstado(icono: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(46.dp)
        )
    }
}
