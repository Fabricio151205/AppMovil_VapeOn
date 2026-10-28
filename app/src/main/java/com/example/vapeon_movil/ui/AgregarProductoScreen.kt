package com.example.vapeon_movil.ui

import androidx.compose.foundation.BorderStroke
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
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.LISTA_MARCAS_DISPONIBLES
import com.example.vapeon_movil.utils.obtenerSaboresPorMarca
import com.example.vapeon_movil.viewmodels.ProductoViewModel
import com.example.vapeon_movil.viewmodels.SaboresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarProductoScreen(
    productoViewModel: ProductoViewModel,
    saboresViewModel: SaboresViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    volver: () -> Unit
) {
    LaunchedEffect(Unit) { saboresViewModel.cargarSabores() }
    val anterior = productoViewModel.datosCreacionPendiente
    var nombre by rememberSaveable { mutableStateOf(anterior?.nombre?.asString().orEmpty()) }
    var marca by rememberSaveable { mutableStateOf(anterior?.marca?.asString().orEmpty()) }
    var precio by rememberSaveable { mutableStateOf(anterior?.precio?.asString().orEmpty()) }
    var stock by rememberSaveable { mutableStateOf(anterior?.stock?.asString().orEmpty()) }
    var descripcion by rememberSaveable { mutableStateOf(anterior?.descripcion?.asString().orEmpty()) }
    var sabor by rememberSaveable { mutableStateOf(anterior?.sabor?.asString().orEmpty()) }
    var tieneSabor by rememberSaveable { mutableStateOf(sabor.isNotBlank()) }

    val cargando = productoViewModel.cargandoCreacion
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
                    text = "AGREGAR PRODUCTO",
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
                    Text(text = "Ej.: LifePod Eco II", color = VapeOnTextSecondary, fontSize = 15.sp)
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
                text = "Marca del Producto",
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

            Text(
                text = "Descripción del Producto",
                color = VapeOnTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            // Campo: Descripción
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                placeholder = {
                    Text(text = "Ej.: En VapeOn nos importa...", color = VapeOnTextSecondary, fontSize = 15.sp)
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

            Text(
                text = "Precio Unitario",
                color = VapeOnTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            // Campo: Precio
            OutlinedTextField(
                value = precio,
                onValueChange = { precio = it },
                placeholder = {
                    Text(text = "Ej.: 50.00", color = VapeOnTextSecondary, fontSize = 15.sp)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

            Text(
                text = "Stock General",
                color = VapeOnTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            // Campo: Stock General
            OutlinedTextField(
                value = stock,
                onValueChange = { stock = it },
                placeholder = {
                    Text(text = "Ej.: 10", color = VapeOnTextSecondary, fontSize = 15.sp)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

            // Selector de Sabores dinámico por Marca (solo si tieneSabor es verdadero)
            val saboresDisponibles = remember(marca, saboresViewModel.listaSabores) { obtenerSaboresPorMarca(marca, saboresViewModel.listaSabores) }
            val saboresSeleccionados = sabor.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            var stockPorSaborState by remember {
                val inicial = sabor.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    .associateWith { "1" }
                mutableStateOf<Map<String, String>>(inicial)
            }

            if (tieneSabor) {
                Text(
                    text = "Seleccionar Sabores (${saboresDisponibles.size} disponibles)",
                    color = VapeOnTextPrimary,
                    fontSize = 15.sp,
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

            val errorVisible = mensajeError.ifEmpty { productoViewModel.errorCreacion.orEmpty() }
            if (errorVisible.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorVisible,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(36.dp))

            // ==========================================
            // BOTÓN PRINCIPAL: GUARDAR PRODUCTO
            // ==========================================
            Button(
                onClick = {
                    val precioDouble = precio.toDoubleOrNull()
                    val stockInt = stock.toIntOrNull()

                    if (nombre.isBlank() || marca.isBlank()) {
                        mensajeError = "Por favor ingresa nombre y marca del producto"
                        return@Button
                    }

                    if (precioDouble == null || !precioDouble.isFinite() || precioDouble <= 0.0) {
                        mensajeError = "Por favor ingresa un precio válido mayor a 0"
                        return@Button
                    }

                    if (stockInt == null || stockInt < 0) {
                        mensajeError = "Por favor ingresa un stock válido o positivo"
                        return@Button
                    }

                    val stockMap = if (tieneSabor) {
                        saboresSeleccionados.associateWith { s ->
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
                    productoViewModel.crearProducto(
                        nombre = nombre,
                        marca = marca,
                        precio = precioDouble,
                        stock = stockFinal,
                        descripcion = descripcion,
                        sabor = saborFinal,
                        stockPorSabor = stockMap
                    )
                },
                enabled = !cargando,
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
                        text = "GUARDAR PRODUCTO",
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
