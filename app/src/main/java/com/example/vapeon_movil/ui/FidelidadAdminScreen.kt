package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.*
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.FidelidadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FidelidadAdminScreen(
    admin: Usuario,
    fidelidadViewModel: FidelidadViewModel,
    productos: List<Producto>,
    cargarProductos: () -> Unit,
    volver: () -> Unit
) {
    var productoId by rememberSaveable { mutableStateOf("") }
    var selectorAbierto by remember { mutableStateOf(false) }
    var clienteEntrega by remember { mutableStateOf<ClienteFidelidad?>(null) }
    LaunchedEffect(admin.id) {
        cargarProductos()
        fidelidadViewModel.cargarAdmin(admin)
    }
    BackHandler(onBack = volver)
    val producto = productos.firstOrNull { it.id == productoId }

    Surface(Modifier.fillMaxSize(), color = VapeOnBackground) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = volver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = VapeOnTextPrimary)
                }
                Text("Cupones de fidelidad", color = VapeOnGold, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text("Producto recompensa", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            val actual = fidelidadViewModel.configuracion
                            Text(
                                actual?.let { "${it.nombre} · ${it.marca}" } ?: "No configurado",
                                color = if (actual == null) VapeOnTextSecondary else VapeOnGold
                            )
                            Spacer(Modifier.height(14.dp))
                            ExposedDropdownMenuBox(
                                expanded = selectorAbierto,
                                onExpandedChange = { selectorAbierto = !selectorAbierto }
                            ) {
                                OutlinedTextField(
                                    value = producto?.let { "${it.nombre} · ${it.marca}" }.orEmpty(),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Seleccionar producto") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(selectorAbierto) },
                                    modifier = Modifier.fillMaxWidth()
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = VapeOnTextPrimary,
                                        unfocusedTextColor = VapeOnTextPrimary
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = selectorAbierto,
                                    onDismissRequest = { selectorAbierto = false }
                                ) {
                                    productos.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text("${item.nombre} · ${item.marca}") },
                                            onClick = { productoId = item.id; selectorAbierto = false }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { producto?.let { fidelidadViewModel.configurarRecompensa(admin, it) } },
                                enabled = producto != null && !fidelidadViewModel.guardando,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                            ) { Text("Configurar recompensa", fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                fidelidadViewModel.error?.let { mensaje ->
                    item { Text(mensaje, color = MaterialTheme.colorScheme.error) }
                }
                fidelidadViewModel.mensaje?.let { mensaje ->
                    item { Text(mensaje, color = VapeOnGold) }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Clientes", color = VapeOnGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { fidelidadViewModel.cargarAdmin(admin) }) { Text("Recargar") }
                    }
                }
                when {
                    fidelidadViewModel.cargando -> item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = VapeOnGold)
                        }
                    }
                    fidelidadViewModel.clientes.isEmpty() -> item {
                        Text("No hay clientes registrados.", color = VapeOnTextSecondary)
                    }
                    else -> items(fidelidadViewModel.clientes, key = { it.usuario.id }) { cliente ->
                        TarjetaClienteFidelidad(
                            cliente,
                            habilitado = !fidelidadViewModel.guardando,
                            entregar = { clienteEntrega = cliente }
                        )
                    }
                }
            }
        }
    }

    clienteEntrega?.let { cliente ->
        AlertDialog(
            onDismissRequest = { clienteEntrega = null },
            title = { Text("Entregar recompensa") },
            text = {
                Text(
                    "Usuario: ${cliente.usuario.nombre} ${cliente.usuario.apellido}\n" +
                        "Producto: ${cliente.recompensa?.nombreProducto ?: fidelidadViewModel.configuracion?.nombre ?: "No configurado"}\n" +
                        "Ciclo: ${cliente.fidelidad.numeroCiclo}"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        fidelidadViewModel.entregarRecompensa(admin, cliente.usuario.id)
                        clienteEntrega = null
                    },
                    enabled = cliente.recompensa?.productoId != null || fidelidadViewModel.configuracion != null
                ) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { clienteEntrega = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaClienteFidelidad(
    cliente: ClienteFidelidad,
    habilitado: Boolean,
    entregar: () -> Unit
) {
    val puntos = cliente.fidelidad.puntosActuales
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "${cliente.usuario.nombre} ${cliente.usuario.apellido}".trim(),
                color = VapeOnTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(cliente.usuario.correo, color = VapeOnTextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text("$puntos / $MAX_PUNTOS_FIDELIDAD puntos", color = VapeOnGold, fontWeight = FontWeight.Bold)
            Text(
                when {
                    puntos == MAX_PUNTOS_FIDELIDAD -> "RECOMPENSA PENDIENTE"
                    MAX_PUNTOS_FIDELIDAD - puntos == 1 -> "Falta 1 compra"
                    else -> "Faltan ${MAX_PUNTOS_FIDELIDAD - puntos} compras"
                },
                color = VapeOnTextSecondary
            )
            if (puntos == MAX_PUNTOS_FIDELIDAD) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = entregar,
                    enabled = habilitado,
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                ) {
                    Icon(Icons.Default.CardGiftcard, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Entregar recompensa")
                }
            }
        }
    }
}
