package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.PedidoViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun fechaPedidoTexto(fecha: String): String = DateTimeFormatter
    .ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
    .format(Instant.parse(fecha).atZone(ZoneId.systemDefault()))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialComprasScreen(
    usuario: Usuario?, pedidoViewModel: PedidoViewModel,
    volver: () -> Unit, irDetallePedido: (String) -> Unit
) {
    val usuarioId = usuario?.takeIf { it.rol.equals("CLIENTE", true) }?.id.orEmpty()
    LaunchedEffect(usuarioId) {
        pedidoViewModel.actualizarUsuarioHistorial(usuario)
        pedidoViewModel.cargarHistorial(usuarioId)
    }
    BackHandler(onBack = volver)
    VapeON_MovilTheme {
        Scaffold(containerColor = VapeOnBackground, topBar = {
            TopAppBar(title = { Text("Historial de compras", color = VapeOnGold) }, navigationIcon = {
                IconButton(onClick = volver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver al catálogo", tint = VapeOnTextPrimary)
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground))
        }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    usuarioId.isBlank() -> item { Text("Inicia sesión como cliente para consultar tus compras.",
                        color = MaterialTheme.colorScheme.error) }
                    pedidoViewModel.usuarioHistorialId != usuarioId || pedidoViewModel.cargandoHistorial ->
                        item { CircularProgressIndicator(color = VapeOnGold) }
                    pedidoViewModel.errorHistorial != null -> item {
                        Text(pedidoViewModel.errorHistorial.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Button(onClick = { pedidoViewModel.cargarHistorial(usuarioId) },
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Reintentar") }
                    }
                    else -> {
                        item {
                            OutlinedButton(onClick = { pedidoViewModel.cargarHistorial(usuarioId) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Actualizar compras") }
                        }
                        if (pedidoViewModel.historial.isEmpty()) item {
                            Text("Todavía no tienes compras registradas.", color = VapeOnTextPrimary,
                                style = MaterialTheme.typography.titleMedium)
                        }
                        items(pedidoViewModel.historial.filter { it.usuarioId == usuarioId }, key = { it.id }) { pedido ->
                            Card(onClick = { irDetallePedido(pedido.id) }, shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Pedido ${pedido.id}", style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold, color = VapeOnTextPrimary)
                                    Text(fechaPedidoTexto(pedido.fecha), color = VapeOnTextPrimary)
                                    Text("Total: S/ ${pedido.total.toPlainString()}", color = VapeOnGold,
                                        style = MaterialTheme.typography.titleMedium)
                                    Text("${pedido.metodoPago} · ${pedido.estado}", color = VapeOnTextPrimary)
                                    Text("Ver detalle", color = VapeOnGold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
