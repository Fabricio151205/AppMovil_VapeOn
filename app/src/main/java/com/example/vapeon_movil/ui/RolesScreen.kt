package com.example.vapeon_movil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.RolesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolesScreen(
    adminActual: Usuario,
    rolesViewModel: RolesViewModel,
    volver: () -> Unit
) {
    var usuarioConfirmacion by remember { mutableStateOf<Usuario?>(null) }
    LaunchedEffect(adminActual.id) { rolesViewModel.cargarUsuarios(adminActual) }

    Scaffold(
        containerColor = VapeOnBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gestión de Roles",
                        color = VapeOnTextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = volver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al panel ADMIN",
                            tint = VapeOnTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "USUARIOS REGISTRADOS",
                color = VapeOnGold,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = rolesViewModel.busqueda,
                onValueChange = rolesViewModel::cambiarBusqueda,
                modifier = Modifier.fillMaxWidth().testTag("roles_busqueda"),
                placeholder = { Text("Buscar por nombre, apellido, correo o DNI") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar usuarios", tint = VapeOnGold)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VapeOnInputBackground,
                    unfocusedContainerColor = VapeOnInputBackground,
                    focusedBorderColor = VapeOnGold,
                    unfocusedBorderColor = VapeOnInputBorder,
                    focusedTextColor = VapeOnTextPrimary,
                    unfocusedTextColor = VapeOnTextPrimary,
                    focusedPlaceholderColor = VapeOnTextSecondary,
                    unfocusedPlaceholderColor = VapeOnTextSecondary,
                    cursorColor = VapeOnGold
                )
            )

            rolesViewModel.error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = VapeOnSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(it, color = VapeOnError, modifier = Modifier.testTag("roles_error"))
                        if (rolesViewModel.conflicto) {
                            OutlinedButton(onClick = { rolesViewModel.cargarUsuarios(adminActual) }) {
                                Text("Recargar usuarios")
                            }
                        }
                    }
                }
            }
            rolesViewModel.mensaje?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = VapeOnSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        it,
                        color = VapeOnGold,
                        modifier = Modifier.padding(12.dp).testTag("roles_exito"),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    rolesViewModel.cargando -> CircularProgressIndicator(
                        color = VapeOnGold,
                        modifier = Modifier.align(Alignment.Center).testTag("roles_cargando")
                    )
                    rolesViewModel.usuariosFiltrados.isEmpty() -> Card(
                        colors = CardDefaults.cardColors(containerColor = VapeOnSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    ) {
                        Text(
                            if (rolesViewModel.busqueda.isBlank()) "No hay usuarios registrados." else "Sin resultados.",
                            color = VapeOnTextSecondary,
                            modifier = Modifier.padding(20.dp).testTag("roles_vacio")
                        )
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("roles_lista"),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(rolesViewModel.usuariosPagina, key = { it.id }) { usuario ->
                            UsuarioRolCard(
                                usuario = usuario,
                                esAdminActual = usuario.id == adminActual.id,
                                actualizando = rolesViewModel.actualizandoRol == usuario.id,
                                cambiarRol = { usuarioConfirmacion = usuario }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VapeOnSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, VapeOnDivider, RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .testTag("roles_paginacion"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Página ${rolesViewModel.paginaActual + 1} de ${rolesViewModel.totalPaginas}",
                    color = VapeOnTextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BotonPagina("Primera", Modifier.weight(1f), rolesViewModel.paginaActual > 0, rolesViewModel::primeraPagina)
                    BotonPagina("Anterior", Modifier.weight(1f), rolesViewModel.paginaActual > 0, rolesViewModel::paginaAnterior)
                    BotonPagina("Siguiente", Modifier.weight(1f),
                        rolesViewModel.paginaActual + 1 < rolesViewModel.totalPaginas, rolesViewModel::paginaSiguiente)
                    BotonPagina("Última", Modifier.weight(1f),
                        rolesViewModel.paginaActual + 1 < rolesViewModel.totalPaginas, rolesViewModel::ultimaPagina)
                }
            }
        }
    }

    usuarioConfirmacion?.let { usuario ->
        val nuevoRol = if (usuario.rol.equals("ADMIN", true)) "CLIENTE" else "ADMIN"
        AlertDialog(
            onDismissRequest = { usuarioConfirmacion = null },
            containerColor = VapeOnSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Confirmar cambio de rol", color = VapeOnGold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Cambiar el rol de ${nombreCompleto(usuario)} de ${usuario.rol.uppercase()} a $nuevoRol?",
                    color = VapeOnTextPrimary
                )
            },
            dismissButton = { TextButton(onClick = { usuarioConfirmacion = null }) { Text("Cancelar") } },
            confirmButton = {
                Button(
                    onClick = {
                        usuarioConfirmacion = null
                        rolesViewModel.cambiarRol(adminActual, usuario.id, nuevoRol)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed),
                    shape = RoundedCornerShape(50)
                ) { Text("Confirmar") }
            }
        )
    }
}

@Composable
private fun UsuarioRolCard(
    usuario: Usuario,
    esAdminActual: Boolean,
    actualizando: Boolean,
    cambiarRol: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("rol_usuario_${usuario.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, VapeOnCardBorder)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(VapeOnRedDark, CircleShape)
                        .border(1.dp, VapeOnGoldVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        iniciales(usuario),
                        color = VapeOnGold,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        nombreCompleto(usuario),
                        color = VapeOnTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        usuario.correo.ifBlank { "Correo no registrado" },
                        color = VapeOnTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "DNI: ${usuario.dni?.takeIf { it.isNotBlank() } ?: "No registrado"}",
                        color = VapeOnTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(if (usuario.rol.equals("ADMIN", true)) VapeOnRedDark else VapeOnSurfaceVariant,
                            RoundedCornerShape(50))
                        .border(1.dp, if (usuario.rol.equals("ADMIN", true)) VapeOnRed else VapeOnCardBorder,
                            RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Rol: ${usuario.rol.uppercase()}",
                        color = if (usuario.rol.equals("ADMIN", true)) VapeOnTextPrimary else VapeOnGold,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = cambiarRol,
                    enabled = !actualizando && !(esAdminActual && usuario.rol.equals("ADMIN", true)),
                    modifier = Modifier.testTag("cambiar_rol_${usuario.id}"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary,
                        disabledContainerColor = VapeOnSelected,
                        disabledContentColor = VapeOnTextMuted
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (actualizando) CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = VapeOnTextPrimary,
                        strokeWidth = 2.dp
                    ) else Text("Cambiar rol", fontWeight = FontWeight.SemiBold)
                }
            }
            if (esAdminActual && usuario.rol.equals("ADMIN", true)) {
                Text(
                    "No puedes degradar tu propia cuenta.",
                    color = VapeOnTextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun BotonPagina(texto: String, modifier: Modifier, habilitado: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.height(42.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        Text(texto, maxLines = 1, style = MaterialTheme.typography.labelMedium)
    }
}

private fun iniciales(usuario: Usuario): String {
    val nombre = usuario.nombre.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    val apellido = usuario.apellido.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    return (nombre + apellido).ifBlank { "?" }
}

private fun nombreCompleto(usuario: Usuario): String =
    "${usuario.nombre.trim()} ${usuario.apellido.trim()}".trim().ifBlank { "Usuario sin nombre" }
