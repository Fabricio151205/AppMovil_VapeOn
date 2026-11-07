package com.example.vapeon_movil.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.components.MarcaSection
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.entities.SaborItem
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.claveMarca
import com.example.vapeon_movil.utils.marcasUnicas
import com.example.vapeon_movil.utils.mismaMarca
import com.example.vapeon_movil.viewmodels.ProductoViewModel

@Composable
fun CatalogoScreen(
    productoViewModel: ProductoViewModel,
    irDetalleProducto: (Producto) -> Unit,
    esAdmin: Boolean = false,
    sabores: List<SaborItem> = emptyList(),
    irAgregarProducto: () -> Unit = {},
    irEditarProducto: (Producto) -> Unit = {},
    onPerfilClick: () -> Unit = {},
    irAdmin: () -> Unit = {},
    irCarrito: () -> Unit = {},
    irHistorial: () -> Unit = {},
    irPedidos: () -> Unit = {},
    irMarcas: () -> Unit = {},
    irSabores: () -> Unit = {},
    irRoles: () -> Unit = {},
    cerrarSesion: () -> Unit = {},
    irTarjetas: () -> Unit = {},
    irDirecciones: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Estado del ViewModel
    val productos      = productoViewModel.productos
    val cargando       = productoViewModel.cargando
    val errorRed       = productoViewModel.error

    // Estado local de UI (RF12 / RF13)
    var busqueda              by rememberSaveable { mutableStateOf("") }
    var categoriaFiltro       by rememberSaveable { mutableStateOf("") }
    var precioMinimo          by rememberSaveable { mutableStateOf("") }
    var precioMaximo          by rememberSaveable { mutableStateOf("") }
    var soloConStock          by rememberSaveable { mutableStateOf(false) }
    var filtroVisual          by rememberSaveable { mutableStateOf("Todos") }
    var marcaFiltro           by rememberSaveable { mutableStateOf("Todas") }
    var ordenPrecio           by rememberSaveable { mutableStateOf("Sin orden") }
    var mostrarModalFiltros   by rememberSaveable { mutableStateOf(false) }
    var productoAEliminar     by remember { mutableStateOf<Producto?>(null) }

    val errorFiltros = productoViewModel.errorFiltros(precioMinimo, precioMaximo)
    val tieneFiltrosFlotantesActivos = remember(
        categoriaFiltro, precioMinimo, precioMaximo, soloConStock, marcaFiltro, ordenPrecio, filtroVisual
    ) {
        categoriaFiltro.isNotBlank() || precioMinimo.isNotBlank() || precioMaximo.isNotBlank() ||
            soloConStock || marcaFiltro != "Todas" || ordenPrecio != "Sin orden" || filtroVisual != "Todos"
    }

    // Carga inicial de productos
    LaunchedEffect(Unit) {
        productoViewModel.cargarProductos()
    }

    // Mostrar error de red como Snackbar
    LaunchedEffect(errorRed) {
        errorRed?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Contenedor principal reutilizando el menú lateral y la barra superior oficial de VapeON
    MenuScreen(
        irCatalogo = { /* Ya estamos en el catálogo */ },
        irCarrito = irCarrito,
        irHistorial = irHistorial,
        irTarjetas = irTarjetas,
        irDirecciones = irDirecciones,
        irPedidos = irPedidos,
        irMarcas = irMarcas,
        irSabores = irSabores,
        irRoles = irRoles,
        cerrarSesion = cerrarSesion,
        esAdmin = esAdmin,
        onPerfilClick = onPerfilClick,
        // Solo admin puede navegar al panel administrativo pulsando el título VAPEON
        onTituloClick = { if (esAdmin) irAdmin() }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)
            ) {
                SnackbarHost(snackbarHostState)

                // ==========================================
                // 1. BARRA DE BÚSQUEDA REDONDEADA (RF12)
                // ==========================================
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = {
                        Text(
                            text = "Buscar Productos...",
                            color = VapeOnTextSecondary,
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = VapeOnTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VapeOnInputBackground,
                        unfocusedContainerColor = VapeOnInputBackground,
                        disabledContainerColor = VapeOnInputBackground,
                        focusedBorderColor = VapeOnGold,
                        unfocusedBorderColor = VapeOnInputBorder,
                        focusedTextColor = VapeOnTextPrimary,
                        unfocusedTextColor = VapeOnTextPrimary,
                        cursorColor = VapeOnGold
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("busqueda_productos")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 2. FILTRO FLOTANTE + BOTONES "TODOS", "NUEVOS", "PROMOCIONES"
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ícono de Filtros ubicado exactamente al lado izquierdo del botón "Todos"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (tieneFiltrosFlotantesActivos) VapeOnGold else VapeOnRedDark)
                            .clickable { mostrarModalFiltros = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                            contentDescription = "Filtros de productos",
                                tint = if (tieneFiltrosFlotantesActivos) VapeOnBackground else VapeOnTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            if (tieneFiltrosFlotantesActivos) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Filtros",
                                    color = VapeOnBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Filtros visuales independientes: no representan Producto.categoria.
                    listOf("Todos", "Nuevos").forEach { opcion ->
                        val seleccionada = opcion == filtroVisual
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (seleccionada) VapeOnRed else VapeOnRedDark)
                                .clickable { filtroVisual = opcion }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opcion,
                                color = VapeOnTextPrimary,
                                fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // 3. ESTADO DE CARGA O LISTA DE PRODUCTOS (RF12 / RF13 / RNE01)
                // ==========================================
                if (cargando) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = VapeOnGold,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Cargando catálogo...",
                                color = VapeOnTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else if (errorRed != null) {
                    Text(errorRed, color = VapeOnTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { productoViewModel.cargarProductos() },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VapeOnGold,
                            contentColor = VapeOnBackground
                        )
                    ) {
                        Text("Reintentar", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // RF12/RF13 se aplican una sola vez en el ViewModel. Marca y orden son capas visuales posteriores.
                    val listaFiltrada = remember(
                        productos, busqueda, categoriaFiltro, precioMinimo, precioMaximo, soloConStock,
                        marcaFiltro, filtroVisual, ordenPrecio
                    ) {
                        var resultado = productoViewModel.filtrarProductos(
                            busqueda = busqueda,
                            categoria = categoriaFiltro,
                            precioMinimo = precioMinimo,
                            precioMaximo = precioMaximo,
                            soloConStock = soloConStock
                        )
                        if (marcaFiltro != "Todas") {
                            resultado = resultado.filter { mismaMarca(it.marca, marcaFiltro) }
                        }
                        if (filtroVisual == "Nuevos") resultado = resultado.filter { it.esNuevo() }
                        when (ordenPrecio) {
                            "Menor a Mayor" -> resultado = resultado.sortedBy { it.precio }
                            "Mayor a Menor" -> resultado = resultado.sortedByDescending { it.precio }
                        }

                        resultado
                    }

                    if (errorFiltros != null) {
                        Text(
                            text = errorFiltros,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("error_filtros")
                        )
                    } else if (listaFiltrada.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se encontraron productos disponibles.",
                                color = VapeOnTextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        // Una sección por identidad de marca; conserva la primera etiqueta legible del inventario.
                        val agrupados = listaFiltrada.groupBy { claveMarca(it.marca) }
                        agrupados.values.forEach { prods ->
                            val nombreMarca = marcasUnicas(prods.map { it.marca }).firstOrNull().orEmpty()
                            MarcaSection(
                                nombreMarca = nombreMarca,
                                productos = prods,
                                irDetalleProducto = irDetalleProducto,
                                esAdmin = esAdmin,
                                sabores = sabores,
                                onVerTodos = {
                                    busqueda = ""
                                    marcaFiltro = nombreMarca
                                    filtroVisual = "Todos"
                                },
                                eliminarProducto = { productoAEliminar = it },
                                editarProducto = { irEditarProducto(it) }
                            )
                        }
                    }
                }

                // Espacio inferior para el botón flotante
                Spacer(modifier = Modifier.height(70.dp))
            }

            // ==========================================
            // 6. BOTÓN FLOTANTE (+) EXCLUSIVO PARA ADMIN
            // ==========================================
            if (esAdmin) {
                FloatingActionButton(
                    onClick = irAgregarProducto,
                    shape = RoundedCornerShape(18.dp),
                    containerColor = VapeOnRed,
                    contentColor = VapeOnTextPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 24.dp)
                        .size(58.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar Producto",
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    // ==========================================
    // VISTA FLOTANTE DE FILTROS RF13
    // ==========================================
    if (mostrarModalFiltros) {
        val marcasDisponibles = remember(productos) {
            listOf("Todas") + marcasUnicas(productos.map { it.marca })
        }

        AlertDialog(
            onDismissRequest = { mostrarModalFiltros = false },
            containerColor = VapeOnSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "FILTRAR PRODUCTOS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = VapeOnGold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECCIÓN: MARCAS
                    Text(
                        text = "Filtrar por Marca",
                        fontWeight = FontWeight.Bold,
                        color = VapeOnTextPrimary,
                        fontSize = 14.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        marcasDisponibles.forEach { m ->
                            val seleccionada = m == "Todas" && marcaFiltro == "Todas" ||
                                m != "Todas" && marcaFiltro != "Todas" && mismaMarca(m, marcaFiltro)
                            Button(
                                onClick = { marcaFiltro = m },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (seleccionada) VapeOnRed else VapeOnInputBackground,
                                    contentColor = VapeOnTextPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = m,
                                    fontSize = 12.sp,
                                    fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = VapeOnInputBorder)

                    Text("Categoría", fontWeight = FontWeight.Bold, color = VapeOnTextPrimary, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (listOf("") + productoViewModel.categoriasDisponibles).forEach { categoria ->
                            val etiqueta = categoria.ifBlank { "Todas" }
                            Button(
                                onClick = { categoriaFiltro = categoria },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (categoriaFiltro.equals(categoria, true)) VapeOnRed else VapeOnInputBackground,
                                    contentColor = VapeOnTextPrimary
                                ),
                                modifier = Modifier.testTag("categoria_$etiqueta")
                            ) { Text(etiqueta, fontSize = 12.sp) }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = precioMinimo,
                            onValueChange = { precioMinimo = it },
                            label = { Text("Precio mínimo") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("precio_minimo")
                        )
                        OutlinedTextField(
                            value = precioMaximo,
                            onValueChange = { precioMaximo = it },
                            label = { Text("Precio máximo") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("precio_maximo")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { soloConStock = !soloConStock },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Solo con stock", color = VapeOnTextPrimary)
                        Switch(
                            checked = soloConStock,
                            onCheckedChange = { soloConStock = it },
                            modifier = Modifier.testTag("solo_con_stock")
                        )
                    }

                    errorFiltros?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("error_filtros_dialogo"))
                    }

                    // SECCIÓN: PRECIO
                    Text(
                        text = "Ordenar por Precio",
                        fontWeight = FontWeight.Bold,
                        color = VapeOnTextPrimary,
                        fontSize = 14.sp
                    )

                    val opcionesOrden = listOf("Sin orden", "Menor a Mayor", "Mayor a Menor")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        opcionesOrden.forEach { op ->
                            val seleccionada = op == ordenPrecio
                            Button(
                                onClick = { ordenPrecio = op },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (seleccionada) VapeOnRed else VapeOnInputBackground,
                                    contentColor = VapeOnTextPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = op,
                                    fontSize = 12.sp,
                                    fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { mostrarModalFiltros = false },
                    enabled = errorFiltros == null,
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                ) {
                    Text("Aplicar Filtros", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        busqueda = ""
                        categoriaFiltro = ""
                        precioMinimo = ""
                        precioMaximo = ""
                        soloConStock = false
                        marcaFiltro = "Todas"
                        ordenPrecio = "Sin orden"
                        filtroVisual = "Todos"
                        mostrarModalFiltros = false
                    }
                ) {
                    Text("Restablecer", color = VapeOnTextSecondary)
                }
            }
        )
    }

    // ==========================================
    // DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN
    // ==========================================
    productoAEliminar?.let { prod ->
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = {
                Text(
                    text = "Eliminar Producto",
                    fontWeight = FontWeight.Bold,
                    color = VapeOnGold
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar '${prod.nombre}'? Esta acción no se puede deshacer.",
                    color = VapeOnTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        productoViewModel.eliminarProducto(prod.id) { productoAEliminar = null }
                    },
                    enabled = prod.id !in productoViewModel.eliminandoIds,
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                ) {
                    Text(if (prod.id in productoViewModel.eliminandoIds) "Eliminando…" else "Eliminar", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productoAEliminar = null }) {
                    Text("Cancelar", color = VapeOnTextSecondary)
                }
            },
            containerColor = VapeOnSurface
        )
    }
}
