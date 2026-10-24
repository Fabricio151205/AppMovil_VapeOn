package com.example.vapeon_movil.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.R
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.viewmodels.ProductoViewModel
import com.example.vapeon_movil.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminHomeScreen(
    irCatalogo: () -> Unit,
    productoViewModel: ProductoViewModel,
    irDetalleProducto: (Producto) -> Unit = {},
    irEditarProducto: (Producto) -> Unit = {},
    onPerfilClick: () -> Unit = {},
    onCajaTotalClick: () -> Unit = {},
    onPrediccionClick: () -> Unit = {},
    onCuponesClick: () -> Unit = {},
    onNuevaVentaClick: () -> Unit = {},
    onEstadisticasClick: () -> Unit = {},
    onInversionistasClick: () -> Unit = {},
    irPedidos: () -> Unit = {},
    irSabores: () -> Unit = {},
    cerrarSesion: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val productosFirebase = productoViewModel.productos
    val cargandoProductos = productoViewModel.cargando
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }
    LaunchedEffect(Unit) { productoViewModel.cargarProductos() }

    MenuScreen(
        irCatalogo = irCatalogo,
        esAdmin = true,
        irPedidos = irPedidos,
        irSabores = irSabores,
        cerrarSesion = cerrarSesion,
        onPerfilClick = onPerfilClick,
        onTituloClick = { /* Ya estamos en AdminHome */ }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            SnackbarHost(snackbarHostState)

            // ==========================================
            // 1. SECCIÓN: CAJA TOTAL
            // ==========================================
            Card(
                onClick = {
                    onCajaTotalClick()
                    scope.launch {
                        snackbarHostState.showSnackbar("Módulo de Caja Total no disponible")
                    }
                },
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = VapeOnRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAJA TOTAL:",
                        color = VapeOnTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Sin datos",
                        color = VapeOnTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 2. SECCIÓN: PREDICCIÓN DE INVENTARIO
            // ==========================================
            Card(
                onClick = {
                    onPrediccionClick()
                    scope.launch {
                        snackbarHostState.showSnackbar("Módulo de Predicción de Inventario no disponible")
                    }
                },
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = VapeOnRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text(
                            text = "Predicción:",
                            color = VapeOnTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "No disponible",
                            color = VapeOnTextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "Sin datos de predicción. Módulo no disponible.",
                        color = VapeOnTextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ==========================================
            // 3. SECCIÓN: PRODUCTOS (Carrusel horizontal)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Productos",
                    color = VapeOnGold,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Ver todos",
                    color = VapeOnTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .clickable { irCatalogo() }
                        .padding(4.dp)
                )
            }

            if (cargandoProductos) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = VapeOnGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Cargando productos...",
                            color = VapeOnTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (productoViewModel.error != null) {
                Text(productoViewModel.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = { productoViewModel.cargarProductos() }) { Text("Reintentar") }
            } else if (productosFirebase.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay productos registrados.",
                        color = VapeOnTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(productosFirebase) { prod ->
                        Card(
                            onClick = { irDetalleProducto(prod) },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                            modifier = Modifier.width(170.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(VapeOnProductImageBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.logo_vapeon),
                                        contentDescription = prod.nombre,
                                        modifier = Modifier.size(80.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = prod.nombre,
                                    color = VapeOnTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${prod.stock} Ud.",
                                    color = VapeOnTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { irEditarProducto(prod) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar",
                                            tint = VapeOnTextPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { productoAEliminar = prod },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = VapeOnTextPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // ==========================================
            // 4. SECCIÓN: MÁS ACCIONES (Grid de 4 botones rojos)
            // ==========================================
            Text(
                text = "Más Acciones",
                color = VapeOnGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = {
                        onCuponesClick()
                        scope.launch {
                            snackbarHostState.showSnackbar("Módulo de Cupones no disponible")
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Cupones",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CUPONES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = {
                        onNuevaVentaClick()
                        scope.launch {
                            snackbarHostState.showSnackbar("Módulo de Nueva Venta no disponible")
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Nueva Venta",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NUEVA VENTA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = {
                        onEstadisticasClick()
                        scope.launch {
                            snackbarHostState.showSnackbar("Módulo de Estadísticas no disponible")
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Estadísticas",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ESTADISTICAS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }

                Button(
                    onClick = {
                        onInversionistasClick()
                        scope.launch {
                            snackbarHostState.showSnackbar("Módulo de Inversionistas no disponible")
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Inversionistas",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "INVERSIONISTAS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    // Diálogo de confirmación para eliminar producto
    productoAEliminar?.let { prod ->
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = {
                Text(
                    text = "Eliminar Producto",
                    fontWeight = FontWeight.Bold,
                    color = VapeOnGold
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar '${prod.nombre}'? Esta acción no se puede deshacer.",
                    color = VapeOnTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        productoViewModel.eliminarProducto(prod.id) { productoAEliminar = null }
                    },
                    enabled = prod.id !in productoViewModel.eliminandoIds,
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                ) {
                    Text(if (prod.id in productoViewModel.eliminandoIds) "Eliminando…" else "Eliminar", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productoAEliminar = null }) {
                    Text("Cancelar", color = VapeOnTextSecondary)
                }
            },
            containerColor = VapeOnSurface
        )
    }
}
