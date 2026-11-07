package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Direccion
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.DireccionesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarDireccionScreen(
    usuario: Usuario?,
    direccionOriginal: Direccion,
    direccionesViewModel: DireccionesViewModel,
    volver: () -> Unit
) {
    val vm = direccionesViewModel
    var alias by rememberSaveable(direccionOriginal.id) { mutableStateOf(direccionOriginal.alias) }
    var direccion by rememberSaveable(direccionOriginal.id) { mutableStateOf(direccionOriginal.direccion) }
    var referencia by rememberSaveable(direccionOriginal.id) { mutableStateOf(direccionOriginal.referencia) }
    var latitud by rememberSaveable(direccionOriginal.id) { mutableStateOf(direccionOriginal.latitud) }
    var longitud by rememberSaveable(direccionOriginal.id) { mutableStateOf(direccionOriginal.longitud) }
    var mostrarMapa by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = !mostrarMapa) { if (!vm.guardando) volver() }

    LaunchedEffect(vm.usuarioId) { vm.actualizarUsuario(usuario) }
    LaunchedEffect(vm.operacionExitosa) {
        if (vm.operacionExitosa) {
            vm.consumirExito()
            volver()
        }
    }
    LaunchedEffect(vm.error) {
        vm.error?.let { error = it; vm.limpiarError() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = VapeOnBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Text("EDITAR DIRECCIÓN", color = VapeOnGold, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (!vm.guardando) volver() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Regresar",
                                tint = VapeOnTextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CampoDireccion(
                    valor = alias,
                    onValorCambia = { alias = it; error = "" },
                    etiqueta = "Alias (Casa, Trabajo, etc.)",
                    maxLineas = 1
                )

                CampoDireccion(
                    valor = direccion,
                    onValorCambia = {
                        // Si cambia el texto, el punto exacto anterior deja de ser confiable.
                        if (it != direccion) {
                            latitud = null
                            longitud = null
                        }
                        direccion = it
                        error = ""
                    },
                    etiqueta = "Dirección de entrega",
                    maxLineas = 3
                )

                CampoDireccion(
                    valor = referencia,
                    onValorCambia = { referencia = it; error = "" },
                    etiqueta = "Referencia (opcional)",
                    maxLineas = 3
                )

                SelectorUbicacionDireccion(
                    tieneUbicacion = latitud != null && longitud != null,
                    habilitado = !vm.guardando,
                    onAbrirMapa = { mostrarMapa = true }
                )

                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        vm.editarDireccion(
                            usuario, direccionOriginal, alias, direccion, referencia,
                            latitud, longitud
                        )
                    },
                    enabled = !vm.guardando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary
                    )
                ) {
                    Text(
                        if (vm.guardando) "GUARDANDO…" else "GUARDAR CAMBIOS",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (mostrarMapa) {
            SeleccionarUbicacionScreen(
                direccion = direccion,
                latitudInicial = latitud,
                longitudInicial = longitud,
                onConfirmar = { la, lo, texto ->
                    latitud = la
                    longitud = lo
                    if (!texto.isNullOrBlank()) direccion = texto.trim()
                    error = ""
                    mostrarMapa = false
                },
                volver = { mostrarMapa = false }
            )
        }
    }
}
