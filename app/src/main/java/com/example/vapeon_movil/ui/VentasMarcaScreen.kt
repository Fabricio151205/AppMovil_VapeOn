package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.CajaTotal
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.moneda

@Composable
fun VentasMarcaScreen(caja: CajaTotal, marca: String, cargando: Boolean, error: String?,
    recargar: () -> Unit, verDetalle: (String) -> Unit, volver: () -> Unit) {
    BackHandler(onBack = volver)
    Surface(color = VapeOnBackground, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OutlinedButton(onClick = volver, modifier = Modifier.heightIn(min = 48.dp)) { Text("Volver a Caja Total") }
                Text("VENTAS — ${marca.ifBlank { "Todos" }}", style = MaterialTheme.typography.headlineMedium, color = VapeOnGold)
                Text("Importes de productos. Delivery separado en Caja Total.", color = VapeOnTextSecondary)
                Button(onClick = recargar, enabled = !cargando, modifier = Modifier.heightIn(min = 48.dp)) { Text("Recargar") }
            }
            when {
                cargando -> item { CircularProgressIndicator(color = VapeOnGold) }
                error != null -> item { Text(error, color = MaterialTheme.colorScheme.error) }
                else -> {
                    val ventas = caja.ventas(marca)
                    if (ventas.isEmpty()) item { Text("No hay ventas entregadas para esta selección.", color = VapeOnTextPrimary) }
                    items(ventas, key = { it.pedido.id }) { venta ->
                        Card(colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Pedido ${venta.pedido.id}", color = VapeOnGold, style = MaterialTheme.typography.titleMedium)
                                Text("Fecha: ${java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(java.time.ZoneId.of("America/Lima")).format(java.time.Instant.parse(venta.pedido.fecha))}", color = VapeOnTextPrimary)
                                venta.productos.forEach { linea ->
                                    Text("${linea.nombre} — ${linea.marca}", color = VapeOnTextPrimary)
                                    Text("Cantidad: ${linea.cantidad} · ${moneda(linea.subtotal)}", color = VapeOnTextPrimary)
                                }
                                Text("Importe: ${moneda(venta.importe)}", color = VapeOnGold)
                                Button(onClick = { verDetalle(venta.pedido.id) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ver detalle") }
                            }
                        }
                    }
                }
            }
        }
    }
}
