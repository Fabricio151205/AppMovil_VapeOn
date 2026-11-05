package com.example.vapeon_movil.ui

import com.example.vapeon_movil.utils.moneda

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.entities.calcularNumeroSecuencialPedido
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.PedidoViewModel
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosAdminScreen(
    usuario: Usuario?,
    pedidoViewModel: PedidoViewModel,
    volver: () -> Unit,
    irDetallePedido: (String) -> Unit
) {
    val esAdmin = usuario?.rol?.equals("ADMIN", true) == true && usuario.id.isNotBlank()
    LaunchedEffect(usuario?.id, usuario?.rol) {
        pedidoViewModel.cargarPedidosAdmin(usuario)
    }
    BackHandler(onBack = volver)

    // Ordenados del más antiguo (atrasado) al más reciente
    val pedidosOrdenados = remember(pedidoViewModel.pedidosAdmin) {
        pedidoViewModel.pedidosAdmin.sortedBy { Instant.parse(it.fecha) }
    }

    Surface(
        color = VapeOnBackground,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ENCABEZADO CON FLECHA Y TÍTULO "Detalle Pedido"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = volver) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver al panel",
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Detalle Pedido",
                    color = VapeOnTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                !esAdmin -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Solo un administrador puede ver los pedidos.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                }
                pedidoViewModel.usuarioAdminId != usuario?.id || pedidoViewModel.cargandoPedidosAdmin -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = VapeOnGold,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Cargando pedidos...",
                                color = VapeOnTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                pedidoViewModel.errorPedidosAdmin != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = pedidoViewModel.errorPedidosAdmin.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { pedidoViewModel.cargarPedidosAdmin(usuario) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
                else -> {
                    if (pedidosOrdenados.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (pedidoViewModel.invalidosAdmin > 0) "No hay pedidos válidos disponibles." else "No hay pedidos registrados.",
                                color = VapeOnTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(pedidosOrdenados, key = { it.id }) { pedido ->
                                val numeroSecuencial = calcularNumeroSecuencialPedido(pedido.id, pedidoViewModel.pedidosAdmin)
                                val textoPedido = String.format(java.util.Locale.ROOT, "Pedido N° %04d", numeroSecuencial)
                                val cantidadTotalOrden = pedido.productos.sumOf { it.cantidad }
                                val resumenProductos = pedido.productos.take(2).joinToString(", ") { "${it.cantidad}x ${it.nombre}" }

                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { irDetallePedido(pedido.id) }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // 1. NÚMERO DE PEDIDO Y ESTADO
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = textoPedido,
                                                color = VapeOnTextSecondary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Estado: ${pedido.estado}",
                                                color = VapeOnGold,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // 2. CLIENTE Y FECHA
                                        Text(
                                            text = "Cliente: ${pedido.nombreCliente}",
                                            color = VapeOnTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = fechaPedidoTexto(pedido.fecha),
                                            color = VapeOnTextSecondary,
                                            fontSize = 13.sp
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // 3. PRODUCTOS Y TOTAL
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = resumenProductos.ifBlank { "Productos de la compra" },
                                                color = VapeOnTextPrimary,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = moneda(pedido.total),
                                                color = VapeOnTextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // 4. LÍNEA DIVISORA DORADA
                                        HorizontalDivider(color = VapeOnGold, thickness = 2.dp)

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // 5. FILA INFERIOR: CANTIDAD Y ENLACE
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Cant.: $cantidadTotalOrden",
                                                color = VapeOnTextSecondary,
                                                fontSize = 13.sp
                                            )

                                            Text(
                                                text = "Ver detalle y actualizar estado",
                                                color = VapeOnGold,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.clickable { irDetallePedido(pedido.id) }
                                            )
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
}
