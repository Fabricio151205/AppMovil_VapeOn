package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Proveedor
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.ProveedorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProveedoresScreen(
    usuario: Usuario?,
    proveedorViewModel: ProveedorViewModel,
    volver: () -> Unit,
    registrar: () -> Unit
) {
    BackHandler(onBack = volver)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(usuario?.id, usuario?.rol) { proveedorViewModel.cargarProveedores(usuario) }
    LaunchedEffect(proveedorViewModel.mensajeExito) {
        proveedorViewModel.mensajeExito?.let {
            snackbar.showSnackbar(it)
            proveedorViewModel.consumirMensajeExito()
        }
    }

    Scaffold(
        containerColor = VapeOnBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("PROVEEDORES", color = VapeOnGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = volver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = VapeOnTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
            )
        }
    ) { padding ->
        when {
            proveedorViewModel.cargando && proveedorViewModel.proveedores.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = VapeOnGold) }

            proveedorViewModel.error != null && proveedorViewModel.proveedores.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(proveedorViewModel.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { proveedorViewModel.cargarProveedores(usuario) },
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                ) { Text("Reintentar") }
                Spacer(Modifier.height(12.dp))
                BotonRegistrarProveedor(registrar)
            }

            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (proveedorViewModel.proveedores.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillParentMaxHeight(0.75f).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("No hay proveedores registrados.", color = VapeOnTextSecondary)
                            Spacer(Modifier.height(18.dp))
                            BotonRegistrarProveedor(registrar)
                        }
                    }
                } else {
                    item { BotonRegistrarProveedor(registrar) }
                    items(proveedorViewModel.proveedores, key = { it.id }) { ProveedorCard(it) }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BotonRegistrarProveedor(registrar: () -> Unit) {
    Button(
        onClick = registrar,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed, contentColor = VapeOnTextPrimary)
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Text("  REGISTRAR PROVEEDOR", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProveedorCard(proveedor: Proveedor) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                proveedor.razonSocial, color = VapeOnGold, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            DatoProveedor("RUC", proveedor.ruc)
            Spacer(Modifier.height(4.dp))
            DatoProveedor("Contacto", proveedor.contacto)
            DatoProveedor("Teléfono", proveedor.telefono)
            DatoProveedor("Correo", proveedor.correo)
            DatoProveedor("Ciudad", proveedor.ciudad)
            DatoProveedor("Tipo de producto", proveedor.tipoProducto)
            if (proveedor.observaciones.isNotBlank()) DatoProveedor("Observaciones", proveedor.observaciones)
        }
    }
}

@Composable
private fun DatoProveedor(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("$etiqueta: ", color = VapeOnTextSecondary, fontWeight = FontWeight.SemiBold)
        Text(valor, color = VapeOnTextPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}
