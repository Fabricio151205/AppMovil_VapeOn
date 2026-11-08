package com.example.vapeon_movil.ui

import androidx.compose.foundation.Canvas
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.R
import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.ui.theme.VapeOnBackground
import com.example.vapeon_movil.ui.theme.VapeOnCardBorder
import com.example.vapeon_movil.ui.theme.VapeOnCardNotSelected
import com.example.vapeon_movil.ui.theme.VapeOnError
import com.example.vapeon_movil.ui.theme.VapeOnGold
import com.example.vapeon_movil.ui.theme.VapeOnRed
import com.example.vapeon_movil.ui.theme.VapeOnRedDark
import com.example.vapeon_movil.ui.theme.VapeOnSurfaceVariant
import com.example.vapeon_movil.ui.theme.VapeOnTextPrimary
import com.example.vapeon_movil.ui.theme.VapeOnTextSecondary
import com.example.vapeon_movil.utils.moneda

/**
 * Boleta única para todos los métodos de pago.
 *
 * Usa exactamente los mismos datos del pedido real: productos, cantidades,
 * precios, delivery, total, monto pagado y vuelto. Solo cambia la nota final
 * según se trate de efectivo o de un pago digital simulado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmacionPedidoScreen(
    pedido: Pedido,
    numeroPedido: Int,
    requiereComprobacion: Boolean,
    cargando: Boolean,
    error: String?,
    reintentarLimpieza: () -> Unit,
    volverAlCatalogo: () -> Unit
) {
    val bloqueado = requiereComprobacion || cargando
    BackHandler { if (!bloqueado) volverAlCatalogo() }
    Scaffold(
        containerColor = VapeOnBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "VAPEON",
                        color = VapeOnGold,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = volverAlCatalogo, enabled = !bloqueado) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Volver al catálogo",
                            tint = VapeOnTextPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp, start = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(VapeOnRedDark)
                            .clickable(enabled = !bloqueado) { volverAlCatalogo() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Volver al catálogo",
                            tint = VapeOnTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = VapeOnBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¡Pedido Registrado\nCorrectamente!",
                color = VapeOnTextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            BoletaPedido(pedido = pedido, numeroPedido = numeroPedido)

            Spacer(modifier = Modifier.height(18.dp))

            if (requiereComprobacion) {
                error?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = VapeOnError,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
                OutlinedButton(
                    onClick = reintentarLimpieza,
                    enabled = !cargando,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("REINTENTAR LIMPIEZA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (cargando) {
                CircularProgressIndicator(color = VapeOnGold, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = volverAlCatalogo,
                enabled = !requiereComprobacion && !cargando,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VapeOnRed,
                    contentColor = VapeOnTextPrimary,
                    disabledContainerColor = VapeOnRed.copy(alpha = 0.45f),
                    disabledContentColor = VapeOnTextPrimary.copy(alpha = 0.7f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "VOLVER AL CATÁLOGO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Boleta con estética de comprobante: cabecera, tabla de productos y totales. */
@Composable
private fun BoletaPedido(pedido: Pedido, numeroPedido: Int) {
    val digital = pedido.pagoId != null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.arreglo_pedido),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
            Text(
                text = "PEDIDO N° $numeroPedido",
                color = VapeOnTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }

        LineaPunteada(color = VapeOnCardBorder)

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            FilaBoleta("Fecha:", fechaPedidoTexto(pedido.fecha))
            Spacer(modifier = Modifier.height(6.dp))
            FilaBoleta("Estado:", pedido.estado)

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = VapeOnGold.copy(alpha = 0.45f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Productos",
                    modifier = Modifier.weight(5f),
                    color = VapeOnTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cant.",
                    modifier = Modifier.weight(1.2f),
                    color = VapeOnTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Precio",
                    modifier = Modifier.weight(2f),
                    color = VapeOnTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = VapeOnCardBorder, thickness = 1.dp)

            pedido.productos.forEach { producto ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(5f)) {
                        Text(
                            text = producto.nombre,
                            color = VapeOnTextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        val detalle = listOfNotNull(
                            producto.marca.takeIf { it.isNotBlank() },
                            (producto.sabor ?: producto.categoria)?.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        if (detalle.isNotBlank()) {
                            Text(
                                text = detalle,
                                color = VapeOnTextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    Text(
                        text = producto.cantidad.toString(),
                        modifier = Modifier.weight(1.2f),
                        color = VapeOnTextPrimary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = moneda(producto.precioUnitario),
                        modifier = Modifier.weight(2f),
                        color = VapeOnTextPrimary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            FilaBoleta("Delivery", moneda(pedido.delivery))

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = VapeOnGold, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Total:",
                    color = VapeOnGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = moneda(pedido.total),
                    color = VapeOnGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            FilaBoleta(if (digital) "Pagado:" else "Pagarás:", moneda(pedido.montoPagado))
            Spacer(modifier = Modifier.height(6.dp))
            FilaBoleta("Vuelto:", moneda(pedido.vuelto))

            Spacer(modifier = Modifier.height(12.dp))
            LineaPunteada(color = VapeOnCardBorder)
            Spacer(modifier = Modifier.height(10.dp))

            if (digital) {
                NotaBoleta("Método de pago: ${pedido.metodoPago}")
                NotaBoleta("Estado: ${pedido.estadoPago ?: pedido.estado}")
                if (pedido.modalidadPago == "SIMULADO") {
                    NotaBoleta("No se realizó ningún cobro real.")
                }
            } else {
                NotaBoleta("Método de pago: EFECTIVO")
                NotaBoleta("Recibirás tu vuelto cuando el motorizado confirme tu entrega.")
            }
            }
        }
    }
}

@Composable
private fun FilaBoleta(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = etiqueta, color = VapeOnTextSecondary, fontSize = 12.sp)
        Text(text = valor, color = VapeOnTextPrimary, fontSize = 12.sp)
    }
}

@Composable
private fun NotaBoleta(texto: String) {
    Text(
        text = texto,
        color = VapeOnTextSecondary,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    )
}

/** Línea discontinua que separa las secciones del comprobante. */
@Composable
private fun LineaPunteada(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                0f
            )
        )
    }
}
