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

/** Presentación de la compra confirmada; CheckoutScreen conserva el control del flujo. */
@Composable
fun ConfirmacionPedidoScreen(
    pedido: Pedido,
    requiereComprobacion: Boolean,
    cargando: Boolean,
    error: String?,
    reintentarLimpieza: () -> Unit,
    volverAlCatalogo: () -> Unit
) {
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
                text = "Pedido N° 1",
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
                    text = "Estado:",
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
            val igvCalculado = pedido.subtotal.toDouble() * 0.18
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "IGV",
                    color = VapeOnTextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "S/. ${"%.2f".format(igvCalculado)}",
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    text = "Pagaras:",
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
