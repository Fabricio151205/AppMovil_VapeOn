package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallePedidoScreen(
    usuario: Usuario?, pedidoId: String, pedidoViewModel: PedidoViewModel, volver: () -> Unit
) {
    val usuarioId = usuario?.takeIf { it.rol.equals("CLIENTE", true) }?.id.orEmpty()
    LaunchedEffect(usuarioId, pedidoId) {
        pedidoViewModel.actualizarUsuarioHistorial(usuario)
        pedidoViewModel.cargarDetallePedido(usuarioId, pedidoId)
    }
    BackHandler(onBack = volver)
    VapeON_MovilTheme {
        Scaffold(containerColor = VapeOnBackground, topBar = {
            TopAppBar(title = { Text("Detalle del pedido", color = VapeOnGold) }, navigationIcon = {
                IconButton(onClick = volver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver al historial", tint = VapeOnTextPrimary)
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground))
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val pedido = pedidoViewModel.detallePedido
                when {
                    usuarioId.isBlank() -> Text("Inicia sesión como cliente para consultar el pedido.",
                        color = MaterialTheme.colorScheme.error)
                    pedidoViewModel.usuarioHistorialId != usuarioId || pedidoViewModel.cargandoDetalle ->
                        CircularProgressIndicator(color = VapeOnGold)
                    pedidoViewModel.errorDetalle != null -> {
                        Text(pedidoViewModel.errorDetalle.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Button(onClick = { pedidoViewModel.cargarDetallePedido(usuarioId, pedidoId) },
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Reintentar") }
                    }
                    pedido != null && pedido.usuarioId == usuarioId && pedido.id == pedidoId -> {
                        Text("Pedido ${pedido.id}", style = MaterialTheme.typography.titleLarge,
                            color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Fecha: ${fechaPedidoTexto(pedido.fecha)}", color = VapeOnTextPrimary)
                        Text("Estado: ${pedido.estado}", color = VapeOnGold)
                        Text("Productos comprados", style = MaterialTheme.typography.titleMedium, color = VapeOnTextPrimary)
                        pedido.productos.forEach { producto ->
                            Card(shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(producto.nombre, style = MaterialTheme.typography.titleMedium,
                                        color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                                    Text(producto.marca, color = VapeOnTextPrimary)
                                    Text("Cantidad: ${producto.cantidad}", color = VapeOnTextPrimary)
                                    Text("Precio unitario: S/ ${producto.precioUnitario.toPlainString()}", color = VapeOnTextPrimary)
                                    Text("Subtotal: S/ ${producto.subtotal.toPlainString()}", color = VapeOnGold)
                                }
                            }
                        }
                        Card(shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = VapeOnRedDark)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Resumen de la compra", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                                Text("Subtotal: S/ ${pedido.subtotal.toPlainString()}", color = VapeOnTextPrimary)
                                Text("Delivery: S/ ${pedido.delivery.toPlainString()}", color = VapeOnTextPrimary)
                                HorizontalDivider(color = VapeOnGold)
                                Text("Total: S/ ${pedido.total.toPlainString()}", color = VapeOnTextPrimary,
                                    style = MaterialTheme.typography.titleMedium)
                                Text("Monto pagado / efectivo previsto: S/ ${pedido.montoPagado.toPlainString()}", color = VapeOnTextPrimary)
                                Text("Vuelto: S/ ${pedido.vuelto.toPlainString()}", color = VapeOnTextPrimary)
                                Text("Método de pago: ${pedido.metodoPago}", color = VapeOnTextPrimary)
                            }
                        }
                        if (pedido.direccionEntrega.isNotBlank()) {
                            Text("Dirección de entrega", color = VapeOnGold, style = MaterialTheme.typography.titleMedium)
                            Text(pedido.direccionEntrega, color = VapeOnTextPrimary)
                        }
                    }
                    else -> Text("No hay un pedido disponible para esta sesión.", color = VapeOnTextPrimary)
                }
            }
        }
    }
}
