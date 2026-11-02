package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.CajaTotal
import com.example.vapeon_movil.ui.theme.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun importeCaja(valor: BigDecimal): String = "S/ " + NumberFormat.getNumberInstance(Locale.US).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}.format(valor).replace(',', ' ')

@Composable
fun CajaTotalScreen(caja: CajaTotal, marcas: List<String>, marcaSeleccionada: String,
    cargando: Boolean, error: String?, advertencias: Int, cargandoMarcas: Boolean,
    errorMarcas: String?, seleccionarMarca: (String) -> Unit, verDetalle: (String) -> Unit,
    volver: () -> Unit) {
    BackHandler(onBack = volver)
    val ventas = caja.ventas(marcaSeleccionada)
    Surface(color = VapeOnBackground, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                IconButton(onClick = volver, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al panel", tint = VapeOnTextPrimary)
                }
                Text("CAJA TOTAL", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = VapeOnTextPrimary)
            }
            when {
                cargando -> item { CircularProgressIndicator(color = VapeOnGold) }
                error != null -> item { Text(error, color = MaterialTheme.colorScheme.error) }
                else -> {
                    item {
                        Text(importeCaja(caja.productos), style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold, color = VapeOnGold)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Productos", color = VapeOnTextSecondary)
                            Text(importeCaja(caja.productos), color = VapeOnTextPrimary)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delivery", color = VapeOnTextSecondary)
                            Text(importeCaja(caja.delivery), color = VapeOnTextPrimary)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = VapeOnGold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total recibido", color = VapeOnTextPrimary)
                            Text(importeCaja(caja.total), color = VapeOnGold, fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(selected = marcaSeleccionada.isBlank(), onClick = { seleccionarMarca("") },
                                    label = { Text("Todos") }, modifier = Modifier.heightIn(min = 48.dp))
                            }
                            items(marcas, key = { it }) { marca ->
                                FilterChip(selected = marca.equals(marcaSeleccionada, true), onClick = { seleccionarMarca(marca) },
                                    label = { Text(marca) }, modifier = Modifier.heightIn(min = 48.dp))
                            }
                        }
                        if (cargandoMarcas) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = VapeOnGold)
                        errorMarcas?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                    item {
                        Text(marcaSeleccionada.ifBlank { "Todos" }, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = VapeOnTextPrimary)
                        if (marcaSeleccionada.isNotBlank()) Text(importeCaja(ventas.fold(BigDecimal.ZERO) { suma, venta -> suma + venta.importe }),
                            style = MaterialTheme.typography.titleMedium, color = VapeOnGold)
                        if (advertencias > 0) Text("Ventas excluidas por datos inconsistentes: $advertencias", color = MaterialTheme.colorScheme.error)
                        if (ventas.isEmpty()) Text("Sin ventas entregadas.", color = VapeOnTextSecondary)
                    }
                    items(ventas, key = { it.pedido.id }) { venta ->
                        Card(shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Pedido ${venta.pedido.id}", color = VapeOnTextSecondary,
                                    style = MaterialTheme.typography.labelMedium)
                                Text(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Lima"))
                                    .format(Instant.parse(venta.pedido.fecha)), color = VapeOnTextSecondary,
                                    style = MaterialTheme.typography.bodySmall)
                                venta.productos.forEach { linea ->
                                    Text(linea.nombre, color = VapeOnTextPrimary, style = MaterialTheme.typography.titleMedium)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Cant.: ${linea.cantidad}", color = VapeOnTextSecondary)
                                        Text(importeCaja(linea.subtotal), color = VapeOnTextPrimary)
                                    }
                                }
                                HorizontalDivider(color = VapeOnGold)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(importeCaja(venta.importe), color = VapeOnGold, fontWeight = FontWeight.Bold)
                                    TextButton(onClick = { verDetalle(venta.pedido.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                        Text("Ver detalle", color = VapeOnTextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
