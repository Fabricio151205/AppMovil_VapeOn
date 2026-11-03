package com.example.vapeon_movil.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.components.MarcaSection
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.ProductoViewModel
<<<<<<< HEAD
=======
import com.example.vapeon_movil.utils.claveMarca
import com.example.vapeon_movil.utils.marcasUnicas
import com.example.vapeon_movil.utils.mismaMarca
import kotlinx.coroutines.launch
>>>>>>> acc9221f3bbcdfd859bec4ce91f95c9606023928

@Composable
fun CatalogoScreen(
    productoViewModel: ProductoViewModel,
    irDetalleProducto: (Producto) -> Unit,
    esAdmin: Boolean = false,
    irAgregarProducto: () -> Unit = {},
    irEditarProducto: (Producto) -> Unit = {},
    onPerfilClick: () -> Unit = {},
    irAdmin: () -> Unit = {},
    irCarrito: () -> Unit = {},
    irHistorial: () -> Unit = {},
    irPedidos: () -> Unit = {},
    irSabores: () -> Unit = {},
    cerrarSesion: () -> Unit = {},
    irTarjetas: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Estado del ViewModel
    val productos      = productoViewModel.productos
    val cargando       = productoViewModel.cargando
    val errorRed       = productoViewModel.error

    // Estado local de UI
<<<<<<< HEAD
    var busqueda              by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todos") } // Todos, Nuevos, Promociones
    var marcaFiltro           by remember { mutableStateOf("Todas") }  // Filtro flotante por Marca
    var ordenPrecio           by remember { mutableStateOf("Sin orden") } // Sin orden, Menor a Mayor, Mayor a Menor
    var mostrarModalFiltros   by remember { mutableStateOf(false) }
=======
    var busqueda              by rememberSaveable { mutableStateOf("") }
    var marcaVerTodos         by rememberSaveable { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todos") }
>>>>>>> acc9221f3bbcdfd859bec4ce91f95c9606023928
    var productoAEliminar     by remember { mutableStateOf<Producto?>(null) }
    var categoriaFiltro by rememberSaveable { mutableStateOf("") }
    var precioMinimo by rememberSaveable { mutableStateOf("") }
    var precioMaximo by rememberSaveable { mutableStateOf("") }
    var soloConStock by rememberSaveable { mutableStateOf(false) }
    var menuCategoriasAbierto by remember { mutableStateOf(false) }
    var filtrosAbiertos by rememberSaveable { mutableStateOf(false) }
    val errorFiltro = productoViewModel.errorFiltros(precioMinimo, precioMaximo)

    // Regla RNE01: El cliente solo puede ver productos con stock > 0, mientras que el admin ve todos.
    val productosPorRol = remember(productos, esAdmin) {
        if (esAdmin) productos else productos.filter { it.stockTotal > 0 }
    }

    val tieneFiltrosFlotantesActivos = remember(marcaFiltro, ordenPrecio) {
        marcaFiltro != "Todas" || ordenPrecio != "Sin orden"
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
        irPedidos = irPedidos,
        irSabores = irSabores,
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
                    onValueChange = { busqueda = it; marcaVerTodos = "" },
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
<<<<<<< HEAD
                // 2. FILTRO FLOTANTE + BOTONES "TODOS", "NUEVOS", "PROMOCIONES"
=======
                // RF13: categorías reales del inventario, independientes de las marcas.
                val filtrosActivos = categoriaFiltro.isNotBlank() || precioMinimo.isNotBlank() ||
                    precioMaximo.isNotBlank() || soloConStock
                TextButton(onClick = { filtrosAbiertos = !filtrosAbiertos }) {
                    Text(if (filtrosAbiertos) "Ocultar filtros" else if (filtrosActivos) "Filtros · Activos" else "Filtros",
                        color = VapeOnGold)
                }
                if (!filtrosAbiertos && errorFiltro != null) {
                    Text(errorFiltro, color = MaterialTheme.colorScheme.error)
                }
                if (filtrosAbiertos) {
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { menuCategoriasAbierto = true },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (categoriaFiltro.isBlank()) "Todas las categorías" else "Categoría: $categoriaFiltro",
                            color = VapeOnGold)
                    }
                    DropdownMenu(expanded = menuCategoriasAbierto,
                        onDismissRequest = { menuCategoriasAbierto = false }) {
                        DropdownMenuItem(text = { Text("Todas las categorías") }, onClick = {
                            categoriaFiltro = ""
                            menuCategoriasAbierto = false
                        })
                        productoViewModel.categoriasDisponibles.forEach { categoria ->
                            DropdownMenuItem(text = { Text(categoria) }, onClick = {
                                categoriaFiltro = categoria
                                menuCategoriasAbierto = false
                            })
                        }
                    }
                }
                val coloresFiltros = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VapeOnTextPrimary, unfocusedTextColor = VapeOnTextPrimary,
                    focusedContainerColor = VapeOnInputBackground, unfocusedContainerColor = VapeOnInputBackground,
                    focusedBorderColor = VapeOnGold, unfocusedBorderColor = VapeOnInputBorder,
                    focusedLabelColor = VapeOnGold, unfocusedLabelColor = VapeOnTextSecondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = precioMinimo, onValueChange = { precioMinimo = it },
                        label = { Text("Precio mínimo") }, prefix = { Text("S/") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = coloresFiltros, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = precioMaximo, onValueChange = { precioMaximo = it },
                        label = { Text("Precio máximo") }, prefix = { Text("S/") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = coloresFiltros, modifier = Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Solo con stock", color = VapeOnTextPrimary, modifier = Modifier.weight(1f))
                    Switch(checked = soloConStock, onCheckedChange = { soloConStock = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = VapeOnRed,
                            checkedThumbColor = VapeOnTextPrimary),
                        modifier = Modifier.semantics { contentDescription = "Solo con stock" })
                }
                errorFiltro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = {
                    busqueda = ""
                    marcaVerTodos = ""
                    categoriaFiltro = ""
                    precioMinimo = ""
                    precioMaximo = ""
                    soloConStock = false
                }) { Text("Limpiar filtros", color = VapeOnGold) }
                Spacer(Modifier.height(12.dp))
                }

                // 2. CHIPS DE CATEGORÍAS (Todos, Nuevos, Promociones)
