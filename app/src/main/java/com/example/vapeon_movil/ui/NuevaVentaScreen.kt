package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.ErroresVentaAdmin
import com.example.vapeon_movil.entities.LineaVentaAdmin
import com.example.vapeon_movil.entities.METODO_PAGO_TARJETA
import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.entities.etiquetaOrigenVenta
import com.example.vapeon_movil.entities.validarVentaAdmin
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.moneda
import com.example.vapeon_movil.viewmodels.NuevaVentaViewModel
import com.example.vapeon_movil.viewmodels.ProductoViewModel

/**
 * Pantalla administrativa para registrar una venta.
 *
 * La venta se guarda como pedido (`origen = ADMIN`) y descuenta stock en el
 * mismo commit atómico del checkout. El administrador decide si la venta se
 * entrega en el momento (sin delivery) o si queda programada (con delivery).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaVentaScreen(
    usuario: Usuario?,
    viewModel: NuevaVentaViewModel,
    productoViewModel: ProductoViewModel,
    volver: () -> Unit
) {
    LaunchedEffect(usuario?.id, usuario?.rol) {
        viewModel.actualizarAdmin(usuario)
        if (productoViewModel.productos.isEmpty()) productoViewModel.cargarProductos()
    }
    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let { viewModel.consumirMensaje() }
    }
    BackHandler {
        if (!viewModel.guardando) volver()
    }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.error) {
        viewModel.error?.let { snackbar.showSnackbar(it) }
    }

    var busqueda by rememberSaveable { mutableStateOf("") }
    var saboresSeleccionados by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var intentoEnvio by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel.ventaRegistrada) {
        if (viewModel.ventaRegistrada != null) intentoEnvio = false
    }

    val errores = if (intentoEnvio) validarVentaAdmin(
        nombreCliente = viewModel.nombreCliente,
        telefono = viewModel.telefono,
        conDelivery = viewModel.conDelivery,
        direccion = viewModel.direccion,
        lineas = viewModel.lineas,
        montoTexto = viewModel.montoRecibido,
        metodoPago = viewModel.metodoPago,
        titularTarjeta = viewModel.titularTarjeta,
        ultimos4Tarjeta = viewModel.ultimos4Tarjeta,
        vencimientoTarjeta = viewModel.vencimientoTarjeta
    ) else ErroresVentaAdmin()

    val bloqueado = viewModel.guardando || viewModel.requiereComprobacion || viewModel.adminId == null
    val productos = remember(productoViewModel.productos, busqueda) {
        val texto = busqueda.trim()
        if (texto.isBlank()) {
            productoViewModel.productos
        } else {
            productoViewModel.productos.filter {
                it.nombre.contains(texto, ignoreCase = true) ||
                    it.marca.contains(texto, ignoreCase = true)
            }
        }
    }

    viewModel.ventaRegistrada?.let { pedido ->
        AlertDialog(
            onDismissRequest = { viewModel.consumirVentaRegistrada() },
            icon = {
                Icon(
                    imageVector = if (pedido.estado == "ENTREGADO") Icons.Default.PointOfSale
                    else Icons.Default.DeliveryDining,
                    contentDescription = null,
                    tint = VapeOnGold
                )
            },
            title = { Text("Venta registrada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${pedido.nombreCliente} · ${moneda(pedido.total)}")
                    Text(
                        text = etiquetaOrigenVenta(pedido.origen, pedido.conDelivery),
                        color = VapeOnTextSecondary
                    )
                    Text(
                        text = if (pedido.estado == "ENTREGADO") {
                            "La venta ya suma en Caja."
                        } else {
                            "Queda como pedido pendiente de entrega."
                        },
                        color = VapeOnTextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.consumirVentaRegistrada() }) {
                    Text("Registrar otra", color = VapeOnGold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.consumirVentaRegistrada()
                    volver()
                }) {
                    Text("Volver al panel", color = VapeOnTextSecondary)
                }
            },
            containerColor = VapeOnSurface
        )
    }

    Scaffold(
        containerColor = VapeOnBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nueva venta", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Registra una venta del cliente desde el panel",
                            color = VapeOnTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = volver, enabled = !viewModel.guardando) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al panel",
                            tint = VapeOnTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { productoViewModel.cargarProductos() },
                        enabled = !productoViewModel.cargando
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar catálogo",
                            tint = if (productoViewModel.cargando) VapeOnTextMuted else VapeOnGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
            )
        },
        bottomBar = {
            ResumenVenta(
                viewModel = viewModel,
                productoViewModel = productoViewModel,
                bloqueado = bloqueado,
                onConfirmar = {
                    intentoEnvio = true
                    viewModel.confirmar(productoViewModel)
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            if (viewModel.adminId == null) {
                item {
                    TarjetaAviso(
                        icono = Icons.Default.Info,
                        texto = "Solo un administrador puede registrar ventas.",
                        color = VapeOnError
                    )
                }
            }

            if (viewModel.requiereComprobacion) {
                item {
                    TarjetaAviso(
                        icono = Icons.Default.Info,
                        texto = "Hay una venta pendiente de comprobar. Los datos quedaron bloqueados " +
                            "para no duplicar el pedido ni el descuento de stock.",
                        color = VapeOnGold
                    )
                }
            }

            viewModel.error?.let { mensaje ->
                item {
                    TarjetaAviso(
                        icono = Icons.Default.Info,
                        texto = mensaje,
                        color = VapeOnError
                    )
                }
            }

            item {
                SeccionVenta("Tipo de entrega") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OpcionEntrega(
                            seleccionada = !viewModel.conDelivery,
                            habilitada = !bloqueado,
                            icono = Icons.Default.Storefront,
                            titulo = "Venta en tienda",
                            descripcion = "Cobras ahora y la venta suma en Caja.",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.actualizarConDelivery(false) }
                        )
                        OpcionEntrega(
                            seleccionada = viewModel.conDelivery,
                            habilitada = !bloqueado,
                            icono = Icons.Default.DeliveryDining,
                            titulo = "Con delivery",
                            descripcion = "Queda pendiente hasta entregar el pedido.",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.actualizarConDelivery(true) }
                        )
                    }
                }
            }

            item {
                SeccionVenta("Datos del cliente") {
                    CampoVenta(
                        valor = viewModel.nombreCliente,
                        onCambio = viewModel::actualizarNombreCliente,
                        etiqueta = "Nombre del cliente *",
                        habilitado = !bloqueado,
                        error = errores.nombre
                    )
                    Spacer(Modifier.height(10.dp))
                    CampoVenta(
                        valor = viewModel.telefono,
                        onCambio = viewModel::actualizarTelefono,
                        etiqueta = if (viewModel.conDelivery) "Teléfono *" else "Teléfono",
                        habilitado = !bloqueado,
                        tipoTeclado = KeyboardType.Phone,
                        error = errores.telefono
                    )
                    if (viewModel.conDelivery) {
                        Spacer(Modifier.height(10.dp))
                        CampoVenta(
                            valor = viewModel.direccion,
                            onCambio = viewModel::actualizarDireccion,
                            etiqueta = "Dirección de entrega *",
                            habilitado = !bloqueado,
                            error = errores.direccion
                        )
                    }
                }
            }

            item {
                SeccionVenta("Productos") {
                    CampoBusquedaVenta(
                        valor = busqueda,
                        onCambio = { busqueda = it },
                        habilitado = !bloqueado
                    )
                }
            }

            when {
                productoViewModel.cargando && productoViewModel.productos.isEmpty() -> item {
                    EstadoCatalogo("Cargando catálogo…")
                }
                productoViewModel.error != null && productoViewModel.productos.isEmpty() -> item {
                    EstadoCatalogo("No se pudo cargar el catálogo. Usa el botón actualizar.")
                }
                productos.isEmpty() -> item {
                    EstadoCatalogo("No hay productos que coincidan con la búsqueda.")
                }
                else -> items(productos, key = { it.id }) { producto ->
                    TarjetaProductoVenta(
                        producto = producto,
                        saborSeleccionado = saboresSeleccionados[producto.id],
                        habilitada = !bloqueado,
                        onSabor = { sabor ->
                            saboresSeleccionados = saboresSeleccionados + (producto.id to sabor)
                        },
                        onAgregar = {
                            viewModel.agregarProducto(producto, saboresSeleccionados[producto.id])
                        }
                    )
                }
            }

            item {
                SeccionVenta(
                    titulo = "Detalle de la venta",
                    subtitulo = if (viewModel.lineas.isEmpty()) null
                    else "${viewModel.lineas.sumOf { it.cantidad }} unidad(es)"
                ) {
                    if (viewModel.lineas.isEmpty()) {
                        EstadoCatalogo("Aún no agregaste productos.")
                        errores.productos?.let {
                            Text(it, color = VapeOnError, fontSize = 12.sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            viewModel.lineas.forEach { linea ->
                                FilaLineaVenta(
                                    linea = linea,
                                    habilitada = !bloqueado,
                                    onSumar = { viewModel.cambiarCantidad(linea, 1) },
                                    onRestar = { viewModel.cambiarCantidad(linea, -1) },
                                    onQuitar = { viewModel.quitarLinea(linea) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SeccionVenta("Pago") {
                    Text(
                        text = "Selecciona cómo se cobra esta venta.",
                        color = VapeOnTextSecondary,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NuevaVentaViewModel.METODOS_PAGO_VENTA.forEach { metodo ->
                            MetodoPagoVenta(
                                metodo = metodo,
                                seleccionado = viewModel.metodoPago == metodo,
                                habilitado = !bloqueado,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.actualizarMetodoPago(metodo) }
                            )
                        }
                    }
                    errores.metodoPago?.let {
                        Text(it, color = VapeOnError, fontSize = 12.sp)
                    }

                    if (viewModel.metodoPago == METODO_PAGO_TARJETA) {
                        Spacer(Modifier.height(4.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = VapeOnSurfaceVariant),
                            border = BorderStroke(1.dp, VapeOnDivider),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = VapeOnGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Datos de la tarjeta",
                                        color = VapeOnTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                CampoVenta(
                                    valor = viewModel.titularTarjeta,
                                    onCambio = viewModel::actualizarTitularTarjeta,
                                    etiqueta = "Titular de la tarjeta *",
                                    habilitado = !bloqueado,
                                    error = errores.titularTarjeta
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CampoVenta(
                                        valor = viewModel.ultimos4Tarjeta,
                                        onCambio = viewModel::actualizarUltimos4Tarjeta,
                                        etiqueta = "Últimos 4 dígitos *",
                                        habilitado = !bloqueado,
                                        tipoTeclado = KeyboardType.Number,
                                        error = errores.ultimos4Tarjeta,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CampoVenta(
                                        valor = viewModel.vencimientoTarjeta,
                                        onCambio = viewModel::actualizarVencimientoTarjeta,
                                        etiqueta = "Vencimiento *",
                                        habilitado = !bloqueado,
                                        tipoTeclado = KeyboardType.Number,
                                        error = errores.vencimientoTarjeta,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = "Pago simulado: no se guarda el número completo ni el CVV.",
                                    color = VapeOnTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    CampoVenta(
                        valor = viewModel.montoRecibido,
                        onCambio = viewModel::actualizarMontoRecibido,
                        etiqueta = if (viewModel.metodoPago == METODO_PAGO_TARJETA)
                            "Monto confirmado (opcional)" else "Monto recibido (opcional)",
                        habilitado = !bloqueado,
                        tipoTeclado = KeyboardType.Decimal,
                        error = errores.monto
                    )
                    val recibido = Pedido.leerMonto(viewModel.montoRecibido)
                    val total = viewModel.total
                    if (recibido != null && recibido >= total) {
                        Text(
                            text = "Vuelto: ${moneda(recibido - total)}",
                            color = VapeOnGold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionVenta(
    titulo: String,
    subtitulo: String? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 16.dp)
                    .background(VapeOnGold, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = titulo.uppercase(),
                color = VapeOnGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            if (subtitulo != null) {
                Spacer(Modifier.width(8.dp))
                Text(text = subtitulo, color = VapeOnTextSecondary, fontSize = 12.sp)
            }
        }
        contenido()
    }
}

@Composable
private fun TarjetaAviso(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text = texto, color = VapeOnTextPrimary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun OpcionEntrega(
    seleccionada: Boolean,
    habilitada: Boolean,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    descripcion: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = habilitada,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) VapeOnSelected else VapeOnCardBackground
        ),
        border = BorderStroke(
            width = if (seleccionada) 2.dp else 1.dp,
            color = if (seleccionada) VapeOnGold else VapeOnDivider
        ),
        modifier = modifier.heightIn(min = 152.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = if (seleccionada) VapeOnGold else VapeOnTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                RadioButton(
                    selected = seleccionada,
                    onClick = onClick,
                    enabled = habilitada,
                    colors = RadioButtonDefaults.colors(selectedColor = VapeOnGold),
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = titulo,
                color = VapeOnTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = descripcion,
                color = VapeOnTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetodoPagoVenta(
    metodo: String,
    seleccionado: Boolean,
    habilitado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val icono = when (metodo) {
        "EFECTIVO" -> Icons.Default.Payments
        "YAPE" -> Icons.Default.PhoneAndroid
        else -> Icons.Default.CreditCard
    }
    Card(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) VapeOnSelected else VapeOnCardBackground
        ),
        border = BorderStroke(
            width = if (seleccionado) 2.dp else 1.dp,
            color = if (seleccionado) VapeOnGold else VapeOnDivider
        ),
        modifier = modifier.height(72.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = if (seleccionado) VapeOnGold else VapeOnTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = metodo,
                color = if (seleccionado) VapeOnTextPrimary else VapeOnTextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CampoVenta(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    habilitado: Boolean,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        enabled = habilitado,
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { mensaje -> { Text(mensaje, color = VapeOnError) } },
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = VapeOnTextPrimary,
            unfocusedTextColor = VapeOnTextPrimary,
            disabledTextColor = VapeOnTextSecondary,
            focusedBorderColor = VapeOnGold,
            unfocusedBorderColor = VapeOnBorder,
            disabledBorderColor = VapeOnDivider,
            focusedLabelColor = VapeOnGold,
            unfocusedLabelColor = VapeOnTextSecondary,
            disabledLabelColor = VapeOnTextMuted,
            cursorColor = VapeOnGold
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun CampoBusquedaVenta(
    valor: String,
    onCambio: (String) -> Unit,
    habilitado: Boolean
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        enabled = habilitado,
        placeholder = { Text("Buscar producto o marca") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = VapeOnTextSecondary)
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = VapeOnTextPrimary,
            unfocusedTextColor = VapeOnTextPrimary,
            focusedBorderColor = VapeOnGold,
            unfocusedBorderColor = VapeOnBorder,
            cursorColor = VapeOnGold
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EstadoCatalogo(texto: String) {
    Text(
        text = texto,
        color = VapeOnTextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun TarjetaProductoVenta(
    producto: Producto,
    saborSeleccionado: String?,
    habilitada: Boolean,
    onSabor: (String) -> Unit,
    onAgregar: () -> Unit
) {
    var menuAbierto by remember { mutableStateOf(false) }
    val sabores = producto.stockPorSabor.keys.toList()
    val stock = producto.stockTotal
    val sinStock = stock <= 0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = producto.nombre,
                        color = VapeOnTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = producto.marca,
                        color = VapeOnTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = moneda(producto.precio),
                        color = VapeOnGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (sinStock) "Sin stock" else "$stock u.",
                        color = if (sinStock) VapeOnError else VapeOnTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (sabores.isNotEmpty()) {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = habilitada && !sinStock) { menuAbierto = true }
                            .background(VapeOnInputBackground, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = saborSeleccionado ?: "Elegir sabor",
                            color = if (saborSeleccionado == null) VapeOnTextSecondary else VapeOnTextPrimary,
                            fontSize = 13.sp
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = VapeOnTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuAbierto,
                        onDismissRequest = { menuAbierto = false }
                    ) {
                        sabores.forEach { sabor ->
                            val disponible = producto.obtenerStockDeSabor(sabor)
                            DropdownMenuItem(
                                text = {
                                    Text("$sabor · $disponible u.")
                                },
                                enabled = disponible > 0,
                                onClick = {
                                    onSabor(sabor)
                                    menuAbierto = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onAgregar,
                    enabled = habilitada && !sinStock,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = VapeOnRedDark,
                        contentColor = VapeOnTextPrimary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (sinStock) "Sin stock" else "Agregar", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun FilaLineaVenta(
    linea: LineaVentaAdmin,
    habilitada: Boolean,
    onSumar: () -> Unit,
    onRestar: () -> Unit,
    onQuitar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = linea.nombre,
                        color = VapeOnTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(linea.sabor?.let { "$it · " }.orEmpty())
                            append(moneda(linea.precioUnitario))
                            append(" c/u")
                        },
                        color = VapeOnTextSecondary,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onQuitar, enabled = habilitada) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar línea",
                        tint = VapeOnError,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StepperButton(
                        icono = Icons.Default.Remove,
                        descripcion = "Quitar una unidad",
                        habilitado = habilitada,
                        onClick = onRestar
                    )
                    Text(
                        text = linea.cantidad.toString(),
                        color = VapeOnTextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 28.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    StepperButton(
                        icono = Icons.Default.Add,
                        descripcion = "Agregar una unidad",
                        habilitado = habilitada && linea.cantidad < linea.stockDisponible,
                        onClick = onSumar
                    )
                }
                Text(
                    text = moneda(linea.subtotal),
                    color = VapeOnGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StepperButton(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    descripcion: String,
    habilitado: Boolean,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        enabled = habilitado,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = VapeOnInputBackground,
            contentColor = VapeOnTextPrimary,
            disabledContainerColor = VapeOnCardBackground,
            disabledContentColor = VapeOnTextMuted
        ),
        modifier = Modifier.size(32.dp)
    ) {
        Icon(icono, contentDescription = descripcion, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ResumenVenta(
    viewModel: NuevaVentaViewModel,
    productoViewModel: ProductoViewModel,
    bloqueado: Boolean,
    onConfirmar: () -> Unit
) {
    Surface(
        color = VapeOnSurface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Subtotal", color = VapeOnTextSecondary, fontSize = 12.sp)
                    Text(moneda(viewModel.subtotal), color = VapeOnTextPrimary, fontWeight = FontWeight.SemiBold)
                }
                if (viewModel.conDelivery) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Delivery", color = VapeOnTextSecondary, fontSize = 12.sp)
                        Text(moneda(viewModel.delivery), color = VapeOnTextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total", color = VapeOnTextSecondary, fontSize = 12.sp)
                    Text(
                        moneda(viewModel.total),
                        color = VapeOnGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            Button(
                onClick = onConfirmar,
                enabled = !viewModel.guardando && viewModel.adminId != null &&
                    (viewModel.requiereComprobacion || (viewModel.lineas.isNotEmpty() && !bloqueado)),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VapeOnRed,
                    contentColor = VapeOnTextPrimary,
                    disabledContainerColor = VapeOnRedDark,
                    disabledContentColor = VapeOnTextMuted
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (viewModel.guardando) {
                    CircularProgressIndicator(
                        color = VapeOnTextPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                } else {
                    Icon(
                        imageVector = if (viewModel.conDelivery) Icons.Default.DeliveryDining
                        else Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = when {
                        viewModel.requiereComprobacion -> "COMPROBAR VENTA PENDIENTE"
                        viewModel.conDelivery -> "REGISTRAR PEDIDO CON DELIVERY"
                        else -> "REGISTRAR VENTA EN TIENDA"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
