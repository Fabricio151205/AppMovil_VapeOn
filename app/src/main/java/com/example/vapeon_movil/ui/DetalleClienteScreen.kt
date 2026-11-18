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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleClienteScreen(
    cliente: Usuario?,
    volver: () -> Unit,
    editar: () -> Unit,
    esAdmin: Boolean,
    cargando: Boolean = false,
    error: String? = null,
    reintentar: () -> Unit = {},
    guardandoPassword: Boolean = false,
    errorPassword: String? = null,
    mensajePassword: String? = null,
    conflictoPassword: Boolean = false,
    enviarRestablecimiento: () -> Unit = {},
    recargarPassword: () -> Unit = {},
    limpiarEstadoPassword: () -> Unit = {}
) {
    var mostrarCambioPassword by remember { mutableStateOf(false) }

    LaunchedEffect(mensajePassword) {
        if (mensajePassword != null) {
            mostrarCambioPassword = false
        }
    }

    BackHandler(onBack = volver)
    Scaffold(
        containerColor = VapeOnBackground,
        topBar = { TopAppBar(
            title = { Text("INFORMACIÓN DEL CLIENTE", color = VapeOnGold, fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = volver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = VapeOnTextPrimary)
            } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
        ) }
    ) { padding ->
        if (cargando) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator(color = VapeOnGold)
        } else if (cliente == null) Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(error ?: "El cliente no está disponible.", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(onClick = reintentar, colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)) {
                Text("Reintentar")
            }
        } else Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DatoClienteDetalle("Nombre", cliente.nombre)
                    DatoClienteDetalle("Apellido", cliente.apellido)
                    DatoClienteDetalle("Correo", cliente.correo)
                    DatoClienteDetalle("DNI", cliente.dni?.takeIf { it.isNotBlank() } ?: "No registrado")
                    DatoClienteDetalle("Teléfono", cliente.telefono)
                    DatoClienteDetalle("Fecha de nacimiento", cliente.fechaNacimiento)
                    DatoClienteDetalle("Rol", cliente.rol)
                    DatoClienteDetalle("Fecha de registro", cliente.createTime?.takeIf { it.isNotBlank() } ?: "No disponible")
                }
            }
            Button(onClick = editar, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed),
                shape = RoundedCornerShape(50)) {
                Text("EDITAR INFORMACIÓN", fontWeight = FontWeight.Bold)
            }
            if (esAdmin) {
                OutlinedButton(
                    onClick = {
                        limpiarEstadoPassword()
                        mostrarCambioPassword = true
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VapeOnGold)
                ) {
                    Text("RESTABLECER CONTRASEÑA", fontWeight = FontWeight.Bold)
                }
            }
            mensajePassword?.let {
                Text(it, color = VapeOnGold, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (mostrarCambioPassword && esAdmin) {
        AlertDialog(
            onDismissRequest = {
                if (!guardandoPassword) {
                    mostrarCambioPassword = false
                    limpiarEstadoPassword()
                }
            },
            containerColor = VapeOnCardBackground,
            title = { Text("Restablecer contraseña", color = VapeOnGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Se enviará un enlace de restablecimiento al correo del cliente. " +
                            "Por seguridad, el administrador no escribe ni conoce la nueva contraseña.",
                        color = VapeOnTextPrimary
                    )
                    errorPassword?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (conflictoPassword) {
                        TextButton(onClick = {
                            recargarPassword()
                        }, enabled = !guardandoPassword) {
                            Text("Recargar cliente", color = VapeOnGold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { enviarRestablecimiento() },
                    enabled = !guardandoPassword,
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                ) {
                    if (guardandoPassword) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = VapeOnTextPrimary, strokeWidth = 2.dp)
                    } else Text("Enviar correo")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarCambioPassword = false
                        limpiarEstadoPassword()
                    },
                    enabled = !guardandoPassword
                ) { Text("Cancelar", color = VapeOnGold) }
            }
        )
    }
}

@Composable
private fun DatoClienteDetalle(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, color = VapeOnGold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(valor.ifBlank { "No disponible" }, color = VapeOnTextPrimary, style = MaterialTheme.typography.bodyLarge)
    }
}
