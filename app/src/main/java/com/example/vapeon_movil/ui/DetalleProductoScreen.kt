package com.example.vapeon_movil.ui

import com.example.vapeon_movil.utils.moneda
import com.example.vapeon_movil.utils.obtenerSaboresPorMarca

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.R
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.SaboresViewModel

@Composable
fun DetalleProductoScreen(
    producto: Producto,
    esAdmin: Boolean = false,
    saboresViewModel: SaboresViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    volver: () -> Unit,
    irEditarProducto: ((Producto) -> Unit)? = null,
    onAgregarAlCarrito: (() -> Unit)? = null   // null para ADMIN (no aplica)
) {
    LaunchedEffect(Unit) { saboresViewModel.cargarSabores() }
    var esFavorito by remember { mutableStateOf(false) }

    // Sabores dinámicos por marca o guardados
    val saboresLista = remember(producto.sabor, producto.marca, saboresViewModel.listaSabores) {
        val guardados = producto.sabor.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (guardados.isNotEmpty()) guardados else obtenerSaboresPorMarca(producto.marca, saboresViewModel.listaSabores)
    }
    var saborSeleccionado by remember(saboresLista) { mutableStateOf(saboresLista.firstOrNull().orEmpty()) }

    val stockSaborSeleccionado = remember(saborSeleccionado, producto.stockPorSabor, producto.stockTotal) {
        if (producto.stockPorSabor.isNotEmpty()) {
            producto.stockPorSabor.entries.find { it.key.equals(saborSeleccionado, ignoreCase = true) }?.value ?: 0
        } else {
            producto.stockTotal
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = VapeOnBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // ==========================================
            // BARRA SUPERIOR CUSTOMIZADA
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Flecha para volver
                IconButton(onClick = { volver() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Título VAPEON
                Text(
                    text = "VAPEON",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = VapeOnGold,
                    letterSpacing = 1.sp
                )

                // Icono de Perfil de Usuario
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VapeOnRedDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Perfil",
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // TARJETA DE IMAGEN Y FAVORITO
            // ==========================================
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = VapeOnProductImageBg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Logo de VapeOn como imagen del producto por defecto
                    Image(
                        painter = painterResource(id = R.drawable.logo_vapeon),
                        contentDescription = producto.nombre.ifEmpty { "Producto VapeON" },
                        modifier = Modifier
                            .size(180.dp)
                            .padding(12.dp)
                    )

                    // Botón de Corazón (Favoritos) interactivo
                    IconButton(
                        onClick = {}, enabled = false,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favoritos no disponible",
                            tint = VapeOnTextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // FILA: MARCA Y STOCK
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Marca
                Text(
                    text = producto.marca.ifBlank { "No disponible" },
                    color = VapeOnTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                // Stock
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = VapeOnTextPrimary)) {
                            append("Stock: ")
                        }
                        withStyle(style = SpanStyle(color = if (stockSaborSeleccionado < 5 && stockSaborSeleccionado > 0) VapeOnRed else VapeOnGold, fontWeight = FontWeight.Bold)) {
                            append(if (stockSaborSeleccionado > 0) "$stockSaborSeleccionado unidades" else "Sin stock")
                        }
                    },
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // NOMBRE DEL PRODUCTO
            // ==========================================
            Text(
                text = producto.nombre.ifBlank { "No disponible" },
                color = VapeOnTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // DESCRIPCIÓN DEL PRODUCTO
            // ==========================================
            Text(
                text = producto.descripcion.ifBlank { "No disponible" },
                color = VapeOnTextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // TARJETA DE PRECIO Y ACCIÓN (CARRITO / EDITAR)
            // ==========================================
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = VapeOnCardBackground
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Precio Unitario",
                            color = VapeOnTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (producto.precio > 0) moneda(producto.precio) else "No disponible",
                            color = VapeOnTextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Botón según Rol (CLIENTE vs ADMIN)
                    if (esAdmin) {
                        Button(
                            onClick = { irEditarProducto?.invoke(producto) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VapeOnGold,
                                contentColor = VapeOnBackground
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Editar Producto",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = { onAgregarAlCarrito?.invoke() },
                            enabled = stockSaborSeleccionado > 0 && producto.precio.isFinite() && producto.precio > 0 && onAgregarAlCarrito != null,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VapeOnRed,
                                contentColor = VapeOnTextPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = if (stockSaborSeleccionado > 0) "Agregar al carrito" else "Sabor sin stock",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // SECTOR DE SELECCIÓN DE SABOR
            // ==========================================
            if (saboresLista.isNotEmpty()) {
                Text(
                    text = "Seleccionar Sabor",
                    color = VapeOnTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    saboresLista.chunked(2).forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            fila.forEach { opcionSabor ->
                                val estaSeleccionado = saborSeleccionado.equals(opcionSabor, ignoreCase = true)
                                SaborPillButton(
                                    sabor = opcionSabor,
                                    estaSeleccionado = estaSeleccionado,
                                    onClick = { saborSeleccionado = opcionSabor },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (fila.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SaborPillButton(
    sabor: String,
    estaSeleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (estaSeleccionado) VapeOnRed else VapeOnRedDark,
            contentColor = VapeOnTextPrimary
        ),
        modifier = modifier.height(52.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (estaSeleccionado) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = VapeOnTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = sabor,
                fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}
