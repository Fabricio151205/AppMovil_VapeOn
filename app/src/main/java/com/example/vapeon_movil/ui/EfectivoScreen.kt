package com.example.vapeon_movil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.importe
import com.example.vapeon_movil.viewmodels.*
import java.math.BigDecimal

/** RF22: presentación del efectivo; la única compra sigue siendo PedidoViewModel.confirmar(). */
@Composable
fun EfectivoScreen(usuario: Usuario?, carritoViewModel: CarritoViewModel, productoViewModel: ProductoViewModel,
    pedidoViewModel: PedidoViewModel, direccion: String, montoTexto: String,
    cambiarMonto: (String) -> Unit, volver: () -> Unit) {
    val bloqueado = pedidoViewModel.cargando || pedidoViewModel.requiereComprobacion
    val subtotal = runCatching { Pedido.subtotal(carritoViewModel.items) }.getOrNull()
    val delivery = BigDecimal.valueOf(carritoViewModel.resumen.delivery).setScale(2)
    val total = subtotal?.plus(delivery)
    val monto = Pedido.leerMonto(montoTexto)
    val vuelto = if (monto != null && total != null && monto >= total) monto - total else null
    PasoCheckout("Efectivo", volver, bloqueado) {
        Text("Pago con entrega", color = VapeOnTextPrimary)
        Text("Dirección: $direccion", color = VapeOnTextSecondary)
        OutlinedTextField(value = montoTexto, onValueChange = cambiarMonto, enabled = !bloqueado,
            label = { Text("¿Con cuanto va a pagar?") }, prefix = { Text("S/") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        val errorMonto = when {
            montoTexto.isBlank() -> null
            monto == null -> "Ingresa un monto válido con máximo dos decimales."
            total != null && monto < total -> "El monto ingresado es insuficiente."
            else -> null
        }
        errorMonto?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Vuelto: S/ ${vuelto?.let { importe(it) } ?: "—"}", color = VapeOnGold)
        carritoViewModel.items.forEach { item ->
            Text("${item.nombre} · Cantidad: ${item.cantidad} · S/ ${importe(item.precioUnit)}",
                color = VapeOnTextPrimary)
        }
        pedidoViewModel.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Card(colors = CardDefaults.cardColors(containerColor = VapeOnRedDark)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Resumen del Pago", color = VapeOnTextPrimary)
                Text("Sub Total: S/ ${subtotal?.let { importe(it) } ?: "—"}", color = VapeOnTextPrimary)
                Text("Delivery: S/ ${importe(delivery)}", color = VapeOnTextPrimary)
                Text("IGV incluido", color = VapeOnTextSecondary)
                HorizontalDivider(color = VapeOnGold)
                Text("TOTAL A PAGAR: S/ ${total?.let { importe(it) } ?: "—"}", color = VapeOnGold)
                if (pedidoViewModel.cargando) CircularProgressIndicator(color = VapeOnGold)
                Button(onClick = {
                    pedidoViewModel.confirmar(usuario, carritoViewModel, productoViewModel, direccion, montoTexto)
                }, enabled = !pedidoViewModel.cargando && (pedidoViewModel.requiereComprobacion ||
                    (direccion.isNotBlank() && vuelto != null)),
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (pedidoViewModel.requiereComprobacion) "Comprobar pedido" else "PAGAR")
                }
            }
        }
    }
}
