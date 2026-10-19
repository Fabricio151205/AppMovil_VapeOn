package com.example.vapeon_movil.ui

import com.example.vapeon_movil.utils.moneda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.components.ItemCarritoCard
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.CarritoViewModel

/**
 * Pantalla del Carrito de Compras — exclusiva para usuarios con rol CLIENTE.
 *
 * Muestra:
 *  - Lista scrollable de ítems con controles de cantidad y eliminación.
 *  - Panel inferior fijo "Resumen del Pago" con SubTotal, Delivery, IGV y Total.
 *  - Botón "PAGAR" para abrir el checkout de efectivo contra entrega.
 *
 * @param carritoViewModel  ViewModel con el estado del carrito.
 * @param volver            Callback para regresar a la pantalla anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarritoScreen(
    carritoViewModel: CarritoViewModel,
    volver: () -> Unit,
    irCheckout: () -> Unit = {}
) {
    val items   = carritoViewModel.items
    val resumen = carritoViewModel.resumen

    Scaffold(
        containerColor = VapeOnBackground,
        topBar = {
            // ── Top App Bar ─────────────────────────────────────────────────
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = VapeOnTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Carrito",
                            color = VapeOnTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = volver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = VapeOnTextPrimary
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VapeOnRedDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil no disponible",
                            tint = VapeOnTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VapeOnBackground
                )
            )
        },
        bottomBar = {
            // ── Panel inferior: Resumen del Pago ────────────────────────────
            ResumenPagoPanel(
                subTotal = resumen.subTotal,
                delivery = resumen.delivery,
                igv      = resumen.igv,
                total    = resumen.total,
                hayItems = carritoViewModel.puedeComprar,
                irCheckout = irCheckout
            )
        }
    ) { paddingValues ->

        if (items.isEmpty()) {
            // ── Estado vacío ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = VapeOnTextSecondary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tu carrito está vacío",
                        color = VapeOnTextSecondary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    carritoViewModel.aviso?.let { Text(it, color = VapeOnGold, modifier = Modifier.padding(16.dp)) }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Agrega productos desde el catálogo",
                        color = VapeOnTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // ── Lista de ítems ────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                carritoViewModel.aviso?.let { aviso -> item { Text(aviso, color = VapeOnGold) } }
                items(items, key = { it.productoId }) { item ->
                    ItemCarritoCard(
                        item         = item,
                        onIncrementar = { carritoViewModel.incrementar(item.productoId) },
                        onDecrementar = { carritoViewModel.decrementar(item.productoId) },
                        onEliminar   = { carritoViewModel.eliminarItem(item.productoId) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Panel de Resumen del Pago (Bottom Bar)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ResumenPagoPanel(
    subTotal: Double,
    delivery: Double,
    igv: Double,
    total: Double,
    hayItems: Boolean,
    irCheckout: () -> Unit
) {
    Surface(
        color = VapeOnSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Línea decorativa dorada centrada
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Surface(
                    color = VapeOnGold,
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Resumen del Pago",
                color = VapeOnTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Fila: Sub Total
            FilaResumen(
                label  = "Sub Total",
                valor  = moneda(subTotal),
                negrita = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fila: Delivery
            FilaResumen(
                label  = "Delivery",
                valor  = moneda(delivery),
                negrita = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fila: IGV
            FilaResumen(
                label  = "IGV (18%)",
                valor  = moneda(igv),
                negrita = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = VapeOnDivider, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Fila: Total
            FilaResumen(
                label   = "TOTAL A PAGAR",
                valor   = moneda(total),
                negrita = true,
                colorLabel = VapeOnTextPrimary,
                colorValor = VapeOnTextPrimary
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Botón PAGAR
            Button(
                onClick = irCheckout,
                enabled = hayItems,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VapeOnRed,
                    contentColor   = VapeOnTextPrimary,
                    disabledContainerColor = VapeOnRedDark,
                    disabledContentColor   = VapeOnTextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text       = "PAGAR",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun FilaResumen(
    label: String,
    valor: String,
    negrita: Boolean,
    colorLabel: androidx.compose.ui.graphics.Color = VapeOnTextSecondary,
    colorValor: androidx.compose.ui.graphics.Color = VapeOnTextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = label,
            color      = colorLabel,
            fontWeight = if (negrita) FontWeight.Bold else FontWeight.Normal,
            fontSize   = if (negrita) 15.sp else 14.sp
        )
        Text(
            text       = valor,
            color      = colorValor,
            fontWeight = if (negrita) FontWeight.Bold else FontWeight.Normal,
            fontSize   = if (negrita) 15.sp else 14.sp
        )
    }
}
