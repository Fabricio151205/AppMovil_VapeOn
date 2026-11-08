package com.example.vapeon_movil.ui

import androidx.compose.runtime.Composable
import com.example.vapeon_movil.entities.Pago

/**
 * Resultado exitoso de la verificación del pago simulado.
 * El pedido se registra al pulsar "VISUALIZAR PEDIDO" (o al volver al catálogo).
 */
@Composable
fun ConfirmacionPagoScreen(
    pago: Pago,
    cargando: Boolean,
    error: String?,
    registrarPedido: () -> Unit,
    volverAlCatalogo: () -> Unit
) {
    EstadoPagoScreen(
        estado = EstadoVisualPago.EXITOSO,
        titulo = "¡Pago exitoso!",
        mensaje = "Tu pago se verificó correctamente.\nPuedes consultar tu compra en el historial.",
        cargando = cargando,
        error = error,
        textoAccionPrincipal = "VISUALIZAR PEDIDO",
        onAccionPrincipal = registrarPedido,
        textoAccionSecundaria = "VOLVER AL CATÁLOGO",
        onAccionSecundaria = volverAlCatalogo
    )
}
