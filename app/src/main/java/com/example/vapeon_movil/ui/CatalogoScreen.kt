package com.example.vapeon_movil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.components.MarcaSection
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.services.ProductoService
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun CatalogoScreen(
    irDetalleProducto: (Producto) -> Unit,
    esAdmin: Boolean = false,
    irAgregarProducto: () -> Unit = {},
    irEditarProducto: (Producto) -> Unit = {},
    onPerfilClick: () -> Unit = {},
    irAdmin: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val productoService = remember {
        RetrofitClient.retrofit.create(ProductoService::class.java)
    }

    var busqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todos") }

    var productosFirebase by remember { mutableStateOf(emptyList<Producto>()) }
    var cargando by remember { mutableStateOf(true) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }

    // Carga de productos desde Firebase Firestore REST API
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val respuesta = productoService.listarProductos()
                val lista = respuesta.documents?.map { doc ->
                    Producto(
                        id = doc.name.substringAfterLast("/"),
                        nombre = doc.fields.nombre?.stringValue ?: "",
                        marca = doc.fields.marca?.stringValue ?: "",
                        precio = doc.fields.precio?.stringValue ?: "",
                        stock = doc.fields.stock?.stringValue ?: "",
                        descripcion = doc.fields.descripcion?.stringValue ?: ""
                    )
                } ?: emptyList()
                productosFirebase = lista
            } catch (e: Exception) {
                println("Error al cargar productos en Catalogo: ${e.localizedMessage}")
            } finally {
                cargando = false
            }
        }
    }

    // Función segura de eliminación para administradores
    fun eliminarProductoFirebase(producto: Producto) {
        scope.launch {
            try {
                productoService.eliminarProducto(producto.id)
                productosFirebase = productosFirebase.filter { it.id != producto.id }
                snackbarHostState.showSnackbar("Producto '${producto.nombre}' eliminado correctamente")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error al eliminar: ${e.localizedMessage}")
            }
        }
    }

    // Contenedor principal reutilizando el menú lateral y la barra superior oficial de VapeON
    MenuScreen(
        irCatalogo = { /* Ya estamos en el catálogo */ },
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
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                SnackbarHost(snackbarHostState)

                // ==========================================
                // 1. BARRA DE BÚSQUEDA REDONDEADA
                // ==========================================
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = {
                        Text(
                            text = "Buscar Productos",
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

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // 2. CHIPS DE CATEGORÍAS (Todos, Nuevos, Promociones)
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val categorias = listOf("Todos", "Nuevos", "Promociones")
                    categorias.forEach { cat ->
                        val seleccionada = cat == categoriaSeleccionada
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (seleccionada) VapeOnRed else VapeOnRedDark)
                                .clickable { categoriaSeleccionada = cat }
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                color = VapeOnTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ==========================================
                // 3. ESTADO DE CARGA O LISTA DE PRODUCTOS
                // ==========================================
                if (cargando) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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
                } else {
                    // Filtrado reactivo de productos
                    val listaFiltrada = productosFirebase.filter { prod ->
                        val coincideTexto = busqueda.isBlank() ||
                                prod.nombre.contains(busqueda, ignoreCase = true) ||
                                prod.marca.contains(busqueda, ignoreCase = true) ||
                                prod.descripcion.contains(busqueda, ignoreCase = true)

                        val coincideCategoria = when (categoriaSeleccionada) {
                            "Nuevos" -> prod.id.length > 5
                            "Promociones" -> prod.precio.toDoubleOrNull()?.let { it < 75 } ?: false
                            else -> true
                        }

                        coincideTexto && coincideCategoria
                    }

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
                        // ==========================================
                        // SECCIÓN: LIFEPOD
                        // ==========================================
                        val productosLifePod = listaFiltrada.filter {
                            it.marca.equals("LifePod", ignoreCase = true) || it.marca.equals("LifePood", ignoreCase = true)
                        }

                        if (productosLifePod.isNotEmpty()) {
                            MarcaSection(
                                nombreMarca = "LifePod",
                                productos = productosLifePod,
                                irDetalleProducto = irDetalleProducto,
                                esAdmin = esAdmin,
                                onVerTodos = { busqueda = "LifePod" },
                                eliminarProducto = { productoAEliminar = it },
                                editarProducto = { irEditarProducto(it) }
                            )
                        }

                        // ==========================================
                        // SECCIÓN: OXBAR
                        // ==========================================
                        val productosOxbar = listaFiltrada.filter {
                            it.marca.equals("Oxbar", ignoreCase = true)
                        }

                        if (productosOxbar.isNotEmpty()) {
                            MarcaSection(
                                nombreMarca = "Oxbar",
                                productos = productosOxbar,
                                irDetalleProducto = irDetalleProducto,
                                esAdmin = esAdmin,
                                onVerTodos = { busqueda = "Oxbar" },
                                eliminarProducto = { productoAEliminar = it },
                                editarProducto = { irEditarProducto(it) }
                            )
                        }

                        // ==========================================
                        // SECCIÓN: NEXA
                        // ==========================================
                        val productosNexa = listaFiltrada.filter {
                            it.marca.equals("Nexa", ignoreCase = true)
                        }

                        if (productosNexa.isNotEmpty()) {
                            MarcaSection(
                                nombreMarca = "Nexa",
                                productos = productosNexa,
                                irDetalleProducto = irDetalleProducto,
                                esAdmin = esAdmin,
                                onVerTodos = { busqueda = "Nexa" },
                                eliminarProducto = { productoAEliminar = it },
                                editarProducto = { irEditarProducto(it) }
                            )
                        }

                        // Secciones dinámicas para otras marcas agregadas por el administrador
                        val otrasMarcas = listaFiltrada
                            .map { it.marca.trim() }
                            .distinct()
                            .filter {
                                !it.equals("LifePod", ignoreCase = true) &&
                                        !it.equals("LifePood", ignoreCase = true) &&
                                        !it.equals("Oxbar", ignoreCase = true) &&
                                        !it.equals("Nexa", ignoreCase = true)
                            }

                        otrasMarcas.forEach { marca ->
                            val productosMarca = listaFiltrada.filter { it.marca.equals(marca, ignoreCase = true) }
                            if (productosMarca.isNotEmpty()) {
                                MarcaSection(
                                    nombreMarca = marca,
                                    productos = productosMarca,
                                    irDetalleProducto = irDetalleProducto,
                                    esAdmin = esAdmin,
                                    onVerTodos = { busqueda = marca },
                                    eliminarProducto = { productoAEliminar = it },
                                    editarProducto = { irEditarProducto(it) }
                                )
                            }
                        }
                    }
                }

                // Espacio inferior para evitar que el contenido quede tapado por el botón flotante
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
    // 7. DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN
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
                        eliminarProductoFirebase(prod)
                        productoAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                ) {
                    Text("Eliminar", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
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