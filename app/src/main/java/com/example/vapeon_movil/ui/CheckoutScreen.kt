package com.example.vapeon_movil.ui

import com.example.vapeon_movil.utils.moneda
import com.example.vapeon_movil.utils.importe

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.*
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    usuario: Usuario?, carritoViewModel: CarritoViewModel,
    productoViewModel: ProductoViewModel, pedidoViewModel: PedidoViewModel,
    volver: () -> Unit, irHome: () -> Unit
) {
    var direccion by rememberSaveable { mutableStateOf("") }
    var montoTexto by rememberSaveable { mutableStateOf("") }
    var efectivoSeleccionado by rememberSaveable { mutableStateOf(false) }
    val bloqueado = pedidoViewModel.cargando || pedidoViewModel.requiereComprobacion
    BackHandler(enabled = bloqueado) { /* Esperar o comprobar el resultado antes de salir. */ }
    val confirmado = pedidoViewModel.pedidoConfirmado
    val subtotal = runCatching { Pedido.subtotal(carritoViewModel.items) }.getOrNull()
    val delivery = BigDecimal.valueOf(carritoViewModel.resumen.delivery).setScale(2)
    val total = subtotal?.plus(delivery)
    val monto = Pedido.leerMonto(montoTexto)
    val vuelto = if (monto != null && total != null && monto >= total) monto - total else null

    var menuAbierto by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = VapeOnBackground,
        topBar = {
            TopAppBar(
                title = { Text("VAPEON", color = VapeOnGold,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Box {
                        IconButton(onClick = { menuAbierto = true }, enabled = !bloqueado && confirmado == null) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menú", tint = VapeOnTextPrimary)
                        }
                        DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                            DropdownMenuItem(text = { Text("Volver al carrito") }, onClick = {
                                menuAbierto = false
                                volver()
                            })
                        }
                    }
                },
                actions = {
                    Surface(shape = CircleShape, color = VapeOnRedDark,
                        modifier = Modifier.padding(end = 16.dp).size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person,
                                contentDescription = "Cliente: ${usuario?.nombre.orEmpty()}", tint = VapeOnTextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (confirmado != null) {
<<<<<<< HEAD
                Text("¡Pedido registrado correctamente!", style = MaterialTheme.typography.titleLarge,
                    color = VapeOnGold)
                Text("Pedido: ${confirmado.id}", color = VapeOnTextPrimary)
                Text("Estado: ${confirmado.estado}", color = VapeOnTextPrimary)
                Text("Total: ${moneda(confirmado.total)}", color = VapeOnTextPrimary)
                Text("Pagarás con ${moneda(confirmado.montoPagado)} al recibir tu pedido. " +
                    "Vuelto: ${moneda(confirmado.vuelto)}", color = VapeOnTextPrimary)
                if (pedidoViewModel.requiereComprobacion) {
                    pedidoViewModel.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = {
                        pedidoViewModel.confirmar(usuario, carritoViewModel, productoViewModel, direccion, montoTexto)
                    }, enabled = !pedidoViewModel.cargando) { Text("Reintentar limpieza local") }
                    if (pedidoViewModel.cargando) CircularProgressIndicator(color = VapeOnGold)
                }
                Button(onClick = irHome, enabled = !pedidoViewModel.requiereComprobacion,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Volver al catálogo")
=======
                // Título de la pantalla
                Text(
                    text = "¡Pedido Registrado\nCorrectamente!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = VapeOnTextPrimary,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tarjeta de Resumen del Pedido
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = VapeOnCardBackground
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Pedido N° 1",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = VapeOnTextPrimary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fecha
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Fecha:",
                                color = VapeOnTextSecondary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = fechaPedidoTexto(confirmado.fecha),
                                color = VapeOnTextPrimary,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Estado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Estado:",
                                color = VapeOnTextSecondary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = confirmado.estado,
                                color = VapeOnTextPrimary,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Productos
                        Text(
                            text = "Productos:",
                            color = VapeOnTextSecondary,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        confirmado.productos.forEach { prod ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = prod.nombre,
                                    color = VapeOnTextPrimary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "S/. ${"%.2f".format(prod.subtotal.toDouble())}",
                                    color = VapeOnTextPrimary,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // IGV
                        val igvCalculado = confirmado.subtotal.toDouble() * 0.18
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "IGV",
                                color = VapeOnTextSecondary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "S/. ${"%.2f".format(igvCalculado)}",
                                color = VapeOnTextPrimary,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total:",
                                color = VapeOnGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "S/. ${"%.2f".format(confirmado.total.toDouble())}",
                                color = VapeOnGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Pagaras
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pagaras:",
                                color = VapeOnTextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "S/.${"%.2f".format(confirmado.montoPagado.toDouble())}",
                                color = VapeOnTextPrimary,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Vuelto
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Vuelto",
                                color = VapeOnTextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "S/.${"%.2f".format(confirmado.vuelto.toDouble())}",
                                color = VapeOnTextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botón principal: VOLVER AL CATALOGO
                Button(
                    onClick = irHome,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "VOLVER AL CATALOGO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
>>>>>>> 7f26e4fe0a697ffcada63b69861397e4c31c81d5
                }
            } else {
                Text("¿Cómo desea pagar?", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = VapeOnTextPrimary)
                Spacer(Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = if (efectivoSeleccionado) BorderStroke(1.dp, VapeOnGold) else null,
                    colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                                .selectable(selected = efectivoSeleccionado, enabled = !bloqueado,
                                    role = Role.RadioButton, onClick = { efectivoSeleccionado = true }),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("EFECTIVO", fontWeight = FontWeight.Bold, color = VapeOnTextPrimary)
                                Text("Pago con entrega", style = MaterialTheme.typography.bodyMedium,
                                    color = VapeOnTextPrimary.copy(alpha = 0.85f))
                            }
                            RadioButton(selected = efectivoSeleccionado, onClick = null, enabled = !bloqueado,
                                colors = RadioButtonDefaults.colors(selectedColor = VapeOnGold,
                                    unselectedColor = VapeOnTextPrimary))
                        }
                        HorizontalDivider(color = VapeOnGold.copy(alpha = 0.65f))
                        OutlinedTextField(value = montoTexto, onValueChange = { montoTexto = it },
                            enabled = !bloqueado, label = { Text("¿Con cuanto va a pagar?") },
                            prefix = { Text("S/") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = VapeOnTextPrimary, unfocusedTextColor = VapeOnTextPrimary,
                                focusedBorderColor = VapeOnGold, unfocusedBorderColor = VapeOnInputBorder,
                                focusedLabelColor = VapeOnGold, unfocusedLabelColor = VapeOnTextPrimary),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    }
                }
                val errorMonto = when {
                    montoTexto.isBlank() -> null
                    monto == null -> "Ingresa un monto válido con máximo dos decimales."
                    total != null && monto < total -> "El monto ingresado es insuficiente."
                    else -> null
                }
                errorMonto?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Vuelto: S/ ${vuelto?.let { importe(it) } ?: "—"}", color = VapeOnGold,
                    style = MaterialTheme.typography.titleMedium)
                OpcionPagoNoDisponible("YAPE QR", "Escanea y paga al instante")
                OpcionPagoNoDisponible("Agregar Tarjeta", "Crédito o Débito")
                Text("Datos de entrega", style = MaterialTheme.typography.titleMedium,
                    color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                Text("Cliente: ${usuario?.nombre.orEmpty()} ${usuario?.apellido.orEmpty()}\n" +
                    "Teléfono: ${usuario?.telefono.orEmpty()}", color = VapeOnTextPrimary)
                OutlinedTextField(value = direccion, onValueChange = { direccion = it },
                    enabled = !bloqueado, label = { Text("Dirección de entrega") }, modifier = Modifier.fillMaxWidth())
                Text("Productos de tu compra", style = MaterialTheme.typography.titleMedium,
                    color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                carritoViewModel.items.forEach { item ->
                    Text("${item.nombre} · Cantidad: ${item.cantidad}\n" +
                        "Precio unitario: S/ ${importe(item.precioUnit)} · " +
                        "Subtotal: S/ ${importe(BigDecimal.valueOf(item.precioUnit).multiply(BigDecimal.valueOf(item.cantidad.toLong())))}",
                        style = MaterialTheme.typography.bodyMedium, color = VapeOnTextPrimary)
                }
                pedidoViewModel.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (subtotal == null) Text("El carrito está vacío o contiene datos inválidos.",
                    color = MaterialTheme.colorScheme.error)
                Card(shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = VapeOnRedDark)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Resumen del Pago", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        FilaPago("Sub Total", subtotal?.let { importe(it) } ?: "—")
                        FilaPago("Delivery", importe(delivery))
                        FilaPago("IGV (18%)", importe(carritoViewModel.resumen.igv))
                        HorizontalDivider(thickness = 2.dp, color = VapeOnGold)
                        FilaPago("TOTAL A PAGAR", total?.let { importe(it) } ?: "—", destacado = true)
                        if (pedidoViewModel.cargando) CircularProgressIndicator(
                            color = VapeOnGold, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Button(onClick = {
                            pedidoViewModel.confirmar(usuario, carritoViewModel, productoViewModel, direccion, montoTexto)
                        }, enabled = !pedidoViewModel.cargando && (pedidoViewModel.requiereComprobacion ||
                            (efectivoSeleccionado && direccion.isNotBlank() && vuelto != null)),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed,
                                contentColor = VapeOnTextPrimary, disabledContainerColor = VapeOnSurfaceVariant,
                                disabledContentColor = VapeOnTextSecondary),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (pedidoViewModel.requiereComprobacion) "Comprobar pedido" else "PAGAR",
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun OpcionPagoNoDisponible(titulo: String, subtitulo: String) {
    Card(shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(titulo, color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                Text(subtitulo, color = VapeOnTextPrimary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium)
                Text("No disponible", color = VapeOnGold, style = MaterialTheme.typography.labelMedium)
            }
            Surface(shape = CircleShape, color = VapeOnRedDark, modifier = Modifier.size(16.dp)) {}
        }
    }
}

@Composable
private fun FilaPago(titulo: String, valor: String, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, modifier = Modifier.weight(1f),
            color = VapeOnTextPrimary.copy(alpha = if (destacado) 1f else 0.85f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.width(12.dp))
        Text("S/ $valor", color = VapeOnTextPrimary, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal)
    }
}
