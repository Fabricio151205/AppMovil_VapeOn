package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.entities.calcularNumeroSecuencialPedido
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.PedidoViewModel
import java.time.Instant

/** Filtros visibles en la lista de pedidos del administrador. */
internal val FILTROS_PEDIDOS_ADMIN = listOf("Todos", "Registrados", "En camino", "Completados")

/**
 * Indica si un pedido debe mostrarse al elegir un filtro.
 *
 * La vista principal ("Todos") oculta los pedidos entregados: estos solo se
 * consultan en "Completados". Así el administrador trabaja únicamente con los
 * pedidos registrados y en camino.
 */
internal fun pedidoCoincideFiltroAdmin(estado: String, filtro: String): Boolean {
    val normalizado = estado.trim().uppercase()
    val entregado = normalizado == "ENTREGADO" || normalizado == "COMPLETADO"
    return when (filtro) {
        "Registrados" -> normalizado == "REGISTRADO"
        "En camino" -> normalizado == "EN CAMINO" || normalizado == "EN_CURSO" ||
            normalizado == "EN CURSO"
        "Completados" -> entregado
        else -> !entregado
    }
}

internal fun filtrarPedidosAdmin(pedidos: List<Pedido>, filtro: String): List<Pedido> =
    pedidos.filter { pedidoCoincideFiltroAdmin(it.estado, filtro) }

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

    // Ordenados del más antiguo (atrasado) al más reciente.
    val pedidosOrdenados = remember(pedidoViewModel.pedidosAdmin) {
        pedidoViewModel.pedidosAdmin.sortedBy { Instant.parse(it.fecha) }
    }
    var filtro by rememberSaveable { mutableStateOf(FILTROS_PEDIDOS_ADMIN.first()) }
    val pedidosFiltrados = remember(pedidosOrdenados, filtro) {
        filtrarPedidosAdmin(pedidosOrdenados, filtro)
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
                Column {
                    Text(
                        text = "Pedidos",
                        color = VapeOnTextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Historial y seguimiento de los pedidos",
                        color = VapeOnTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filtros por estado sobre los datos reales de Firestore.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FILTROS_PEDIDOS_ADMIN.forEach { opcion ->
                    val seleccionado = opcion == filtro
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (seleccionado) VapeOnRed else VapeOnRedDark)
                            .clickable { filtro = opcion }
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
                    if (pedidosFiltrados.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    pedidosOrdenados.isEmpty() && pedidoViewModel.invalidosAdmin > 0 ->
                                        "No hay pedidos válidos disponibles."
                                    filtro == "Completados" -> "No hay pedidos completados."
                                    filtro == "Registrados" -> "No hay pedidos registrados."
                                    filtro == "En camino" -> "No hay pedidos en camino."
                                    else -> "No hay pedidos activos por atender."
                                },
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
                            items(pedidosFiltrados, key = { it.id }) { pedido ->
                                PedidoCard(
                                    numeroPedido = calcularNumeroSecuencialPedido(
                                        pedido.id, pedidoViewModel.pedidosAdmin),
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
