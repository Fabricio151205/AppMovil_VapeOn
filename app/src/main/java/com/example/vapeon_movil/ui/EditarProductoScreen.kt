package com.example.vapeon_movil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.LISTA_MARCAS_DISPONIBLES
import com.example.vapeon_movil.utils.obtenerSaboresPorMarca
import com.example.vapeon_movil.viewmodels.ProductoViewModel
import com.example.vapeon_movil.viewmodels.SaboresViewModel

@Composable
fun EditarProductoScreen(
    producto: Producto,
    productoViewModel: ProductoViewModel,
    saboresViewModel: SaboresViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    volver: () -> Unit
) {
    LaunchedEffect(Unit) { saboresViewModel.cargarSabores() }
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(producto.id, productoViewModel) {
        productoViewModel.iniciarEdicion(producto.id)
        onDispose {
            if ((context as? android.app.Activity)?.isChangingConfigurations != true) productoViewModel.cerrarEdicion()
        }
    }
    val documento = productoViewModel.documentoEdicion
    if (documento == null) {
        Surface(color = VapeOnBackground, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.statusBarsPadding().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("EDITAR PRODUCTO", style = MaterialTheme.typography.titleLarge)
                if (productoViewModel.cargandoEdicion) CircularProgressIndicator()
                productoViewModel.errorEdicion?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = { productoViewModel.cargarProductoEdicion(producto.id) },
                    enabled = !productoViewModel.cargandoEdicion) { Text("Recargar datos") }
                TextButton(onClick = volver) { Text("Volver") }
            }
        }
        return
    }
    key(documento.name, documento.updateTime) {
        FormularioEditarProducto(documento, productoViewModel, saboresViewModel, volver)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioEditarProducto(
    documento: ProductoDocumento, productoViewModel: ProductoViewModel, saboresViewModel: SaboresViewModel, volver: () -> Unit
) {
    val producto = Producto(
        id = documento.name.substringAfterLast('/'), nombre = documento.fields.nombre?.asString().orEmpty(),
        marca = documento.fields.marca?.asString().orEmpty(), precio = documento.fields.precio?.asDouble() ?: 0.0,
        stock = documento.fields.stock?.asInt() ?: 0, descripcion = documento.fields.descripcion?.asString().orEmpty(),
        sabor = documento.fields.sabor?.asString().orEmpty(),
        stockPorSabor = documento.fields.stockPorSabor.aStockMap() // Bug A: cargar mapa de stock existente
    )

    var nombre by rememberSaveable { mutableStateOf(producto.nombre) }
    var marca by rememberSaveable { mutableStateOf(producto.marca) }
    var precio by rememberSaveable { mutableStateOf(producto.precio.toString()) }
    var stock by rememberSaveable { mutableStateOf(producto.stock.toString()) }
    var descripcion by rememberSaveable { mutableStateOf(producto.descripcion) }
    var sabor by rememberSaveable { mutableStateOf(producto.sabor) }
    var stockPorSaborState by rememberSaveable { mutableStateOf(producto.stockPorSabor.mapValues { it.value.toString() }) }
    var tieneSabor by rememberSaveable { mutableStateOf(producto.sabor.isNotBlank() || producto.stockPorSabor.isNotEmpty()) }

    val cargando = productoViewModel.guardandoEdicion
    var mensajeError by rememberSaveable { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VapeOnBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==========================================
            // BARRA SUPERIOR CUSTOMIZADA
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Flecha para volver
                IconButton(onClick = { volver() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Título VAPEON
                Text(
                    text = "VAPEON",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = VapeOnGold,
                    letterSpacing = 1.sp
                )

                // Icono de Perfil de Usuario
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VapeOnRedDark),
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

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // TÍTULO DE LA PANTALLA
            // ==========================================
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "EDITAR PRODUCTO",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = VapeOnTextPrimary,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // CAMPOS DEL FORMULARIO
            // ==========================================
            // Campo: Nombres
            Text(
                text = "Nombre del Producto",
                color = VapeOnTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                placeholder = {
                    Text(text = "Nombres", color = VapeOnTextSecondary, fontSize = 15.sp)
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

            // Campo: Marca (ExposedDropdownMenu)
            var menuMarcaExpandido by remember { mutableStateOf(false) }

            Text(
                text = "Marca",
                color = VapeOnTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            ExposedDropdownMenuBox(
                expanded = menuMarcaExpandido,
                onExpandedChange = { menuMarcaExpandido = !menuMarcaExpandido },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it },
                    readOnly = true,
                    placeholder = {
                        Text(text = "Seleccionar Marca", color = VapeOnTextSecondary, fontSize = 15.sp)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuMarcaExpandido)
                    },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = menuMarcaExpandido,
                    onDismissRequest = { menuMarcaExpandido = false },
                    modifier = Modifier.background(VapeOnSurface)
                ) {
                    LISTA_MARCAS_DISPONIBLES.forEach { opcionMarca ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = opcionMarca,
                                    color = VapeOnTextPrimary,
                                    fontWeight = if (marca == opcionMarca) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                marca = opcionMarca
                                menuMarcaExpandido = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            //Campo: Descripción
            Text(
                text = "Descripción del producto",
                color = VapeOnTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                placeholder = {
                    Text(text = "Descripción", color = VapeOnTextSecondary, fontSize = 15.sp)
                },
                singleLine = false,
                maxLines = 3,
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

            //Campo: Precio
            Text(
                text = "Precio",
                color = VapeOnTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = precio,
                onValueChange = { precio = it },
                placeholder = {
                    Text(text = "Precio", color = VapeOnTextSecondary, fontSize = 15.sp)
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

            //Campo: Stock
            Text(
                text = "Stock disponible",
                color = VapeOnTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = stock,
                onValueChange = { stock = it },
                placeholder = {
                    Text(text = "Stock", color = VapeOnTextSecondary, fontSize = 15.sp)
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

            // Selector de Sabores con opcionalidad (tieneSabor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¿El producto incluye selección de sabores?",
                        color = VapeOnTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (tieneSabor) "Sí, seleccionar sabores e inventario individual" else "No, producto sin sabor o sabor único",
                        color = VapeOnTextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Casilla redondeada con Ícono de Check cuando está activo
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            tieneSabor = !tieneSabor
                            if (!tieneSabor) {
                                sabor = ""
                                stockPorSaborState = emptyMap()
                            }
                        }
                        .background(if (tieneSabor) VapeOnRed else VapeOnSurface)
                        .border(
                            width = 1.dp,
                            color = if (tieneSabor) VapeOnRed else VapeOnInputBorder,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (tieneSabor) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Con Sabor",
                            tint = VapeOnTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (tieneSabor) {
                // Selector de Sabores dinámico por Marca con cápsulas y multi-selección
                val saboresDisponibles = remember(marca, saboresViewModel.listaSabores) { obtenerSaboresPorMarca(marca, saboresViewModel.listaSabores) }
                val saboresSeleccionados = sabor.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()

                Text(
                    text = "Seleccionar Sabores (${saboresDisponibles.size} disponibles)",
                    color = VapeOnTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    saboresDisponibles.chunked(2).forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fila.forEach { opcionSabor ->
                                val seleccionado = saboresSeleccionados.any { it.equals(opcionSabor, ignoreCase = true) }
                                Button(
                                    onClick = {
                                        val nuevaLista = if (seleccionado) {
                                            saboresSeleccionados.filterNot { it.equals(opcionSabor, ignoreCase = true) }
                                        } else {
                                            saboresSeleccionados + opcionSabor
                                        }
                                        sabor = nuevaLista.joinToString(", ")
                                        if (!seleccionado && !stockPorSaborState.containsKey(opcionSabor)) {
                                            stockPorSaborState = stockPorSaborState + (opcionSabor to "1")
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (seleccionado) VapeOnRed else VapeOnInputBackground,
                                        contentColor = VapeOnTextPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (seleccionado) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Seleccionado",
                                                tint = VapeOnTextPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = opcionSabor,
                                            fontSize = 12.sp,
                                            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (fila.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Entradas de Stock individual por cada Sabor seleccionado
                if (saboresSeleccionados.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Stock por Sabor seleccionado",
                        color = VapeOnGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    saboresSeleccionados.forEach { saborItem ->
                        val valorActual = stockPorSaborState[saborItem] ?: "1"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = saborItem,
                                color = VapeOnTextPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = valorActual,
                                onValueChange = { nuevoValor ->
                                    stockPorSaborState = stockPorSaborState + (saborItem to nuevoValor)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = VapeOnInputBackground,
                                    unfocusedContainerColor = VapeOnInputBackground,
                                    focusedBorderColor = VapeOnGold,
                                    unfocusedBorderColor = VapeOnInputBorder,
                                    focusedTextColor = VapeOnTextPrimary,
                                    unfocusedTextColor = VapeOnTextPrimary
                                ),
                                modifier = Modifier.width(90.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            val errorVisible = mensajeError.ifEmpty { productoViewModel.errorEdicion.orEmpty() }
            if (errorVisible.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorVisible,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            // Recarga explícita: reemplaza el formulario; nunca reenvía la edición antigua.
            TextButton(
                onClick = { productoViewModel.cargarProductoEdicion(producto.id) },
                enabled = !cargando
            ) { 
                Text(
                    text = "Recargar datos",
                    color = VapeOnGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(36.dp))

            // ==========================================
            // BOTÓN PRINCIPAL: GUARDAR CAMBIOS
            // ==========================================
            Button(
                onClick = {
                    val precioDouble = precio.toDoubleOrNull()
                    val stockInt = stock.toIntOrNull()

                    if (nombre.isBlank()) {
                        mensajeError = "Por favor ingresa el nombre del producto"
                        return@Button
                    }

                    if (precioDouble == null || !precioDouble.isFinite() || precioDouble <= 0.0) {
                        mensajeError = "Por favor ingresa un precio válido mayor a 0"
                        return@Button
                    }

                    if (stockInt == null || stockInt < 0) {
                        mensajeError = "Por favor ingresa un stock válido"
                        return@Button
                    }

                    val saboresSeleccionadosSet = if (tieneSabor) {
                        sabor.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                    } else emptySet()

                    val stockMap = if (tieneSabor) {
                        saboresSeleccionadosSet.associateWith { s ->
                            (stockPorSaborState[s] ?: "1").toIntOrNull() ?: 1
                        }
                    } else emptyMap()

                    val saborFinal = if (tieneSabor) sabor else ""
                    val stockFinal = if (tieneSabor && stockMap.isNotEmpty()) stockMap.values.sum() else (stockInt ?: 0)

                    // Registrar o actualizar automáticamente en la colección máster /sabores
                    if (tieneSabor) {
                        stockMap.forEach { (nombreSabor, stockSabor) ->
                            val existente = saboresViewModel.listaSabores.find {
                                it.nombre.equals(nombreSabor.trim(), ignoreCase = true) &&
                                        it.marca.equals(marca.trim(), ignoreCase = true)
                            }
                            if (existente != null) {
                                saboresViewModel.editarSabor(
                                    saborAntiguo = existente,
                                    nuevoNombre = existente.nombre,
                                    nuevaMarca = existente.marca,
                                    nuevoStock = stockSabor
                                )
                            } else {
                                saboresViewModel.agregarSabor(
                                    nombre = nombreSabor.trim(),
                                    marca = marca.trim(),
                                    stock = stockSabor
                                )
                            }
                        }
                    }

                    mensajeError = ""
                    productoViewModel.guardarProductoEdicion(
                        nombre = nombre,
                        marca = marca,
                        precio = precioDouble,
                        stock = stockFinal,
                        descripcion = descripcion,
                        sabor = saborFinal,
                        stockPorSabor = stockMap
                    )
                },
                enabled = !cargando && !productoViewModel.requiereRecargarEdicion,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VapeOnRed,
                    contentColor = VapeOnTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = VapeOnTextPrimary
                    )
                } else {
                    Text(
                        text = "GUARDAR CAMBIOS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
