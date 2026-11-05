package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Marca
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.MarcasViewModel

/**
 * Edición de una marca.
 *
 * Si la marca tiene productos o sabores asociados, su identidad no puede
 * cambiarse: el campo y el botón se deshabilitan para no depender solo del
 * error que devuelve el ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarMarcaScreen(
    marca: Marca,
    tieneRelaciones: Boolean,
    marcasViewModel: MarcasViewModel,
    relacionesCargando: Boolean = false,
    volver: () -> Unit
) {
    BackHandler(onBack = volver)
    var nombre by rememberSaveable(marca.id) { mutableStateOf(marca.nombre) }
    var error by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(marcasViewModel.operacionExitosa) {
        if (marcasViewModel.operacionExitosa) { marcasViewModel.consumirExito(); volver() }
    }
    LaunchedEffect(marcasViewModel.mensajeError) {
        marcasViewModel.mensajeError?.let { error = it; marcasViewModel.limpiarError() }
    }

    val bloqueado = tieneRelaciones || relacionesCargando

    Scaffold(
        containerColor = VapeOnBackground,
        topBar = {
            TopAppBar(
                title = { Text("EDITAR MARCA", color = VapeOnGold, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = volver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = VapeOnTextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            if (tieneRelaciones) {
                Text("Esta marca tiene productos o sabores asociados. Su identidad no puede cambiarse.", color = VapeOnGold)
            } else if (relacionesCargando) {
                Text("Verificando productos y sabores asociados...", color = VapeOnTextSecondary)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; error = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre") },
                singleLine = true,
                enabled = !bloqueado,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VapeOnTextPrimary,
                    unfocusedTextColor = VapeOnTextPrimary,
                    focusedBorderColor = VapeOnGold
                )
            )
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { marcasViewModel.editarMarca(marca, nombre, tieneRelaciones) },
                enabled = !marcasViewModel.cargando && !bloqueado,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
            ) { Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Bold) }
        }
    }
}
