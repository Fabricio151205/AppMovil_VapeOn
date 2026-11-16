package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.R
import com.example.vapeon_movil.components.ImagenSaborView
import com.example.vapeon_movil.entities.PopularidadProducto
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.entities.SaborItem
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.imagenProducto
import com.example.vapeon_movil.viewmodels.FavoritosAdminViewModel
import com.example.vapeon_movil.viewmodels.ProductoViewModel

/**
 * Ranking de productos más populares para el administrador.
 *
 * Se calcula con la cantidad de documentos de `favoritos` por producto. La
 * pantalla es de solo lectura y no muestra identificadores ni datos de los
 * clientes. Los productos que ya no existen en el catálogo se muestran con un
 * aviso neutro, sin inventar precio, stock ni imagen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritosAdminScreen(
    admin: Usuario?,
    productoViewModel: ProductoViewModel,
    sabores: List<SaborItem>,
    favoritosAdminViewModel: FavoritosAdminViewModel,
    volver: () -> Unit
) {
    val adminId = admin?.takeIf { it.rol.equals("ADMIN", true) }?.id.orEmpty()

    LaunchedEffect(adminId) {
        favoritosAdminViewModel.actualizarUsuario(admin)
        if (adminId.isNotBlank()) {
            favoritosAdminViewModel.cargarRanking(admin)
            if (productoViewModel.productos.isEmpty()) productoViewModel.cargarProductos()
        }
    }

    BackHandler(onBack = volver)

    Scaffold(
        containerColor = VapeOnBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Productos más populares",
                        color = VapeOnGold,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = volver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Volver al panel",
                            tint = VapeOnTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val productosPorId = remember(productoViewModel.productos) {
                productoViewModel.productos.associateBy { it.id }
            }
            val ranking = favoritosAdminViewModel.popularidad
            val cargando = favoritosAdminViewModel.cargando ||
                (ranking.isNotEmpty() && productoViewModel.cargando && productosPorId.isEmpty())

            when {
                adminId.isBlank() -> EstadoPopularidad(
                    icono = Icons.Outlined.FavoriteBorder,
                    titulo = "Inicia sesión como administrador",
                    detalle = "El ranking de favoritos solo está disponible para el rol ADMIN."
                )

                cargando && ranking.isEmpty() -> CargandoPopularidad()

                favoritosAdminViewModel.error != null && ranking.isEmpty() -> EstadoPopularidad(
                    icono = Icons.Outlined.FavoriteBorder,
                    titulo = "No se pudo cargar el ranking",
                    detalle = favoritosAdminViewModel.error.orEmpty(),
                    accion = "Reintentar",
                    onAccion = { favoritosAdminViewModel.cargarRanking(admin) }
                )

                ranking.isEmpty() -> EstadoPopularidad(
                    icono = Icons.Outlined.FavoriteBorder,
                    titulo = "Aún no hay productos favoritos",
                    detalle = "Cuando los clientes guarden productos, aparecerán aquí ordenados por popularidad."
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        EncabezadoRanking(
                            totalFavoritos = ranking.sumOf { it.favoritos },
                            totalProductos = ranking.size,
                            invalidos = favoritosAdminViewModel.documentosInvalidos
                        )
                    }

                    itemsIndexed(ranking, key = { _, item -> item.productoId }) { indice, item ->
                        FilaPopularidad(
                            posicion = indice + 1,
                            popularidad = item,
                            producto = productosPorId[item.productoId],
                            sabores = sabores
                        )
                    }

                    item {
                        Text(
                            text = "El ranking usa únicamente el conteo de favoritos por producto. " +
                                "No se muestran datos personales de los clientes.",
                            color = VapeOnTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EncabezadoRanking(
    totalFavoritos: Int,
    totalProductos: Int,
    invalidos: Int
) {
    Column {
        Text(
            text = "Ranking por favoritos",
            color = VapeOnTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Productos ordenados por la cantidad de clientes que los guardaron.",
            color = VapeOnTextSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = VapeOnRed),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "FAVORITOS REGISTRADOS",
                        color = VapeOnTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = totalFavoritos.toString(),
                        color = VapeOnTextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = VapeOnRed),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "PRODUCTOS EN EL RANKING",
                        color = VapeOnTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = totalProductos.toString(),
                        color = VapeOnTextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        if (invalidos > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Se omitieron $invalidos registros inválidos del ranking.",
                color = VapeOnError,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun FilaPopularidad(
    posicion: Int,
    popularidad: PopularidadProducto,
    producto: Producto?,
    sabores: List<SaborItem>
) {
    val imagen = remember(producto, sabores) {
        producto?.let { imagenProducto(it, sabores) }
    }
    val stock = producto?.stockTotal ?: 0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (posicion <= 3) VapeOnGold else VapeOnSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$posicion",
                    color = if (posicion <= 3) VapeOnBackground else VapeOnTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VapeOnProductImageBg),
                contentAlignment = Alignment.Center
            ) {
                when {
                    producto != null && !imagen.isNullOrBlank() -> ImagenSaborView(
                        dataUri = imagen,
                        contentDescription = producto.nombre.ifEmpty { "Producto VapeON" },
                        contentScale = ContentScale.Crop,
                        maxDimensionPx = 256,
                        modifier = Modifier.fillMaxSize()
                    )

                    producto != null -> Image(
                        painter = painterResource(id = R.drawable.logo_vapeon),
                        contentDescription = producto.nombre.ifEmpty { "Producto VapeON" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    else -> Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = VapeOnTextMuted,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (producto != null) {
                    Text(
                        text = producto.nombre.ifBlank { "Producto sin nombre" },
                        color = VapeOnTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = producto.marca.ifBlank { "Sin marca" },
                        color = VapeOnTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (stock > 0) "Stock: $stock u." else "Sin stock",
                        color = when {
                            stock == 0 -> VapeOnRed
                            stock <= 5 -> VapeOnGold
                            else -> VapeOnTextSecondary
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Producto no disponible",
                        color = VapeOnTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ya no forma parte del catálogo vigente.",
                        color = VapeOnTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = VapeOnRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = popularidad.favoritos.toString(),
                        color = VapeOnTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = if (popularidad.favoritos == 1) "favorito" else "favoritos",
                    color = VapeOnTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun CargandoPopularidad() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = VapeOnGold, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Calculando popularidad...",
                color = VapeOnTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun EstadoPopularidad(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    detalle: String,
    accion: String? = null,
    onAccion: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(VapeOnSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = VapeOnGold,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = titulo,
                color = VapeOnTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = detalle,
                color = VapeOnTextSecondary,
                fontSize = 13.sp
            )
            if (accion != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAccion,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary
                    )
                ) {
                    Text(accion, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
