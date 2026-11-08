package com.example.vapeon_movil.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.vapeon_movil.viewmodels.PedidoViewModel

/**
 * Verificación del pago en efectivo.
 *
 * El monto se ingresa una sola vez en [MetodoPagoScreen]; esta pantalla solo
 * muestra la espera mientras se registra el pedido o permite comprobar el
 * resultado cuando la respuesta quedó en duda. Ya no repite el formulario.
 */
@Composable
fun EfectivoScreen(
    pedidoViewModel: PedidoViewModel,
    reintentar: () -> Unit,
    volver: () -> Unit
) {
    val comprobando = pedidoViewModel.requiereComprobacion && !pedidoViewModel.cargando

    if (!pedidoViewModel.cargando && !pedidoViewModel.requiereComprobacion) {
        // Sin operación pendiente no hay nada que verificar: volver al método de pago.
        LaunchedEffect(Unit) { volver() }
        return
    }

    EstadoPagoScreen(
        estado = EstadoVisualPago.ESPERANDO,
        titulo = "Esperando Verificación\ndel Pago",
        mensaje = if (comprobando) "Estamos comprobando si tu pedido quedó registrado." else "",
        cargando = pedidoViewModel.cargando,
        progreso = pedidoViewModel.cargando || !comprobando,
        error = pedidoViewModel.error,
        textoAccionPrincipal = if (comprobando) "COMPROBAR PEDIDO" else null,
        onAccionPrincipal = reintentar,
        onVolver = if (pedidoViewModel.cargando) null else volver
    )
}
