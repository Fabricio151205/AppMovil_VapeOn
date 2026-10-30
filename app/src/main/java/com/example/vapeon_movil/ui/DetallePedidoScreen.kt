package com.example.vapeon_movil.ui

import com.example.vapeon_movil.utils.moneda

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
    val esAdmin = usuario?.rol?.equals("ADMIN", true) == true
    val usuarioId = usuario?.takeIf { esAdmin || it.rol.equals("CLIENTE", true) }?.id.orEmpty()
    val guardando = esAdmin && pedidoViewModel.guardandoEstado
    val actualizar: () -> Unit = {
        if (esAdmin) pedidoViewModel.cargarDetalleAdmin(usuario, pedidoId)
        else {
            pedidoViewModel.actualizarUsuarioHistorial(usuario)
            pedidoViewModel.cargarDetallePedido(usuarioId, pedidoId)
        }
    }
    LaunchedEffect(usuarioId, esAdmin, pedidoId) { actualizar() }
    BackHandler { if (!guardando) volver() }
    VapeON_MovilTheme {
        Scaffold(containerColor = VapeOnBackground, topBar = {
            TopAppBar(title = { Text("Detalle del pedido", color = VapeOnGold) }, navigationIcon = {
                IconButton(onClick = volver, enabled = !guardando) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver a pedidos", tint = VapeOnTextPrimary)
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground))
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val pedido = if (esAdmin) pedidoViewModel.detalleAdmin else pedidoViewModel.detallePedido
                val sesionDetalle = if (esAdmin) pedidoViewModel.usuarioAdminId else pedidoViewModel.usuarioHistorialId
                val cargando = if (esAdmin) pedidoViewModel.cargandoDetalleAdmin else pedidoViewModel.cargandoDetalle
                val error = if (esAdmin) pedidoViewModel.errorDetalleAdmin else pedidoViewModel.errorDetalle
                when {
                    usuarioId.isBlank() -> Text("Inicia sesión como cliente para consultar el pedido.",
                        color = MaterialTheme.colorScheme.error)
                    sesionDetalle != usuarioId || cargando ->
                        CircularProgressIndicator(color = VapeOnGold)
                    error != null -> {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        Button(onClick = actualizar,
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Reintentar") }
                    }
                    pedido != null && (esAdmin || pedido.usuarioId == usuarioId) && pedido.id == pedidoId -> {
                        Text("Pedido ${pedido.id}", style = MaterialTheme.typography.titleLarge,
                            color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Fecha: ${fechaPedidoTexto(pedido.fecha)}", color = VapeOnTextPrimary)
                        Text("Estado del pedido: ${pedido.estado}", color = VapeOnGold)
                        OutlinedButton(onClick = actualizar, enabled = !guardando,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Actualizar pedido") }
                        if (esAdmin) {
                            Text("Cliente: ${pedido.nombreCliente}\nTeléfono: ${pedido.telefono}", color = VapeOnTextPrimary)
                            var estadoSeleccionado by remember(pedido.id, pedido.estado) { mutableStateOf(pedido.estado) }
                            Card(shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = VapeOnSurface)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Cambiar estado del envío", color = VapeOnGold,
                                        style = MaterialTheme.typography.titleMedium)
                                    PedidoViewModel.ESTADOS_ENVIO.forEach { estado ->
                                        FilterChip(selected = estadoSeleccionado == estado,
                                            onClick = { estadoSeleccionado = estado }, enabled = !guardando,
                                            label = { Text(estado) }, modifier = Modifier.heightIn(min = 48.dp))
                                    }
                                    pedidoViewModel.errorEstado?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                    pedidoViewModel.mensajeEstado?.let { Text(it, color = VapeOnGold) }
                                    if (guardando) CircularProgressIndicator(color = VapeOnGold)
                                    Button(onClick = {
                                        pedidoViewModel.actualizarEstadoPedido(usuario, pedido.id, estadoSeleccionado)
                                    }, enabled = !guardando && estadoSeleccionado != pedido.estado,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Guardar estado") }
                                }
                            }
                        }
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
                                    Text("Precio unitario: ${moneda(producto.precioUnitario)}", color = VapeOnTextPrimary)
                                    Text("Subtotal: ${moneda(producto.subtotal)}", color = VapeOnGold)
                                }
                            }
                        }
                        Card(shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = VapeOnRedDark)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Resumen de la compra", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                                Text("Subtotal: ${moneda(pedido.subtotal)}", color = VapeOnTextPrimary)
                                Text("Delivery: ${moneda(pedido.delivery)}", color = VapeOnTextPrimary)
                                HorizontalDivider(color = VapeOnGold)
                                Text("Total: ${moneda(pedido.total)}", color = VapeOnTextPrimary,
                                    style = MaterialTheme.typography.titleMedium)
                                Text("Monto pagado / efectivo previsto: ${moneda(pedido.montoPagado)}", color = VapeOnTextPrimary)
                                Text("Vuelto: ${moneda(pedido.vuelto)}", color = VapeOnTextPrimary)
                                Text("Método de pago: ${pedido.metodoPago}", color = VapeOnTextPrimary)
                                pedido.estadoPago?.let { Text("Estado del pago: $it", color = VapeOnGold) }
                                pedido.numeroOperacion?.let { Text("Operación: $it", color = VapeOnTextSecondary) }
                                pedido.modalidadPago?.let { Text("Modalidad: $it", color = VapeOnTextSecondary) }
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
