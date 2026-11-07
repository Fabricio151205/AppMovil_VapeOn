package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Direccion
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.DireccionesViewModel

/**
 * CRUD de direcciones del cliente.
 *
 * Se abre desde el menú lateral ("Mis direcciones") y desde el checkout. El
 * mapa se muestra como capa a pantalla completa para no perder la lista.
 */
@Composable
fun DireccionesScreen(
    usuario: Usuario?,
    direccionesViewModel: DireccionesViewModel,
    volver: () -> Unit,
    agregarDireccion: () -> Unit,
    editarDireccion: (Direccion) -> Unit
) {
    val id = usuario?.takeIf { it.rol.equals("CLIENTE", true) }?.id.orEmpty()
    val vm = direccionesViewModel
    var eliminar by remember { mutableStateOf<Direccion?>(null) }
    var direccionParaMapa by remember { mutableStateOf<Direccion?>(null) }

    LaunchedEffect(id) {
        vm.actualizarUsuario(usuario)
        if (id.isNotBlank()) vm.cargarDirecciones(usuario)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PasoCheckout(
            titulo = "Mis direcciones",
            volver = volver,
            bloqueado = direccionParaMapa != null
        ) {
            if (id.isBlank() || vm.usuarioId != id) {
                Text(
                    "Inicia sesión como cliente para gestionar tus direcciones.",
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    "Guarda tus direcciones para reutilizarlas en cada entrega.",
                    color = VapeOnTextSecondary
                )

                Button(
                    onClick = agregarDireccion,
                    enabled = !vm.guardando && vm.eliminandoId == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary
                    )
                ) {
                    Text("Agregar nueva dirección", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = { vm.cargarDirecciones(usuario) },
                        enabled = !vm.cargando,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            if (vm.error == null) "Actualizar" else "Reintentar",
                            color = VapeOnGold
                        )
                    }
                    if (vm.cargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = VapeOnGold
                        )
                    }
                }

                vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                vm.mensaje?.let { Text(it, color = VapeOnGold) }

                when {
                    vm.cargando && vm.direcciones.isEmpty() -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = VapeOnGold)
                    }

                    vm.direcciones.isEmpty() -> Text(
                        "No tienes direcciones guardadas.",
                        color = VapeOnTextSecondary
                    )

                    else -> vm.direcciones.forEach { direccion ->
                        TarjetaDireccion(
                            direccion = direccion,
                            eliminando = vm.eliminandoId == direccion.id,
                            accionesHabilitadas = vm.eliminandoId == null && !vm.guardando,
                            onVerUbicacion = { direccionParaMapa = direccion },
                            onEditar = { editarDireccion(direccion) },
                            onEliminar = { eliminar = direccion }
                        )
                    }
                }
            }
        }

        direccionParaMapa?.let { direccionMapa ->
            BackHandler { direccionParaMapa = null }
            VerUbicacionScreen(
                direccion = direccionMapa.direccion,
                volver = { direccionParaMapa = null },
                latitud = direccionMapa.latitud,
                longitud = direccionMapa.longitud
            )
        }
    }

    eliminar?.let { direccion ->
        AlertDialog(
            onDismissRequest = { eliminar = null },
            title = { Text("Eliminar dirección", color = VapeOnGold) },
            text = {
                Text(
                    "¿Eliminar \"${direccion.alias}\" (${direccion.direccion})? " +
                        "No se puede deshacer.",
                    color = VapeOnTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.eliminarDireccion(usuario, direccion)
                        eliminar = null
                    },
                    enabled = vm.eliminandoId == null,
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(
                    onClick = { eliminar = null },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Cancelar") }
            },
            containerColor = VapeOnSurface
        )
    }
}

@Composable
private fun TarjetaDireccion(
    direccion: Direccion,
    eliminando: Boolean,
    accionesHabilitadas: Boolean,
    onVerUbicacion: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardNotSelected),
        border = BorderStroke(1.dp, VapeOnCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
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
                    text = direccion.alias,
                    color = VapeOnGold,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = direccion.direccion,
                color = VapeOnTextPrimary
            )

            if (direccion.referencia.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ref.: ${direccion.referencia}",
                    color = VapeOnTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = VapeOnGold, thickness = 1.dp)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = onVerUbicacion,
                    enabled = accionesHabilitadas,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Ver ubicación", color = VapeOnTextPrimary) }

                TextButton(
                    onClick = onEditar,
                    enabled = accionesHabilitadas,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Editar", color = VapeOnGold) }

                TextButton(
                    onClick = onEliminar,
                    enabled = accionesHabilitadas,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        if (eliminando) "Eliminando…" else "Eliminar",
                        color = VapeOnRed
                    )
                }
            }
        }
    }
}
