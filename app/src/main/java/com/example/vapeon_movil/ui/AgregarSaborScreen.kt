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
import androidx.activity.compose.BackHandler
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.marcasUnicas
import com.example.vapeon_movil.viewmodels.MarcasViewModel
import com.example.vapeon_movil.viewmodels.SaboresViewModel

/**
 * Pantalla de alta (CREATE) de SaborItem.
 * Formulario del catálogo maestro: nombre y marca. El inventario vive en Producto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarSaborScreen(
    saboresViewModel: SaboresViewModel,
    marcasViewModel: MarcasViewModel,
    volver: () -> Unit
) {
    BackHandler(onBack = volver)
    var nombre by rememberSaveable { mutableStateOf("") }
    var marca by rememberSaveable { mutableStateOf("") }
    var mensajeError by rememberSaveable { mutableStateOf("") }
    var menuMarcaExpandido by remember { mutableStateOf(false) }
    val marcasDisponibles = marcasUnicas(marcasViewModel.listaMarcas.map { it.nombre })
    LaunchedEffect(Unit) { marcasViewModel.cargarMarcas() }
    LaunchedEffect(marcasDisponibles) {
        if (marca.isBlank() && marcasDisponibles.isNotEmpty()) marca = marcasDisponibles.first()
    }

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
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "VAPEON",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = VapeOnGold,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(VapeOnRedDark),
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

            // Titulo
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                Column {
                    Text(
                        text = "AGREGAR SABOR",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = VapeOnTextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Completa los datos del nuevo sabor",
                        fontSize = 14.sp,
                        color = VapeOnTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Campo: Nombre
            Text(
                text = "Nombre del Sabor",
                color = VapeOnTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; mensajeError = "" },
                placeholder = { Text("Ej: Mango Ice", color = VapeOnTextSecondary, fontSize = 15.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VapeOnInputBackground,
                    unfocusedContainerColor = VapeOnInputBackground,
                    focusedBorderColor = VapeOnGold, unfocusedBorderColor = VapeOnInputBorder,
                    focusedTextColor = VapeOnTextPrimary, unfocusedTextColor = VapeOnTextPrimary,
                    cursorColor = VapeOnGold
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
                    marcasDisponibles.forEach { opcion ->
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

            // Preview del sabor
            if (nombre.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Vista previa",
                            color = VapeOnGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = nombre, color = VapeOnTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = marca, color = VapeOnGold, fontSize = 13.sp)
                    }
                }
            }

            if (mensajeError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = mensajeError, color = MaterialTheme.colorScheme.error, fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(36.dp))

            // Boton Guardar
            Button(
                onClick = {
                    when {
                        nombre.isBlank() ->
                            mensajeError = "El nombre del sabor es obligatorio"
                        marca.isBlank() -> mensajeError = "Selecciona una marca registrada"
                        else -> {
                            mensajeError = ""
                            saboresViewModel.agregarSabor(nombre.trim(), marca)
                        }
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VapeOnRed, contentColor = VapeOnTextPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    text = "GUARDAR SABOR",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
