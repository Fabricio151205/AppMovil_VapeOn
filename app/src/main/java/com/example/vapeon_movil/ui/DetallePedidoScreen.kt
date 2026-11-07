package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.BuildConfig
import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.ProductoPedido
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.entities.calcularNumeroSecuencialPedido
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.moneda
import com.example.vapeon_movil.viewmodels.PedidoViewModel
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

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
    val pedidoVisible = if (esAdmin) pedidoViewModel.detalleAdmin else pedidoViewModel.detallePedido
    var mostrarMapa by rememberSaveable(pedidoId) { mutableStateOf(false) }
    BackHandler {
        if (mostrarMapa) mostrarMapa = false
        else if (!guardando) volver()
    }
    VapeON_MovilTheme {
        if (mostrarMapa && pedidoVisible != null) {
            VerUbicacionScreen(
                direccion = pedidoVisible.direccionEntrega,
                volver = { mostrarMapa = false },
                latitud = pedidoVisible.latitud,
                longitud = pedidoVisible.longitud
            )
            return@VapeON_MovilTheme
        }
        Surface(modifier = Modifier.fillMaxSize(), color = VapeOnBackground) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                EncabezadoDetalle(volver = volver, habilitado = !guardando)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Detalle del Pedido",
                    color = VapeOnTextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                val pedido = pedidoVisible
                val sesionDetalle = if (esAdmin) pedidoViewModel.usuarioAdminId else pedidoViewModel.usuarioHistorialId
                val cargando = if (esAdmin) pedidoViewModel.cargandoDetalleAdmin else pedidoViewModel.cargandoDetalle
                val error = if (esAdmin) pedidoViewModel.errorDetalleAdmin else pedidoViewModel.errorDetalle
                when {
                    usuarioId.isBlank() -> Text(
                        "Inicia sesión como cliente para consultar el pedido.",
                        color = MaterialTheme.colorScheme.error
                    )
                    sesionDetalle != usuarioId || cargando -> CargandoDetalle()
                    error != null -> {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = actualizar, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Reintentar")
                        }
                    }
                    pedido != null && (esAdmin || pedido.usuarioId == usuarioId) && pedido.id == pedidoId -> {
                        val listaBase = if (esAdmin) pedidoViewModel.pedidosAdmin else listOf(pedido)
                        val numeroSecuencial = calcularNumeroSecuencialPedido(pedido.id, listaBase)
                        ContenidoDetallePedido(
                            pedido = pedido,
                            numeroSecuencial = numeroSecuencial,
                            esAdmin = esAdmin,
                            guardando = guardando,
                            usuario = usuario,
                            pedidoViewModel = pedidoViewModel,
                            verMapa = { mostrarMapa = true }
                        )
                    }
                    else -> Text("No hay un pedido disponible para esta sesión.", color = VapeOnTextPrimary)
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun EncabezadoDetalle(volver: () -> Unit, habilitado: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = volver, enabled = habilitado) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver a pedidos",
                tint = VapeOnTextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "VAPEON",
            color = VapeOnGold,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(VapeOnRed),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Perfil",
                tint = VapeOnTextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun CargandoDetalle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = VapeOnGold, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("Cargando pedido...", color = VapeOnTextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ContenidoDetallePedido(
    pedido: Pedido,
    numeroSecuencial: Int,
    esAdmin: Boolean,
    guardando: Boolean,
    usuario: Usuario?,
    pedidoViewModel: PedidoViewModel,
    verMapa: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Pedido N° $numeroSecuencial",
            color = VapeOnTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Fecha: ${fechaPedidoTexto(pedido.fecha)}",
            color = VapeOnTextPrimary,
            fontSize = 12.sp
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Estado del Pedido:", color = VapeOnGold, fontSize = 13.sp)
        Text(
            text = pedido.estado,
            color = VapeOnGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text("Dirección de entrega:", color = VapeOnTextPrimary, fontSize = 13.sp)
    Text(
        text = pedido.direccionEntrega,
        color = VapeOnTextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )

    if (pedido.direccionEntrega.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        MapaEntregaCard(pedido = pedido, verMapa = verMapa)
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text("Cliente: ${pedido.nombreCliente}", color = VapeOnTextPrimary, fontSize = 13.sp)
    Text("Teléfono: ${pedido.telefono}", color = VapeOnTextPrimary, fontSize = 13.sp)

    Spacer(modifier = Modifier.height(16.dp))

    EstadoPedidoCard(
        pedido = pedido,
        esAdmin = esAdmin,
        guardando = guardando,
        usuario = usuario,
        pedidoViewModel = pedidoViewModel
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "PRODUCTOS COMPRADOS",
        color = VapeOnTextPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(10.dp))

    pedido.productos.forEachIndexed { indice, producto ->
        ProductoComprado(producto)
        if (indice != pedido.productos.lastIndex) Spacer(modifier = Modifier.height(16.dp))
    }

    Spacer(modifier = Modifier.height(20.dp))
    ResumenCompraCard(pedido)
}

@Composable
private fun EstadoPedidoCard(
    pedido: Pedido,
    esAdmin: Boolean,
    guardando: Boolean,
    usuario: Usuario?,
    pedidoViewModel: PedidoViewModel
) {
    val estados = listOf(
        "R" to "REGISTRADO",
        "EC" to "EN CAMINO",
        "E" to "ENTREGADO"
    )
    val estadoActual = pedido.estado.trim().uppercase()
    val indiceActual = estados.indexOfFirst { it.second == estadoActual }.coerceAtLeast(0)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Estado del Pedido",
                color = VapeOnTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                estados.forEachIndexed { indice, (sigla, estado) ->
                    if (indice > 0) LineaPunteada(modifier = Modifier.weight(1f))
                    PasoEstado(
                        sigla = sigla,
                        alcanzado = indice <= indiceActual,
                        habilitado = esAdmin && !guardando,
                        onClick = {
                            if (estado != pedido.estado)
                                pedidoViewModel.actualizarEstadoPedido(usuario, pedido.id, estado)
                        }
                    )
                }
            }

            if (guardando) {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(color = VapeOnGold, modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp)
            }
            pedidoViewModel.errorEstado?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            pedidoViewModel.mensajeEstado?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(it, color = VapeOnGold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LineaPunteada(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(7) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(VapeOnCardBorder)
            )
            if (it != 6) Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
private fun PasoEstado(
    sigla: String,
    alcanzado: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (alcanzado) VapeOnRed else VapeOnRedDark)
            .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = sigla,
            color = VapeOnTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProductoComprado(producto: ProductoPedido) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = producto.nombre,
                color = VapeOnGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = producto.marca,
                color = VapeOnTextPrimary,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        FilaDetalle("Cantidad:", producto.cantidad.toString())
        FilaDetalle("Precio unitario:", moneda(producto.precioUnitario))
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = VapeOnGold, thickness = 1.dp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Subtotal:", color = VapeOnGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(moneda(producto.subtotal), color = VapeOnGold, fontSize = 14.sp,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FilaDetalle(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(etiqueta, color = VapeOnTextPrimary, fontSize = 13.sp)
        Text(valor, color = VapeOnTextPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun ResumenCompraCard(pedido: Pedido) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resumen de la Compra",
                color = VapeOnTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            FilaResumen("Sub Total", moneda(pedido.subtotal))
            FilaResumen("Delivery", moneda(pedido.delivery))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TOTAL A PAGAR", color = VapeOnTextPrimary, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold)
                Text(moneda(pedido.total), color = VapeOnTextPrimary, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = VapeOnTextPrimary, thickness = 1.5.dp)
            Spacer(modifier = Modifier.height(2.dp))
            FilaResumen("Monto Pagado:", moneda(pedido.montoPagado))
            FilaResumen("Vuelto:", moneda(pedido.vuelto))
            FilaResumen("Método de Pago:", pedido.metodoPago)
        }
    }
}

@Composable
private fun FilaResumen(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(etiqueta, color = VapeOnTextPrimary, fontSize = 13.sp)
        Text(valor, color = VapeOnTextPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun MapaEntregaCard(pedido: Pedido, verMapa: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (pedido.tieneUbicacionEntrega) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    if (BuildConfig.MAPS_API_KEY_CONFIGURED) {
                        MapaEntrega(pedido, Modifier.fillMaxSize())
                    } else {
                        Box(Modifier.padding(16.dp)) { FaltaApiKey() }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = VapeOnGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (pedido.tieneUbicacionEntrega)
                        "Punto exacto confirmado por el cliente"
                    else "Ubicación aproximada por dirección",
                    color = VapeOnTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (pedido.tieneUbicacionEntrega) "Ver mapa" else "Ver ubicación",
                    color = VapeOnGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = verMapa)
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Mapa de solo lectura con el punto exacto confirmado por el cliente.
 *
 * Se muestra en modo "lite" para que la vista sea liviana dentro del detalle y
 * no interfiera con el desplazamiento vertical de la pantalla.
 */
@Composable
private fun MapaEntrega(pedido: Pedido, modifier: Modifier = Modifier) {
    val latitud = pedido.latitud ?: return
    val longitud = pedido.longitud ?: return
    val punto = remember(latitud, longitud) { LatLng(latitud, longitud) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(punto, 17f)
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false,
            scrollGesturesEnabled = false,
            zoomGesturesEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false
        )
    ) {
        Marker(
            state = remember(punto) { MarkerState(position = punto) },
            title = pedido.direccionEntrega,
            snippet = "Punto de entrega"
        )
    }
}
