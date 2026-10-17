package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.PedidoViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun fechaPedidoTexto(fecha: String): String = try {
    DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
        .format(Instant.parse(fecha).atZone(ZoneId.systemDefault()))
} catch (e: Exception) {
    fecha
}

@Composable
fun HistorialComprasScreen(
    usuario: Usuario?,
    pedidoViewModel: PedidoViewModel,
    volver: () -> Unit,
    irDetallePedido: (String) -> Unit
) {
    val usuarioId = usuario?.takeIf { it.rol.equals("CLIENTE", true) }?.id.orEmpty()
    var filtroEstado by remember { mutableStateOf("Todos") }

    LaunchedEffect(usuarioId) {
        pedidoViewModel.actualizarUsuarioHistorial(usuario)
        if (usuarioId.isNotBlank()) {
            pedidoViewModel.cargarHistorial(usuarioId)
        }
    }

    BackHandler(onBack = volver)

    MenuScreen(
        irCatalogo = volver,
        onPerfilClick = { /* Perfil */ },
        onTituloClick = { /* Titulo */ }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==========================================
                // 1. TÍTULO Y SUBTÍTULO
                // ==========================================
                item {
                    Column {
                        Text(
                            text = "Pedidos",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = VapeOnTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Historial y seguimiento de tus pedidos",
                            fontSize = 14.sp,
                            color = VapeOnTextSecondary
                        )
                    }
                }

                // ==========================================
                // 2. CHIPS DE FILTRO (Todos, En Curso, Completados)
                // ==========================================
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val opcionesFiltro = listOf("Todos", "En Curso", "Completados")
                        opcionesFiltro.forEach { opcion ->
                            val estaSeleccionado = opcion == filtroEstado
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (estaSeleccionado) VapeOnRed else VapeOnRedDark)
                                    .clickable { filtroEstado = opcion }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opcion,
                                    color = VapeOnTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 3. ESTADOS DE CARGA / ERROR / LISTA
                // ==========================================
                when {
                    usuarioId.isBlank() -> {
                        item {
                            Text(
                                text = "Inicia sesión como cliente para consultar tus compras.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                        }
                    }

                    pedidoViewModel.usuarioHistorialId != usuarioId || pedidoViewModel.cargandoHistorial -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = VapeOnGold)
                            }
                        }
                    }

                    pedidoViewModel.errorHistorial != null -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = pedidoViewModel.errorHistorial.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { pedidoViewModel.cargarHistorial(usuarioId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                                ) {
                                    Text("Reintentar", color = VapeOnTextPrimary)
                                }
                            }
                        }
                    }

                    else -> {
                        // Lista filtrada estrictamente por el usuarioId del cliente
                        val listaCliente = pedidoViewModel.historial.filter { it.usuarioId == usuarioId }

                        val listaFiltrada = when (filtroEstado) {
                            "En Curso" -> listaCliente.filter {
                                it.estado.equals("EN_CURSO", true) ||
                                it.estado.equals("EN CURSO", true) ||
                                it.estado.equals("REGISTRADO", true)
                            }
                            "Completados" -> listaCliente.filter {
                                it.estado.equals("ENTREGADO", true) ||
                                it.estado.equals("COMPLETADO", true)
                            }
                            else -> listaCliente
                        }

                        if (listaFiltrada.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No tienes pedidos registrados en esta categoría.",
                                        color = VapeOnTextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = listaFiltrada,
                                key = { _, item -> item.id }
                            ) { index, pedido ->
                                PedidoCard(
                                    numeroPedido = index + 1,
                                    pedido = pedido,
                                    onClickDetalle = { irDetallePedido(pedido.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PedidoCard(
    numeroPedido: Int,
    pedido: Pedido,
    onClickDetalle: () -> Unit
) {
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
            // Título: Pedido N° X
            Text(
                text = "Pedido N° $numeroPedido",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VapeOnTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fila: Fecha
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

            Spacer(modifier = Modifier.height(6.dp))

            // Fila: Productos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Productos:",
                    color = VapeOnTextSecondary,
                    fontSize = 14.sp
                )
                val cantidadItems = pedido.productos.sumOf { it.cantidad }
                Text(
                    text = "$cantidadItems Items",
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Fila: Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total:",
                    color = VapeOnGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "S/. ${"%.2f".format(pedido.total.toDouble())}",
                    color = VapeOnGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fila Inferior: Pill Badge de Estado y Botón Ver Detalle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge Píldora de Estado
                val estadoNormalizado = pedido.estado.uppercase()
                val (colorFondo, colorTexto, textoMostrar) = when {
                    estadoNormalizado.contains("CURSO") -> Triple(VapeOnRedDark, VapeOnTextPrimary, "En Curso")
                    estadoNormalizado.contains("ENTREGADO") || estadoNormalizado.contains("COMPLETADO") -> Triple(Color(0xFF4A3C12), VapeOnGold, "Entregado")
                    else -> Triple(Color(0xFF383838), VapeOnTextSecondary, "Registrado")
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colorFondo)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = textoMostrar,
                        color = colorTexto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Enlace Ver Detalle
                Text(
                    text = "Ver detalle",
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onClickDetalle() }
                )
            }
        }
    }
}
