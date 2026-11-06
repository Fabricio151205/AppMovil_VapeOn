package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.ProductoViewModel

@Composable
fun SugerenciasReabastecimientoScreen(
    productoViewModel: ProductoViewModel,
    volver: () -> Unit,
    irEditarProducto: (Producto) -> Unit
) {
    BackHandler(onBack = volver)

    val productos = productoViewModel.productos
    val cargando = productoViewModel.cargando

    // Agrupar sugerencias de compra por marca
    val sugerenciasPorMarca = productos
        .filter { it.stockTotal <= 5 }
        .groupBy { it.marca }

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ENCABEZADO CON FLECHA Y TÍTULO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = volver) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sugerencias de Compra",
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

            if (sugerenciasPorMarca.isEmpty() && !cargando) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Todas las marcas cuentan con stock óptimo para la demanda.",
                        color = VapeOnTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    sugerenciasPorMarca.forEach { (marcaNombre, listaProds) ->
                        item {
                            Text(
                                text = "Marca: $marcaNombre (${listaProds.size} requeridos)",
                                color = VapeOnGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(listaProds, key = { it.id }) { producto ->
                            val sugerenciaCantidad = (15 - producto.stockTotal).coerceAtLeast(10)

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { irEditarProducto(producto) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp)
                                ) {
                                    Text(
                                        text = producto.nombre,
                                        color = VapeOnTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Stock Actual: ${producto.stockTotal} u. · Pedido Sugerido: +$sugerenciaCantidad u.",
                                        color = VapeOnTextSecondary,
                                        fontSize = 13.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    HorizontalDivider(color = VapeOnGold, thickness = 2.dp)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Gestionar inventario",
                                        color = VapeOnGold,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
