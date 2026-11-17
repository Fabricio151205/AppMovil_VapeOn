package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.ClientesViewModel

@Composable
fun ClientesScreen(
    admin: Usuario?,
    viewModel: ClientesViewModel,
    volver: () -> Unit,
    verDetalle: (String) -> Unit
) {
    BackHandler(onBack = volver)
    LaunchedEffect(admin?.id, admin?.rol) { viewModel.cargarClientes(admin) }
    ClientesContenido(
        clientes = viewModel.clientesFiltrados,
        busqueda = viewModel.busqueda,
        cargando = viewModel.cargando,
        error = viewModel.error,
        volver = volver,
        cambiarBusqueda = viewModel::cambiarBusqueda,
        reintentar = { viewModel.cargarClientes(admin, forzar = true) },
        verDetalle = { id -> if (viewModel.seleccionarCliente(admin, id)) verDetalle(id) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClientesContenido(
    clientes: List<Usuario>,
    busqueda: String,
    cargando: Boolean,
    error: String?,
    volver: () -> Unit,
    cambiarBusqueda: (String) -> Unit,
    reintentar: () -> Unit,
    verDetalle: (String) -> Unit
) {
    Scaffold(
        containerColor = VapeOnBackground,
        topBar = {
            TopAppBar(
                title = { Text("CLIENTES", color = VapeOnGold, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = volver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = VapeOnTextPrimary)
                } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = cambiarBusqueda,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar cliente...") },
                leadingIcon = { Icon(Icons.Default.Search, "Buscar") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VapeOnGold, focusedLabelColor = VapeOnGold,
                    focusedTextColor = VapeOnTextPrimary, unfocusedTextColor = VapeOnTextPrimary
                )
            )
            Spacer(Modifier.height(14.dp))
            when {
                cargando && clientes.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VapeOnGold)
                }
                error != null && clientes.isEmpty() -> Column(
                    Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = reintentar, colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)) {
                        Text("Reintentar")
                    }
                }
                clientes.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (busqueda.isBlank()) "No hay clientes registrados." else "No se encontraron clientes.",
                        color = VapeOnTextSecondary)
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(clientes, key = { it.id }) { cliente -> ClienteCard(cliente) { verDetalle(cliente.id) } }
                }
            }
        }
    }
}

@Composable
private fun ClienteCard(cliente: Usuario, abrir: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = abrir),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = CircleShape, color = VapeOnGold, modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(inicialesCliente(cliente), color = VapeOnBackground, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(nombreCompletoCliente(cliente), color = VapeOnTextPrimary,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(cliente.correo, color = VapeOnTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("DNI: ${cliente.dni?.takeIf { it.isNotBlank() } ?: "No registrado"}", color = VapeOnTextSecondary)
                Text("Teléfono: ${cliente.telefono}", color = VapeOnTextSecondary)
                Text("CLIENTE", color = VapeOnGold, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

internal fun nombreCompletoCliente(cliente: Usuario): String =
    "${cliente.nombre} ${cliente.apellido}".trim().ifBlank { "Cliente sin nombre" }

private fun inicialesCliente(cliente: Usuario): String = listOf(cliente.nombre, cliente.apellido)
    .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }.joinToString("").take(2).ifBlank { "CL" }
