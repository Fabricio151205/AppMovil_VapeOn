package com.example.vapeon_movil.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.vapeon_movil.entities.Direccion
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.DireccionesViewModel

/**
 * Selección de la dirección de entrega durante el checkout.
 *
 * La lista proviene de la colección `direcciones` del cliente; ya no hay
 * direcciones fijas. Desde aquí se pueden agregar o editar direcciones y ver
 * cada una en el mapa.
 */
@Composable
fun SeleccionDireccionScreen(
    usuario: Usuario?,
    direccionesViewModel: DireccionesViewModel,
    direccion: String,
    cambiarDireccion: (Direccion) -> Unit,
    volver: () -> Unit,
    continuar: () -> Unit,
    agregarDireccion: () -> Unit,
    editarDireccion: (Direccion) -> Unit
) {
    val id = usuario?.takeIf { it.rol.equals("CLIENTE", true) }?.id.orEmpty()
    val vm = direccionesViewModel
    var direccionSeleccionada by remember { mutableStateOf(direccion) }
    var direccionParaMapa by remember { mutableStateOf<Direccion?>(null) }

    LaunchedEffect(id) {
        vm.actualizarUsuario(usuario)
        if (id.isNotBlank()) vm.cargarDirecciones(usuario)
    }

    BackHandler(enabled = direccionParaMapa != null) {
        direccionParaMapa = null
    }

    val seleccionValida = vm.direcciones.any { it.direccion == direccionSeleccionada }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = VapeOnBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ENCABEZADO SUPERIOR CON FLECHA Y TÍTULO
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = volver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = VapeOnTextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Selecciona una\nDirección",
                        color = VapeOnTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (id.isBlank() || vm.usuarioId != id) {
                    Text(
                        text = "Inicia sesión como cliente para usar tus direcciones.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    when {
                        vm.cargando && vm.direcciones.isEmpty() -> {
                            CircularProgressIndicator(color = VapeOnGold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Cargando tus direcciones…",
                                color = VapeOnTextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        else -> {
                            vm.error?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                                TextButton(
                                    onClick = { vm.cargarDirecciones(usuario) },
                                    enabled = !vm.cargando,
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) { Text("Reintentar", color = VapeOnGold) }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (vm.direcciones.isEmpty()) {
                                Text(
                                    text = "Aún no tienes direcciones guardadas.",
                                    color = VapeOnTextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Agrega una para continuar con la entrega.",
                                    color = VapeOnTextSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                            } else if (!seleccionValida) {
                                Text(
                                    text = "Toca una dirección para seleccionarla.",
                                    color = VapeOnGold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                vm.direcciones.forEach { dir ->
                                    val esSeleccionada = dir.direccion == direccionSeleccionada

                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (esSeleccionada) VapeOnSelected
                                            else VapeOnCardNotSelected
                                        ),
                                        border = BorderStroke(1.dp, VapeOnCardBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                direccionSeleccionada = dir.direccion
                                                cambiarDireccion(dir)
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(18.dp)
                                        ) {
                                            Text(
                                                text = dir.alias,
                                                color = VapeOnGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = dir.direccion,
                                                color = VapeOnTextPrimary,
                                                fontSize = 14.sp
                                            )

                                            if (dir.referencia.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Ref.: ${dir.referencia}",
                                                    color = VapeOnTextSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            HorizontalDivider(color = VapeOnGold, thickness = 2.dp)

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Editar",
                                                    color = VapeOnGold,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier
                                                        .clickable { editarDireccion(dir) }
                                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                                )
                                                Text(
                                                    text = "Ver Ubicación",
                                                    color = VapeOnTextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier
                                                        .clickable { direccionParaMapa = dir }
                                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "La dirección se guarda en tu cuenta y se puede reutilizar en tus próximas compras.",
                        color = VapeOnTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.height(30.dp))

                // FILA DE BOTONES INFERIORES: CONTINUAR Y AGREGAR NUEVA DIRECCIÓN
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            vm.direcciones.firstOrNull { it.direccion == direccionSeleccionada }?.let { seleccionada ->
                                cambiarDireccion(seleccionada)
                                continuar()
                            }
                        },
                        enabled = seleccionValida && !vm.cargando,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VapeOnRed,
                            contentColor = VapeOnTextPrimary,
                            disabledContainerColor = VapeOnRedDark,
                            disabledContentColor = VapeOnTextSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "CONTINUAR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Button(
                        onClick = agregarDireccion,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VapeOnRedDark,
                            contentColor = VapeOnTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "AGREGAR\nNUEVA DIRECCIÓN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        direccionParaMapa?.let { direccionMapa ->
            VerUbicacionScreen(
                direccion = direccionMapa.direccion,
                volver = { direccionParaMapa = null },
                latitud = direccionMapa.latitud,
                longitud = direccionMapa.longitud
            )
        }
    }
}
