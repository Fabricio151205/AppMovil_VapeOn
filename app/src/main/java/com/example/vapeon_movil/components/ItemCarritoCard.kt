package com.example.vapeon_movil.components

import com.example.vapeon_movil.utils.moneda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.example.vapeon_movil.R
import com.example.vapeon_movil.entities.ItemCarrito
import com.example.vapeon_movil.ui.theme.*

/**
 * Tarjeta de un ítem dentro del carrito de compras.
 *
 * Muestra imagen, nombre, precio unitario, controles de cantidad (− / +)
 * y botón de eliminación (×). Mínimo táctil 48dp en todos los controles.
 *
 * @param item         Ítem del carrito a representar.
 * @param onIncrementar  Callback al pulsar (+).
 * @param onDecrementar  Callback al pulsar (−). Si cantidad llega a 0 el ViewModel lo elimina.
 * @param onEliminar   Callback al pulsar (×) para quitar el ítem completamente.
 */
@Composable
fun ItemCarritoCard(
    item: ItemCarrito,
    onIncrementar: () -> Unit,
    onDecrementar: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Imagen del producto ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VapeOnSurface),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = item.imagenRes ?: R.drawable.logo_vapeon),
                    contentDescription = "Imagen de ${item.nombre}",
                    modifier = Modifier
                        .size(56.dp)
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ── Info y controles ─────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                // Nombre del producto
                Text(
                    text = item.nombre,
                    color = VapeOnTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Precio unitario
                Text(
                    text = "${moneda(item.precioUnit)} c/u.",
                    color = VapeOnGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Controles de cantidad: (−) cantidad (+)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botón decrementar
                    FilledTonalIconButton(
                        onClick = onDecrementar,
                        enabled = item.cantidad > 1,
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = VapeOnRed,
                            contentColor = VapeOnTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "−",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = VapeOnTextPrimary
                        )
                    }

                    // Cantidad actual
                    Text(
                        text = "%02d".format(item.cantidad),
                        color = VapeOnTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    // Botón incrementar
                    FilledTonalIconButton(
                        onClick = onIncrementar,
                        enabled = item.stockDisponible?.let { item.cantidad < it } == true,
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = VapeOnRed,
                            contentColor = VapeOnTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)

                    ) {
                        Text(
                            text = "+",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = VapeOnTextPrimary
                        )
                    }
                }
            }

            // ── Botón eliminar (×) ───────────────────────────────────────────
            IconButton(
                onClick = onEliminar,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Top)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Eliminar ${item.nombre} del carrito",
                    tint = VapeOnTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
