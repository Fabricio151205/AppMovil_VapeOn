package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.CajaTotal
import com.example.vapeon_movil.entities.obtenerNumeroPedidoReal
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.moneda
import java.time.Instant

@Composable
fun CajaTotalScreen(
    caja: CajaTotal,
    marcas: List<String>,
    marcaSeleccionada: String,
    cargando: Boolean,
    error: String?,
    advertencias: Int,
    cargandoMarcas: Boolean,
    errorMarcas: String?,
    desde: String,
    hasta: String,
    errorRango: String?,
    cambiarDesde: (String) -> Unit,
    cambiarHasta: (String) -> Unit,
    aplicarRango: () -> Unit,
    restablecerRango: () -> Unit,
    seleccionarMarca: (String) -> Unit,
    verDetalleMarca: (String) -> Unit,
    verDetallePedido: (String) -> Unit,
    volver: () -> Unit
) {
    BackHandler(onBack = volver)

    val marcasVisibles = remember(marcas, caja.marcas) {
        (marcas + caja.marcas).map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    val ventasFiltradas = remember(caja, marcaSeleccionada) {
        caja.ventas(marcaSeleccionada)
            .sortedWith(compareByDescending<com.example.vapeon_movil.entities.VentaCaja> { Instant.parse(it.pedido.fecha) }.thenBy { it.pedido.id })
    }
    val reporteProductos = remember(caja, marcaSeleccionada) { caja.reporteProductos(marcaSeleccionada) }
    val reporteCategorias = remember(caja, marcaSeleccionada) { caja.reporteCategorias(marcaSeleccionada) }

    Surface(
        color = VapeOnBackground,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ENCABEZADO CON BOTÓN DE REGRESO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = volver) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver al panel",
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TÍTULO "CAJA TOTAL" Y MONTO ACUMULADO
            Text(
                text = "CAJA TOTAL",
                color = VapeOnTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = moneda(caja.total.toDouble()),
                color = VapeOnTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Rango de fechas", color = VapeOnGold, fontWeight = FontWeight.Bold)
                    Text("Formato: dd/mm/aaaa", color = VapeOnTextSecondary, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = desde,
                            onValueChange = cambiarDesde,
                            label = { Text("Desde") },
                            placeholder = { Text("dd/mm/aaaa") },
                            singleLine = true,
                            isError = errorRango != null,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = hasta,
                            onValueChange = cambiarHasta,
                            label = { Text("Hasta") },
                            placeholder = { Text("dd/mm/aaaa") },
                            singleLine = true,
                            isError = errorRango != null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    errorRango?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = restablecerRango, modifier = Modifier.weight(1f)) {
                            Text("Restablecer")
                        }
                        Button(onClick = aplicarRango, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)) {
                            Text("Aplicar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResumenCajaFila("Ingresos por productos", moneda(caja.productos.toDouble()))
                    ResumenCajaFila("Delivery", moneda(caja.delivery.toDouble()))
                    HorizontalDivider(color = VapeOnGold)
                    ResumenCajaFila("Total", moneda(caja.total.toDouble()), destacado = true)
                    ResumenCajaFila("Unidades vendidas", caja.unidadesVendidas.toString())
                }
            }

            if (advertencias > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "$advertencias pedido(s) con datos inválidos fueron excluidos del reporte.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // FILA DE CHIPS DE FILTRO DE MARCAS EN FORMATO PÍLDORA (TODOS, OXBAR, LIFEPOD, ETC.)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    val esTodosSeleccionado = marcaSeleccionada.isBlank()
                    Button(
                        onClick = { seleccionarMarca("") },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (esTodosSeleccionado) VapeOnRed else VapeOnRedDark,
                            contentColor = VapeOnTextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Todos",
                            fontWeight = if (esTodosSeleccionado) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }

                items(marcasVisibles, key = { it }) { marca ->
                    val esEstaMarca = marca.equals(marcaSeleccionada, ignoreCase = true)
                    Button(
                        onClick = { seleccionarMarca(marca) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (esEstaMarca) VapeOnRed else VapeOnRedDark,
                            contentColor = VapeOnTextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = marca,
                            fontWeight = if (esEstaMarca) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (cargando || cargandoMarcas) {
                Spacer(modifier = Modifier.height(20.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = VapeOnGold)
            }

            if (error != null || errorMarcas != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = error ?: errorMarcas.orEmpty(), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (marcaSeleccionada.isBlank()) "INGRESOS POR PRODUCTO"
                    else "INGRESOS POR PRODUCTO — $marcaSeleccionada",
                color = VapeOnTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (reporteProductos.isEmpty()) {
                Text("Sin productos vendidos en la selección.", color = VapeOnTextSecondary,
                    modifier = Modifier.fillMaxWidth())
            } else {
                reporteProductos.forEach { producto ->
                    ReporteCajaFila(producto.nombre, producto.marca, producto.unidades, producto.ingreso.toDouble())
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "INGRESOS POR CATEGORÍA",
                color = VapeOnTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (reporteCategorias.isEmpty()) {
                Text("Sin categorías vendidas en la selección.", color = VapeOnTextSecondary,
                    modifier = Modifier.fillMaxWidth())
            } else {
                reporteCategorias.forEach { categoria ->
                    ReporteCajaFila(categoria.categoria, null, categoria.unidades, categoria.ingreso.toDouble())
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TÍTULO DE LA SECCIÓN DE MARCA SELECCIONADA
            val tituloSeccion = marcaSeleccionada.ifBlank { "Todas las Ventas" }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = tituloSeccion,
                    color = VapeOnTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TARJETAS DE PEDIDOS ENTREGADOS (MÁS RECIENTE ARRIBA)
            if (ventasFiltradas.isEmpty() && !cargando) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin ventas entregadas registradas.",
                        color = VapeOnTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ventasFiltradas.forEach { venta ->
                        val numeroSecuencial = obtenerNumeroPedidoReal(venta.pedido.id, caja)
                        val textoPedido = String.format(java.util.Locale.ROOT, "Pedido N° %04d", numeroSecuencial)
                        val cantidadTotalOrden = venta.productos.sumOf { it.cantidad }

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { verDetallePedido(venta.pedido.id) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                // CABECERA REAL SECUENCIAL "Pedido N° 0001"
                                Text(
                                    text = textoPedido,
                                    color = VapeOnTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // PRODUCTOS E IMPORTES
                                venta.productos.forEach { linea ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = linea.nombre,
                                            color = VapeOnTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = moneda(linea.subtotal.toDouble()),
                                            color = VapeOnTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                HorizontalDivider(color = VapeOnGold, thickness = 2.dp)

                                Spacer(modifier = Modifier.height(8.dp))

                                // FILA INFERIOR: CANTIDAD Y "Ver detalle"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cant.: $cantidadTotalOrden",
                                        color = VapeOnTextSecondary,
                                        fontSize = 13.sp
                                    )

                                    Text(
                                        text = "Ver detalle",
                                        color = VapeOnTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.clickable { verDetallePedido(venta.pedido.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResumenCajaFila(etiqueta: String, valor: String, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, color = if (destacado) VapeOnTextPrimary else VapeOnTextSecondary,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal)
        Text(valor, color = if (destacado) VapeOnGold else VapeOnTextPrimary,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReporteCajaFila(
    titulo: String,
    detalle: String?,
    unidades: Int,
    ingreso: Double
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(titulo, color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                detalle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = VapeOnTextSecondary, fontSize = 12.sp)
                }
                Text("$unidades ${if (unidades == 1) "unidad" else "unidades"}",
                    color = VapeOnTextSecondary, fontSize = 13.sp)
            }
            Text(moneda(ingreso), color = VapeOnGold, fontWeight = FontWeight.Bold)
        }
    }
}
