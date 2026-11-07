package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.components.ImagenSaborView
import com.example.vapeon_movil.entities.SaborItem
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.marcasParaFormulario
import com.example.vapeon_movil.utils.marcasUnicas
import com.example.vapeon_movil.utils.mismaMarca
import com.example.vapeon_movil.viewmodels.MarcasViewModel
import com.example.vapeon_movil.viewmodels.SaboresViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaboresAdminScreen(
    usuario: Usuario?,
    saboresViewModel: SaboresViewModel,
    marcasViewModel: MarcasViewModel,
    volver: () -> Unit,
    irAgregarSabor: () -> Unit = {},
    irEditarSabor: (SaborItem) -> Unit = {},
    irCatalogo: () -> Unit = {},
    irPedidos: () -> Unit = {},
    cerrarSesion: () -> Unit = {},
    onPerfilClick: () -> Unit = {}
) {
    val esAdmin = usuario?.rol?.equals("ADMIN", true) == true
    BackHandler(onBack = volver)

    var saborEditar by remember { mutableStateOf<SaborItem?>(null) }
    var saborEliminar by remember { mutableStateOf<SaborItem?>(null) }
    var mostrarDialogoAgregar by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val marcasDisponibles = marcasParaFormulario(
        marcasViewModel.listaMarcas.map { it.nombre } + saboresViewModel.listaSabores.map { it.marca }
    )
    LaunchedEffect(Unit) { marcasViewModel.cargarMarcas() }

    LaunchedEffect(saboresViewModel.operacionExitosa) {
        if (saboresViewModel.operacionExitosa) {
            scope.launch { snackbarHostState.showSnackbar("Operación exitosa") }
            saboresViewModel.consumirExito()
        }
    }
    LaunchedEffect(saboresViewModel.mensajeError) {
        saboresViewModel.mensajeError?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
            saboresViewModel.limpiarError()
        }
    }

    MenuScreen(
        irCatalogo = irCatalogo,
        irPedidos = irPedidos,
        cerrarSesion = cerrarSesion,
        esAdmin = esAdmin,
        onPerfilClick = onPerfilClick,
        onTituloClick = {}
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
                // Encabezado compacto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gestión de Sabores",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = VapeOnTextPrimary
                        )

                        Spacer(modifier = Modifier.size(10.dp))

                        Text(
                            text = "${saboresViewModel.listaSabores.size} sabores registrados en la base de datos",
                            fontSize = 12.sp,
                            color = VapeOnTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Búsqueda por Texto
                OutlinedTextField(
                    value = saboresViewModel.busquedaQuery,
                    onValueChange = { saboresViewModel.cambiarBusqueda(it) },
                    placeholder = { Text("Buscar sabor por nombre...", color = VapeOnTextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = VapeOnGold
                        )
                    },
                    trailingIcon = {
                        if (saboresViewModel.busquedaQuery.isNotEmpty()) {
                            IconButton(onClick = { saboresViewModel.cambiarBusqueda("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar Búsqueda",
                                    tint = VapeOnTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VapeOnInputBackground,
                        unfocusedContainerColor = VapeOnInputBackground,
                        focusedBorderColor = VapeOnGold,
                        unfocusedBorderColor = VapeOnInputBorder,
                        focusedTextColor = VapeOnTextPrimary,
                        unfocusedTextColor = VapeOnTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Chips filtro por marca
                val filtroActual = saboresViewModel.marcaFiltroSeleccionada
                val marcasFiltro = listOf("Todas") + marcasDisponibles

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(marcasFiltro) { marcaOption ->
                        val estaSeleccionado = marcaOption == filtroActual
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (estaSeleccionado) VapeOnRed else VapeOnRedDark)
                                .clickable { saboresViewModel.cambiarFiltroMarca(marcaOption) }
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = marcaOption,
                                color = VapeOnTextPrimary,
                                fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val queryLimpia = saboresViewModel.busquedaQuery.trim().lowercase()
                val listaFiltrada = saboresViewModel.listaSabores.filter { item ->
                    val coincideMarca = filtroActual == "Todas" || mismaMarca(item.marca, filtroActual)
                    val coincideNombre = queryLimpia.isEmpty() || item.nombre.lowercase().contains(queryLimpia)
                    coincideMarca && coincideNombre
                }

                if (saboresViewModel.cargando) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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
                                text = "Cargando sabores...",
                                color = VapeOnTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else if (listaFiltrada.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron sabores registrados.",
                            color = VapeOnTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(
                            items = listaFiltrada,
                            key = { item -> if (item.id.isNotBlank()) item.id else "${item.marca}:${item.nombre}" }
                        ) { itemSabor ->
                            SaborItemCard(
                                saborItem = itemSabor,
                                onEditar = {
                                    saborEditar = itemSabor
                                    irEditarSabor(itemSabor)
                                },
                                onEliminar = { saborEliminar = itemSabor }
                            )
                        }
                    }
                }
            }

            // FAB Flotante (+) en la esquina inferior derecha
            FloatingActionButton(
                onClick = {
                    mostrarDialogoAgregar = true
                    irAgregarSabor()
                },
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
                    contentDescription = "Agregar Sabor",
                    tint = VapeOnTextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }

    // Diálogo Agregar
    if (mostrarDialogoAgregar) {
        DialogoFormularioSabor(
            titulo = "Agregar Nuevo Sabor",
            nombreInicial = "",
            marcaInicial = marcasDisponibles.firstOrNull().orEmpty(),
            marcasDisponibles = marcasDisponibles,
            onGuardar = { nombre, marca ->
                saboresViewModel.agregarSabor(nombre, marca)
                mostrarDialogoAgregar = false
            },
            onDescartar = { mostrarDialogoAgregar = false }
        )
    }

    // Diálogo Editar
    saborEditar?.let { saborAntiguo ->
        DialogoFormularioSabor(
            titulo = "Editar Sabor",
            nombreInicial = saborAntiguo.nombre,
            marcaInicial = saborAntiguo.marca,
            marcasDisponibles = marcasUnicas(marcasDisponibles + saborAntiguo.marca),
            onGuardar = { nombre, marca ->
                saboresViewModel.editarSabor(saborAntiguo, nombre, marca)
                saborEditar = null
            },
            onDescartar = { saborEditar = null }
        )
    }

    // Diálogo Eliminar
    saborEliminar?.let { saborItem ->
        AlertDialog(
            onDismissRequest = { saborEliminar = null },
            icon = {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = VapeOnRed)
            },
            title = {
                Text(text = "Eliminar Sabor", fontWeight = FontWeight.Bold, color = VapeOnGold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Eliminar definitivamente este sabor del catálogo y base de datos?",
                        color = VapeOnTextSecondary,
                        fontSize = 14.sp
                    )
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(saborItem.nombre, color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                            Text("Marca: ${saborItem.marca}", color = VapeOnGold, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saboresViewModel.eliminarSabor(saborItem)
                        saborEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                ) {
                    Text("Eliminar", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { saborEliminar = null }) {
                    Text("Cancelar", color = VapeOnTextSecondary)
                }
            },
            containerColor = VapeOnSurface
        )
    }
}

// ---- Tarjeta del catálogo maestro de sabores ----
@Composable
private fun SaborItemCard(
    saborItem: SaborItem,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!saborItem.imagenUri.isNullOrBlank()) {
                ImagenSaborView(
                    dataUri = saborItem.imagenUri,
                    contentDescription = "Imagen de ${saborItem.nombre}",
                    contentScale = ContentScale.Crop,
                    maxDimensionPx = 256,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(saborItem.nombre, color = VapeOnTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(saborItem.marca, color = VapeOnGold, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = VapeOnGold, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = VapeOnRed, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

// ---- Diálogo formulario: Agregar / Editar sabor del catálogo maestro ----
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoFormularioSabor(
    titulo: String,
    nombreInicial: String,
    marcaInicial: String,
    marcasDisponibles: List<String>,
    onGuardar: (nombre: String, marca: String) -> Unit,
    onDescartar: () -> Unit
) {
    var nombreInput by remember { mutableStateOf(nombreInicial) }
    var marcaInput by remember { mutableStateOf(marcaInicial) }
    var menuMarcaExpandido by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDescartar,
        title = { Text(text = titulo, fontWeight = FontWeight.Bold, color = VapeOnGold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombreInput,
                    onValueChange = { nombreInput = it },
                    label = { Text("Nombre del Sabor", color = VapeOnTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VapeOnInputBackground,
                        unfocusedContainerColor = VapeOnInputBackground,
                        focusedBorderColor = VapeOnGold,
                        unfocusedBorderColor = VapeOnInputBorder,
                        focusedTextColor = VapeOnTextPrimary,
                        unfocusedTextColor = VapeOnTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = menuMarcaExpandido,
                    onExpandedChange = { menuMarcaExpandido = !menuMarcaExpandido },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = marcaInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Marca", color = VapeOnTextSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuMarcaExpandido) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = VapeOnInputBackground,
                            unfocusedContainerColor = VapeOnInputBackground,
                            focusedBorderColor = VapeOnGold,
                            unfocusedBorderColor = VapeOnInputBorder,
                            focusedTextColor = VapeOnTextPrimary,
                            unfocusedTextColor = VapeOnTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = menuMarcaExpandido,
                        onDismissRequest = { menuMarcaExpandido = false },
                        modifier = Modifier.background(VapeOnSurface)
                    ) {
                        marcasDisponibles.forEach { opMarca ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        opMarca,
                                        color = VapeOnTextPrimary,
                                        fontWeight = if (marcaInput == opMarca) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = { marcaInput = opMarca; menuMarcaExpandido = false }
                            )
                        }
                    }
                }
                if (mensajeError.isNotEmpty()) {
                    Text(text = mensajeError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        nombreInput.isBlank() || marcaInput.isBlank() ->
                            mensajeError = "Completa nombre y marca"
                        else -> onGuardar(nombreInput.trim(), marcaInput.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
            ) {
                Text("Guardar", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDescartar) {
                Text("Cancelar", color = VapeOnTextSecondary)
            }
        },
        containerColor = VapeOnSurface
    )
}
