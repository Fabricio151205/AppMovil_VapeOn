package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.DireccionesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarDireccionScreen(
    usuario: Usuario?,
    direccionesViewModel: DireccionesViewModel,
    volver: () -> Unit
) {
    val vm = direccionesViewModel
    var alias by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var referencia by rememberSaveable { mutableStateOf("") }
    var latitud by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitud by rememberSaveable { mutableStateOf<Double?>(null) }
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
                        Text("NUEVA DIRECCIÓN", color = VapeOnGold, fontWeight = FontWeight.Bold)
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
                Text(
                    "El alias te ayuda a reconocerla en el checkout.",
                    color = VapeOnTextSecondary
                )

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
                        vm.agregarDireccion(
                            usuario, alias, direccion, referencia, latitud, longitud
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
                        if (vm.guardando) "GUARDANDO…" else "GUARDAR DIRECCIÓN",
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

/** Tarjeta con el acceso al selector de punto exacto en el mapa. */
@Composable
internal fun SelectorUbicacionDireccion(
    tieneUbicacion: Boolean,
    habilitado: Boolean,
    onAbrirMapa: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = VapeOnGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ubicación en el mapa",
                    color = VapeOnGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Text(
                text = if (tieneUbicacion) {
                    "Punto exacto confirmado. Puedes ajustarlo cuando quieras."
                } else {
                    "Escribe la dirección y luego marca en el mapa el punto exacto " +
                        "donde debe llegar el pedido."
                },
                color = VapeOnTextSecondary,
                fontSize = 13.sp
            )

            OutlinedButton(
                onClick = onAbrirMapa,
                enabled = habilitado,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, VapeOnGold)
            ) {
                Text(
                    text = if (tieneUbicacion) "CAMBIAR PUNTO EN EL MAPA" else "SELECCIONAR EN EL MAPA",
                    color = VapeOnGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
internal fun CampoDireccion(
    valor: String,
    onValorCambia: (String) -> Unit,
    etiqueta: String,
    maxLineas: Int
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambia,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(etiqueta) },
        singleLine = maxLineas == 1,
        minLines = if (maxLineas == 1) 1 else 2,
        maxLines = maxLineas,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = VapeOnTextPrimary,
            unfocusedTextColor = VapeOnTextPrimary,
            focusedBorderColor = VapeOnGold,
            unfocusedBorderColor = VapeOnBorder
        )
    )
}
