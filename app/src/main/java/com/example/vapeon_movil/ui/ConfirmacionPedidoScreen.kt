package com.example.vapeon_movil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.ui.theme.*

/** Vista independiente: presenta únicamente los datos del pedido real confirmado. */
@Composable
fun ConfirmacionPedidoScreen(
    pedido: Pedido,
    requiereComprobacion: Boolean,
    cargando: Boolean,
    error: String?,
    reintentarLimpieza: () -> Unit,
    volverAlCatalogo: () -> Unit
) {
    PasoCheckout("Confirmación del pedido", volverAlCatalogo, bloqueado = requiereComprobacion || cargando) {
    // Título de la pantalla
    Text(
        text = "¡Pedido Registrado\nCorrectamente!",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = VapeOnTextPrimary,
        lineHeight = 32.sp
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Tarjeta de Resumen del Pedido
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = VapeOnCardBackground
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Pedido: ${pedido.id}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VapeOnTextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Fecha:",
                    color = VapeOnTextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = fechaPedidoTexto(pedido.fecha),
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Estado del envío:",
                    color = VapeOnTextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = pedido.estado,
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Productos
            Text(
                text = "Productos:",
                color = VapeOnTextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            pedido.productos.forEach { prod ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = prod.nombre,
                        color = VapeOnTextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "S/. ${"%.2f".format(prod.subtotal.toDouble())}",
                        color = VapeOnTextPrimary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // IGV
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "IGV incluido",
                    color = VapeOnTextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Incluido en los precios",
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Subtotal: S/ ${com.example.vapeon_movil.utils.importe(pedido.subtotal)}", color = VapeOnTextPrimary)
            Text("Delivery: S/ ${com.example.vapeon_movil.utils.importe(pedido.delivery)}", color = VapeOnTextPrimary)

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total:",
                    color = VapeOnGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "S/. ${"%.2f".format(pedido.total.toDouble())}",
                    color = VapeOnGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pagaras
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (pedido.pagoId == null) "Pagaras:" else "Pagado (SIMULADO):",
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "S/.${"%.2f".format(pedido.montoPagado.toDouble())}",
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vuelto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Vuelto",
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "S/.${"%.2f".format(pedido.vuelto.toDouble())}",
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text("Método de pago: ${pedido.metodoPago}", color = VapeOnTextPrimary)
    pedido.estadoPago?.let { Text("Estado del pago: $it", color = VapeOnGold) }
    pedido.numeroOperacion?.let { Text("Operación: $it", color = VapeOnTextSecondary) }
    if (pedido.modalidadPago == "SIMULADO") Text("No se realizó ningún cobro real.", color = VapeOnTextSecondary)

    if (requiereComprobacion) {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            reintentarLimpieza()
        }, enabled = !cargando) { Text("Reintentar limpieza local") }
        if (cargando) CircularProgressIndicator(color = VapeOnGold)
    }
    // Botón principal: VOLVER AL CATALOGO
    Button(
        onClick = volverAlCatalogo,
        enabled = !requiereComprobacion,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = VapeOnRed,
            contentColor = VapeOnTextPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(
            text = "VOLVER AL CATALOGO",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 0.5.sp
        )
    }
    }
}
