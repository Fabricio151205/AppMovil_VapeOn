package com.example.vapeon_movil.ui

import com.example.vapeon_movil.utils.moneda

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@Composable
fun DetalleProductoScreen(
    producto: Producto,
    esAdmin: Boolean = false,
    volver: () -> Unit,
    irEditarProducto: ((Producto) -> Unit)? = null,
    onAgregarAlCarrito: (() -> Unit)? = null   // null para ADMIN (no aplica)
) {
<<<<<<< HEAD
    var esFavorito by remember { mutableStateOf(false) }

    var totaldescuento by remember { mutableStateOf(0.00) }

    // Lista de sabores por defecto mostrada en la imagen
    val listaSabores = remember {
        listOf("Strawberry Watermelon", "Blue Razz Ice", "Miami Mint", "Peach Ice")
    }
    var saborSeleccionado by remember { mutableStateOf(listaSabores.first()) }
=======
>>>>>>> b242f1cce132296831da28c76db35981fb9e9c10

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
                        withStyle(style = SpanStyle(color = VapeOnGold, fontWeight = FontWeight.Bold)) {
                            append(if (producto.stock > 0) "${producto.stock} unidades" else "Sin stock")
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
                            enabled = producto.stock > 0 && producto.precio.isFinite() && producto.precio > 0 && onAgregarAlCarrito != null,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VapeOnRed,
                                contentColor = VapeOnTextPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Agregar al carrito",
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
            Text(
                text = "Seleccionar Sabor",
                color = VapeOnTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Sabores: No disponible", color = VapeOnTextSecondary)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
