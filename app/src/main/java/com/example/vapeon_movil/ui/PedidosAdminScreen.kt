package com.example.vapeon_movil.ui

import com.example.vapeon_movil.utils.moneda

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosAdminScreen(
    usuario: Usuario?, pedidoViewModel: PedidoViewModel,
    volver: () -> Unit, irDetallePedido: (String) -> Unit
) {
    val esAdmin = usuario?.rol?.equals("ADMIN", true) == true && usuario.id.isNotBlank()
    LaunchedEffect(usuario?.id, usuario?.rol) { pedidoViewModel.cargarPedidosAdmin(usuario) }
    BackHandler(onBack = volver)
    VapeON_MovilTheme {
        Scaffold(containerColor = VapeOnBackground, topBar = {
            TopAppBar(title = { Text("Pedidos", color = VapeOnGold) }, navigationIcon = {
                IconButton(onClick = volver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver al panel", tint = VapeOnTextPrimary)
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground))
        }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    !esAdmin -> item { Text("Solo un administrador puede ver los pedidos.",
                        color = MaterialTheme.colorScheme.error) }
                    pedidoViewModel.usuarioAdminId != usuario?.id || pedidoViewModel.cargandoPedidosAdmin ->
                        item { CircularProgressIndicator(color = VapeOnGold) }
                    pedidoViewModel.errorPedidosAdmin != null -> item {
                        Text(pedidoViewModel.errorPedidosAdmin.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Button(onClick = { pedidoViewModel.cargarPedidosAdmin(usuario) },
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Reintentar") }
                    }
                    else -> {
                        if (pedidoViewModel.invalidosAdmin > 0) item {
                            Text("${pedidoViewModel.invalidosAdmin} pedido(s) contienen datos inválidos y no se pueden mostrar.",
                                color = MaterialTheme.colorScheme.error)
                        }
                        item {
                            OutlinedButton(onClick = { pedidoViewModel.cargarPedidosAdmin(usuario) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Actualizar pedidos") }
                        }
                        if (pedidoViewModel.pedidosAdmin.isEmpty()) item {
                            Text(if (pedidoViewModel.invalidosAdmin > 0) "No hay pedidos válidos disponibles." else "No hay pedidos registrados.", color = VapeOnTextPrimary)
                        }
                        items(pedidoViewModel.pedidosAdmin, key = { it.id }) { pedido ->
                            Card(onClick = { irDetallePedido(pedido.id) }, shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Pedido ${pedido.id}", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                                    Text(pedido.nombreCliente, color = VapeOnTextPrimary)
                                    Text(fechaPedidoTexto(pedido.fecha), color = VapeOnTextPrimary)
                                    Text("Total: ${moneda(pedido.total)}", color = VapeOnGold)
                                    Text("Estado: ${pedido.estado}", color = VapeOnTextPrimary)
                                    Text("Ver detalle y actualizar estado", color = VapeOnGold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
