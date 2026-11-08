package com.example.vapeon_movil.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.Pago
import com.example.vapeon_movil.ui.theme.VapeOnError
import com.example.vapeon_movil.ui.theme.VapeOnGold
import com.example.vapeon_movil.ui.theme.VapeOnTextMuted
import com.example.vapeon_movil.ui.theme.VapeOnTextPrimary
import com.example.vapeon_movil.ui.theme.VapeOnTextSecondary
import com.example.vapeon_movil.utils.importe

/**
 * Verificación del pago digital simulado (Yape / tarjeta).
 *
 * - PENDIENTE: "Esperando Verificación del Pago" con indicador dorado.
 * - RECHAZADO: "¡Algo falló!" con opción de reintentar o volver al catálogo.
 * - CONFIRMADO se presenta en [ConfirmacionPagoScreen].
 */
@Composable
fun PagoSimuladoScreen(
    pago: Pago?,
    cargando: Boolean,
    error: String?,
    comprobar: () -> Unit,
    aprobar: () -> Unit,
    rechazar: () -> Unit,
    nuevoIntento: () -> Unit,
    volver: () -> Unit,
    volverAlCatalogo: () -> Unit
) {
    val rechazado = pago?.estadoPago == "RECHAZADO"

    if (rechazado) {
        EstadoPagoScreen(
            estado = EstadoVisualPago.RECHAZADO,
            titulo = "¡Algo falló!",
            mensaje = "No pudimos verificar tu pago.\nTu carrito se conserva para que puedas intentarlo nuevamente.",
            cargando = cargando,
            error = error,
            textoAccionPrincipal = "VOLVER A INTENTAR",
            onAccionPrincipal = nuevoIntento,
            textoAccionSecundaria = "VOLVER AL CATÁLOGO",
            onAccionSecundaria = volverAlCatalogo,
            onVolver = nuevoIntento
        )
        return
    }

    EstadoPagoScreen(
        estado = EstadoVisualPago.ESPERANDO,
        titulo = "Esperando Verificación\ndel Pago",
        cargando = cargando,
        error = error,
        // Mientras existe una operación pendiente no se abandona la verificación.
        onVolver = if (pago == null && !cargando) volver else null,
        contenidoExtra = {
            pago?.let {
                Text(
                    text = "Monto: S/ ${importe(it.monto)}",
                    color = VapeOnTextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            Text(
                text = "SIMULACIÓN ACADÉMICA",
                color = VapeOnTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = aprobar,
                    enabled = !cargando && pago != null,
                    border = BorderStroke(1.dp, VapeOnGold)
                ) {
                    Text("Simular aprobación", color = VapeOnGold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = rechazar,
                    enabled = !cargando && pago != null,
                    border = BorderStroke(1.dp, VapeOnError)
                ) {
                    Text("Simular rechazo", color = VapeOnError, fontSize = 12.sp)
                }
            }

            if (pago == null) {
                TextButton(onClick = comprobar, enabled = !cargando) {
                    Text("Comprobar pago", color = VapeOnGold, fontSize = 12.sp)
                }
            } else {
                TextButton(onClick = comprobar, enabled = !cargando) {
                    Text("Comprobar pago", color = VapeOnTextPrimary, fontSize = 12.sp)
                }
            }
        }
    )
}
