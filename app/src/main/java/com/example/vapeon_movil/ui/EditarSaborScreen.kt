package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import com.example.vapeon_movil.components.SelectorImagenSabor
import com.example.vapeon_movil.entities.SaborItem
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.marcasParaFormulario
import com.example.vapeon_movil.viewmodels.MarcasViewModel
import com.example.vapeon_movil.viewmodels.SaboresViewModel

/**
 * Edición de un sabor del catálogo maestro.
 *
 * Si el administrador desmarca la imagen y guarda, el campo `imagenUri` se
 * elimina del documento de Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarSaborScreen(
    saborOriginal: SaborItem,
    saboresViewModel: SaboresViewModel,
    marcasViewModel: MarcasViewModel,
    volver: () -> Unit
) {
    BackHandler(onBack = volver)
    var nombre by rememberSaveable(saborOriginal.id) { mutableStateOf(saborOriginal.nombre) }
    var marca by rememberSaveable(saborOriginal.id) { mutableStateOf(saborOriginal.marca) }
    var mensajeError by rememberSaveable { mutableStateOf("") }
    var menuMarcaExpandido by remember { mutableStateOf(false) }
    var mostrarConfirmacionEliminar by remember { mutableStateOf(false) }
    var imagenDataUri by remember(saborOriginal.id) { mutableStateOf(saborOriginal.imagenUri) }
    var procesandoImagen by rememberSaveable { mutableStateOf(false) }

    val marcasDisponibles = marcasParaFormulario(
        marcasViewModel.listaMarcas.map { it.nombre } + saborOriginal.marca
    )
    LaunchedEffect(Unit) { marcasViewModel.cargarMarcas() }

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
            modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = volver) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text("VAPEON", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VapeOnGold, letterSpacing = 1.sp)
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(VapeOnRedDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Perfil", tint = VapeOnTextPrimary, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                Column {
                    Text("EDITAR SABOR", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = VapeOnTextPrimary, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Modifica los datos del sabor", fontSize = 14.sp, color = VapeOnTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                "Nombre del Sabor",
                color = VapeOnTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; mensajeError = "" },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VapeOnInputBackground,
                    unfocusedContainerColor = VapeOnInputBackground,
                    focusedBorderColor = VapeOnGold,
                    unfocusedBorderColor = VapeOnInputBorder,
                    focusedTextColor = VapeOnTextPrimary,
                    unfocusedTextColor = VapeOnTextPrimary,
                    cursorColor = VapeOnGold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Marca",
                color = VapeOnTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            )
            ExposedDropdownMenuBox(
                expanded = menuMarcaExpandido,
                onExpandedChange = { menuMarcaExpandido = !menuMarcaExpandido },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = marca,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuMarcaExpandido) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VapeOnInputBackground,
                        unfocusedContainerColor = VapeOnInputBackground,
                        focusedBorderColor = VapeOnGold,
                        unfocusedBorderColor = VapeOnInputBorder,
                        focusedTextColor = VapeOnTextPrimary,
                        unfocusedTextColor = VapeOnTextPrimary,
                        cursorColor = VapeOnGold
                    ),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = menuMarcaExpandido,
                    onDismissRequest = { menuMarcaExpandido = false },
                    modifier = Modifier.background(VapeOnSurface)
                ) {
                    marcasDisponibles.forEach { opcion ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    opcion,
                                    color = VapeOnTextPrimary,
                                    fontWeight = if (marca == opcion) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = { marca = opcion; menuMarcaExpandido = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SelectorImagenSabor(
                imagenDataUri = imagenDataUri,
                onImagenCambiada = { imagenDataUri = it },
                procesandoImagen = procesandoImagen,
                onProcesandoCambia = { procesandoImagen = it },
                textoCheckbox = "El sabor cuenta con una imagen",
                textoAyuda = "Si desmarcas y guardas, se eliminará la imagen actual."
            )

            if (mensajeError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = mensajeError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    when {
                        nombre.isBlank() -> mensajeError = "El nombre del sabor es obligatorio"
                        marca.isBlank() -> mensajeError = "Selecciona una marca"
                        procesandoImagen -> mensajeError = "Espera a que termine de procesarse la imagen"
                        else -> {
                            mensajeError = ""
                            saboresViewModel.editarSabor(saborOriginal, nombre.trim(), marca, imagenDataUri)
                        }
                    }
                },
                enabled = !saboresViewModel.cargando && !procesandoImagen,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed, contentColor = VapeOnTextPrimary),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(text = "GUARDAR CAMBIOS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { mostrarConfirmacionEliminar = true },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text = "ELIMINAR SABOR", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (mostrarConfirmacionEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionEliminar = false },
            title = { Text("Eliminar sabor", color = VapeOnGold) },
            text = { Text("¿Seguro que deseas eliminar '" + saborOriginal.nombre + "' de " + saborOriginal.marca + "?") },
            confirmButton = {
                TextButton(onClick = {
                    saboresViewModel.eliminarSabor(saborOriginal)
                    mostrarConfirmacionEliminar = false
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionEliminar = false }) { Text("Cancelar", color = VapeOnTextPrimary) }
            },
            containerColor = VapeOnSurface
        )
    }
}
