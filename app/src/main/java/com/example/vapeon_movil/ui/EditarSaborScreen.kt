package com.example.vapeon_movil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.vapeon_movil.entities.SaborItem
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.LISTA_MARCAS_DISPONIBLES
import com.example.vapeon_movil.viewmodels.SaboresViewModel

/**
 * Pantalla de edicion (UPDATE) de SaborItem.
 * Formulario pre-rellenado con los datos actuales del sabor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarSaborScreen(
    saborOriginal: SaborItem,
    saboresViewModel: SaboresViewModel,
    volver: () -> Unit
) {
    BackHandler(onBack = volver)
    var nombre by rememberSaveable { mutableStateOf(saborOriginal.nombre) }
    var marca by rememberSaveable { mutableStateOf(saborOriginal.marca) }
    var stock by rememberSaveable { mutableStateOf(saborOriginal.stock.toString()) }
    var mensajeError by rememberSaveable { mutableStateOf("") }
    var menuMarcaExpandido by remember { mutableStateOf(false) }
    var mostrarConfirmacionEliminar by remember { mutableStateOf(false) }

    LaunchedEffect(saboresViewModel.operacionExitosa) {
        if (saboresViewModel.operacionExitosa) {
            saboresViewModel.consumirExito()
            volver()
        }
    }
    LaunchedEffect(saboresViewModel.mensajeError) {
        saboresViewModel.mensajeError?.let { msg ->
            mensajeError = msg
            saboresViewModel.limpiarError()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = VapeOnBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barra superior
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = volver) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = VapeOnTextPrimary, modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "VAPEON",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VapeOnGold, letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(VapeOnRedDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Perfil",
                        tint = VapeOnTextPrimary, modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Titulo
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                Column {
                    Text(
                        text = "EDITAR SABOR",
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, color = VapeOnTextPrimary, letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Modifica los datos del sabor",
                        fontSize = 14.sp, color = VapeOnTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Chip con datos originales
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = VapeOnSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Original:",
                        color = VapeOnTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${saborOriginal.nombre} — ${saborOriginal.marca} (Stock: ${saborOriginal.stock})",
                        color = VapeOnGold, fontSize = 13.sp, fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Campo: Nombre
            Text(
                text = "Nombre del Sabor",
                color = VapeOnTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; mensajeError = "" },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VapeOnInputBackground, unfocusedContainerColor = VapeOnInputBackground,
                    focusedBorderColor = VapeOnGold, unfocusedBorderColor = VapeOnInputBorder,
                    focusedTextColor = VapeOnTextPrimary, unfocusedTextColor = VapeOnTextPrimary, cursorColor = VapeOnGold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Marca
            Text(
                text = "Marca",
                color = VapeOnTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            )
            ExposedDropdownMenuBox(
                expanded = menuMarcaExpandido,
                onExpandedChange = { menuMarcaExpandido = !menuMarcaExpandido },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = marca, onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuMarcaExpandido) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VapeOnInputBackground, unfocusedContainerColor = VapeOnInputBackground,
                        focusedBorderColor = VapeOnGold, unfocusedBorderColor = VapeOnInputBorder,
                        focusedTextColor = VapeOnTextPrimary, unfocusedTextColor = VapeOnTextPrimary, cursorColor = VapeOnGold
                    ),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = menuMarcaExpandido,
                    onDismissRequest = { menuMarcaExpandido = false },
                    modifier = Modifier.background(VapeOnSurface)
                ) {
                    LISTA_MARCAS_DISPONIBLES.forEach { opcion ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    opcion, color = VapeOnTextPrimary,
                                    fontWeight = if (marca == opcion) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = { marca = opcion; menuMarcaExpandido = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Stock
            Text(
                text = "Stock disponible",
                color = VapeOnTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = stock,
                onValueChange = { nuevo -> stock = nuevo.filter { it.isDigit() } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VapeOnInputBackground, unfocusedContainerColor = VapeOnInputBackground,
                    focusedBorderColor = VapeOnGold, unfocusedBorderColor = VapeOnInputBorder,
                    focusedTextColor = VapeOnTextPrimary, unfocusedTextColor = VapeOnTextPrimary, cursorColor = VapeOnGold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (mensajeError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = mensajeError, color = MaterialTheme.colorScheme.error, fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(36.dp))

            // Boton Guardar Cambios
            Button(
                onClick = {
                    val stockInt = stock.toIntOrNull()
                    when {
                        nombre.isBlank() ->
                            mensajeError = "El nombre del sabor es obligatorio"
                        stockInt == null || stockInt < 0 ->
                            mensajeError = "Ingresa un stock valido (>= 0)"
                        else -> {
                            mensajeError = ""
                            saboresViewModel.editarSabor(saborOriginal, nombre.trim(), marca, stockInt)
                        }
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed, contentColor = VapeOnTextPrimary),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    text = "GUARDAR CAMBIOS",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Boton Eliminar
            OutlinedButton(
                onClick = { mostrarConfirmacionEliminar = true },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VapeOnRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, VapeOnRed),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    text = "ELIMINAR SABOR",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialogo confirmar eliminacion
    if (mostrarConfirmacionEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionEliminar = false },
            title = {
                Text("Eliminar Sabor", fontWeight = FontWeight.Bold, color = VapeOnGold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Esta accion no se puede deshacer.",
                        color = VapeOnTextSecondary, fontSize = 14.sp
                    )
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(saborOriginal.nombre, color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                            Text("Marca: ${saborOriginal.marca}", color = VapeOnGold, fontSize = 13.sp)
                            Text("Stock: ${saborOriginal.stock}", color = VapeOnTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saboresViewModel.eliminarSabor(saborOriginal)
                        mostrarConfirmacionEliminar = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                ) {
                    Text("Eliminar", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionEliminar = false }) {
                    Text("Cancelar", color = VapeOnTextSecondary)
                }
            },
            containerColor = VapeOnSurface
        )
    }
}
