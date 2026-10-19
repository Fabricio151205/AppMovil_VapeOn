package com.example.vapeon_movil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.ProductoViewModel

@Composable
fun EditarProductoScreen(
    producto: Producto,
    productoViewModel: ProductoViewModel,
    volver: () -> Unit
) {
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
        FormularioEditarProducto(documento, productoViewModel, volver)
    }
}

@Composable
private fun FormularioEditarProducto(
    documento: ProductoDocumento, productoViewModel: ProductoViewModel, volver: () -> Unit
) {
    val producto = Producto(
        id = documento.name.substringAfterLast('/'), nombre = documento.fields.nombre?.asString().orEmpty(),
        marca = documento.fields.marca?.asString().orEmpty(), precio = documento.fields.precio?.asDouble() ?: 0.0,
        stock = documento.fields.stock?.asInt() ?: 0, descripcion = documento.fields.descripcion?.asString().orEmpty()
    )

    var nombre by rememberSaveable { mutableStateOf(producto.nombre) }
    var marca by rememberSaveable { mutableStateOf(producto.marca) }
    var precio by rememberSaveable { mutableStateOf(producto.precio.toString()) }
    var stock by rememberSaveable { mutableStateOf(producto.stock.toString()) }
    var descripcion by rememberSaveable { mutableStateOf(producto.descripcion) }

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
                color = VapeOnTextSecondary,
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

            // Campo: Marca
            Text(
                text = "Marca",
                color = VapeOnTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = marca,
                onValueChange = { marca = it },
                placeholder = {
                    Text(text = "Marca", color = VapeOnTextSecondary, fontSize = 15.sp)
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

            //Campo: Precio
            Text(
                text = "Precio (S/)",
                color = VapeOnTextSecondary,
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
                color = VapeOnTextSecondary,
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

            //Campo: Descripción
            Text(
                text = "Descripción del producto",
                color = VapeOnTextSecondary,
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
            TextButton(onClick = { productoViewModel.cargarProductoEdicion(producto.id) },
                enabled = !cargando) { Text("Recargar datos") }

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

                    mensajeError = ""
                    productoViewModel.guardarProductoEdicion(nombre, marca, precioDouble, stockInt,
                        descripcion)
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