>>>>>>> acc9221f3bbcdfd859bec4ce91f95c9606023928
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
                                contentDescription = "Filtros de Marca y Precio",
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

                    // Botones de categoría principales
                    val categorias = listOf("Todos", "Nuevos", "Promociones")
                    categorias.forEach { cat ->
                        val seleccionada = cat.equals(categoriaSeleccionada, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (seleccionada) VapeOnRed else VapeOnRedDark)
                                .clickable { categoriaSeleccionada = cat }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
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
                            .height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = VapeOnGold,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Cargando catálogo...",
                                color = VapeOnTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (errorRed != null) {
                    Text(errorRed, color = VapeOnTextSecondary)
                    Button(
                        onClick = { productoViewModel.cargarProductos() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VapeOnGold,
                            contentColor = VapeOnTextPrimary)
                    ) {
                        Text("Reintentar")
                    }
                } else {
<<<<<<< HEAD
                    // Filtrado reactivo completo
                    val listaFiltrada = remember(productosPorRol, busqueda, categoriaSeleccionada, marcaFiltro, ordenPrecio) {
                        var resultado = productosPorRol.filter { prod ->
                            // 1. Filtro por texto de búsqueda
                            val coincideTexto = busqueda.isBlank() ||
                                    prod.nombre.contains(busqueda, ignoreCase = true) ||
                                    prod.marca.contains(busqueda, ignoreCase = true) ||
                                    prod.descripcion.contains(busqueda, ignoreCase = true)

                            // 2. Filtro por categoría principal (Todos, Nuevos, Promociones)
                            val coincideCategoria = when {
                                categoriaSeleccionada.equals("Nuevos", ignoreCase = true) -> prod.esNuevo()
                                categoriaSeleccionada.equals("Promociones", ignoreCase = true) -> prod.precio <= 60.0
                                else -> true // "Todos"
                            }

                            // 3. Filtro por Marca seleccionada en la vista flotante
                            val coincideMarca = when {
                                marcaFiltro.equals("Todas", ignoreCase = true) -> true
                                else -> prod.marca.equals(marcaFiltro, ignoreCase = true)
                            }

                            coincideTexto && coincideCategoria && coincideMarca
                        }

                        // 4. Ordenamiento por Precio seleccionado en la vista flotante
                        when (ordenPrecio) {
                            "Menor a Mayor" -> resultado = resultado.sortedBy { it.precio }
                            "Mayor a Menor" -> resultado = resultado.sortedByDescending { it.precio }
                        }

                        resultado
                    }
=======
                    // Filtrado reactivo de productos
                    val listaConFiltros = productoViewModel.filtrarProductos(
                        busqueda, categoriaFiltro, precioMinimo, precioMaximo, soloConStock)
                    val listaFiltrada = if (marcaVerTodos.isBlank()) listaConFiltros else
                        listaConFiltros.filter { mismaMarca(it.marca, marcaVerTodos) }
>>>>>>> acc9221f3bbcdfd859bec4ce91f95c9606023928

                    if (listaFiltrada.isEmpty()) {
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
<<<<<<< HEAD
                        // Agrupación por marca manteniendo el orden filtrado
                        val marcasOrdenadas = listOf("LifePod", "Oxbar", "Nexa")
                        val agrupados = listaFiltrada.groupBy { prod ->
                            marcasOrdenadas.find { it.equals(prod.marca.trim(), ignoreCase = true) } ?: prod.marca.trim()
=======
                        // ── SECCIÓN: LIFEPOD ──────────────────────────────
                        val productosLifePod = listaFiltrada.filter {
                            mismaMarca(it.marca, "LifePod")
>>>>>>> acc9221f3bbcdfd859bec4ce91f95c9606023928
                        }

                        agrupados.forEach { (nombreMarca, prods) ->
                            MarcaSection(
                                nombreMarca = nombreMarca,
                                productos = prods,
                                irDetalleProducto = irDetalleProducto,
                                esAdmin = esAdmin,
<<<<<<< HEAD
                                onVerTodos = { busqueda = nombreMarca },
=======
                                onVerTodos = { busqueda = ""; marcaVerTodos = "LifePod" },
>>>>>>> acc9221f3bbcdfd859bec4ce91f95c9606023928
                                eliminarProducto = { productoAEliminar = it },
                                editarProducto = { irEditarProducto(it) }
                            )
                        }
<<<<<<< HEAD
=======

                        // ── SECCIÓN: OXBAR ────────────────────────────────
                        val productosOxbar = listaFiltrada.filter {
                            mismaMarca(it.marca, "Oxbar")
                        }
                        if (productosOxbar.isNotEmpty()) {
                            MarcaSection(
                                nombreMarca = "Oxbar",
                                productos = productosOxbar,
                                irDetalleProducto = irDetalleProducto,
                                esAdmin = esAdmin,
                                onVerTodos = { busqueda = ""; marcaVerTodos = "Oxbar" },
                                eliminarProducto = { productoAEliminar = it },
                                editarProducto = { irEditarProducto(it) }
                            )
                        }

                        // ── SECCIÓN: NEXA ─────────────────────────────────
                        val productosNexa = listaFiltrada.filter {
                            mismaMarca(it.marca, "Nexa")
                        }
                        if (productosNexa.isNotEmpty()) {
                            MarcaSection(
                                nombreMarca = "Nexa",
                                productos = productosNexa,
                                irDetalleProducto = irDetalleProducto,
                                esAdmin = esAdmin,
                                onVerTodos = { busqueda = ""; marcaVerTodos = "Nexa" },
                                eliminarProducto = { productoAEliminar = it },
                                editarProducto = { irEditarProducto(it) }
                            )
                        }

                        // ── SECCIONES DINÁMICAS (otras marcas) ────────────
                        val especiales = setOf(claveMarca("LifePod"), claveMarca("Oxbar"), claveMarca("Nexa"))
                        val otrasMarcas = marcasUnicas(listaFiltrada.map { it.marca })
                            .filter { claveMarca(it) !in especiales }

                        otrasMarcas.forEach { marca ->
                            val productosMarca = listaFiltrada.filter {
                                mismaMarca(it.marca, marca)
                            }
                            if (productosMarca.isNotEmpty()) {
                                MarcaSection(
                                    nombreMarca = marca,
                                    productos = productosMarca,
                                    irDetalleProducto = irDetalleProducto,
                                    esAdmin = esAdmin,
                                    onVerTodos = { busqueda = ""; marcaVerTodos = marca },
                                    eliminarProducto = { productoAEliminar = it },
                                    editarProducto = { irEditarProducto(it) }
                                )
                            }
                        }
>>>>>>> acc9221f3bbcdfd859bec4ce91f95c9606023928
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
    // VISTA FLOTANTE DE FILTROS POR MARCA Y PRECIO (DIALOG)
    // ==========================================
    if (mostrarModalFiltros) {
        val marcasDisponibles = remember(productosPorRol) {
            val conocidas = listOf("LifePod", "Oxbar", "Nexa")
            val otras = productosPorRol
                .map { it.marca.trim() }
                .filter { m -> m.isNotBlank() && conocidas.none { c -> c.equals(m, ignoreCase = true) } }
                .distinct()
            listOf("Todas") + conocidas + otras
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
                            val seleccionada = m.equals(marcaFiltro, ignoreCase = true)
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
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                ) {
                    Text("Aplicar Filtros", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        marcaFiltro = "Todas"
                        ordenPrecio = "Sin orden"
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
