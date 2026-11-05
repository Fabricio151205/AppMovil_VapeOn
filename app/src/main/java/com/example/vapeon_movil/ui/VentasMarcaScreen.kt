package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.CajaTotal
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.moneda

@Composable
fun VentasMarcaScreen(
    caja: CajaTotal,
    marca: String,
    cargando: Boolean,
    error: String?,
    recargar: () -> Unit,
    verDetalle: (String) -> Unit,
    volver: () -> Unit
) {
    BackHandler(onBack = volver)

    val ventas = caja.ventas(marca)
    val tituloPantalla = if (marca.isNotBlank()) "Detalle Venta $marca" else "Detalle Ventas General"

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
            // ENCABEZADO CON FLECHA Y TÍTULO "Detalle Venta [Marca]"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = volver) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver a Caja Total",
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tituloPantalla,
                    color = VapeOnTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (cargando) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = VapeOnGold)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (error != null) {
                Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (ventas.isEmpty() && !cargando) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se registraron ventas para $marca.",
                        color = VapeOnTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ventas.forEachIndexed { index, venta ->
                        val numeroPedidoVisual = index + 1
                        val cantidadTotalOrden = venta.productos.sumOf { it.cantidad }

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { verDetalle(venta.pedido.id) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                // TÍTULO "Pedido N° X"
                                Text(
                                    text = "Pedido N° $numeroPedidoVisual",
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
                                        modifier = Modifier.clickable { verDetalle(venta.pedido.id) }
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
