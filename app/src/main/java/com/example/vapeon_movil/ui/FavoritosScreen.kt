package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.R
import com.example.vapeon_movil.components.ImagenSaborView
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.entities.SaborItem
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.imagenProducto
import com.example.vapeon_movil.utils.moneda
import com.example.vapeon_movil.viewmodels.CarritoViewModel
import com.example.vapeon_movil.viewmodels.FavoritosViewModel
import com.example.vapeon_movil.viewmodels.ProductoViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla "Mis favoritos" del cliente.
 *
 * Solo muestra la relación guardada en `favoritos`; los datos de cada tarjeta
 * se resuelven contra el catálogo vigente para no conservar precios ni stock
 * desactualizados.
 */
@Composable
fun FavoritosScreen(
    usuario: Usuario?,
    productoViewModel: ProductoViewModel,
    sabores: List<SaborItem>,
    favoritosViewModel: FavoritosViewModel,
    carritoViewModel: CarritoViewModel,
    volver: () -> Unit,
    irCatalogo: () -> Unit = {},
    irCarrito: () -> Unit = {},
    irHistorial: () -> Unit = {},
    irFavoritos: () -> Unit = {},
    irTarjetas: () -> Unit = {},
    irDirecciones: () -> Unit = {},
    irFidelidad: () -> Unit = {},
    irDetalleProducto: (Producto) -> Unit = {},
    cerrarSesion: () -> Unit = {},
    onPerfilClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val usuarioId = usuario?.takeIf { !it.rol.equals("ADMIN", ignoreCase = true) }?.id.orEmpty()

    LaunchedEffect(usuarioId) {
        favoritosViewModel.consumirMensaje()
        favoritosViewModel.actualizarUsuario(usuario)
        if (usuarioId.isNotBlank()) {
            favoritosViewModel.cargarFavoritos(usuario)
            productoViewModel.cargarProductos()
        }
    }

    LaunchedEffect(favoritosViewModel.mensaje) {
        favoritosViewModel.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            favoritosViewModel.consumirMensaje()
        }
    }

    LaunchedEffect(favoritosViewModel.error, favoritosViewModel.favoritos.size) {
        val error = favoritosViewModel.error
        if (error != null && favoritosViewModel.favoritos.isNotEmpty()) {
            snackbarHostState.showSnackbar(error)
            favoritosViewModel.limpiarError()
        }
    }

    BackHandler(onBack = volver)

    MenuScreen(
        irCatalogo = irCatalogo,
        irCarrito = irCarrito,
        irHistorial = irHistorial,
        irFavoritos = irFavoritos,
        irTarjetas = irTarjetas,
        irDirecciones = irDirecciones,
        irFidelidad = irFidelidad,
        cerrarSesion = cerrarSesion,
        onPerfilClick = onPerfilClick,
        onTituloClick = { /* Título */ }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val productosPorId = remember(productoViewModel.productos) {
                productoViewModel.productos.associateBy { it.id }
            }
            val favoritosVisibles = favoritosViewModel.favoritos.mapNotNull { favorito ->
                productosPorId[favorito.productoId]?.let { favorito to it }
            }
            val cargando = favoritosViewModel.cargando ||
                (favoritosViewModel.favoritos.isNotEmpty() && productoViewModel.cargando &&
                    favoritosVisibles.isEmpty())

            when {
                usuarioId.isBlank() -> EstadoFavoritos(
                    icono = Icons.Outlined.FavoriteBorder,
                    titulo = "Inicia sesión como cliente",
                    detalle = "Los favoritos se guardan en tu cuenta."
                )

                cargando && favoritosVisibles.isEmpty() -> CargandoFavoritos()

                favoritosViewModel.error != null && favoritosViewModel.favoritos.isEmpty() -> EstadoFavoritos(
                    icono = Icons.Outlined.FavoriteBorder,
                    titulo = "No se pudieron cargar tus favoritos",
                    detalle = favoritosViewModel.error.orEmpty(),
                    accion = "Reintentar",
                    onAccion = { favoritosViewModel.cargarFavoritos(usuario) }
                )

                favoritosVisibles.isEmpty() -> EstadoFavoritos(
                    icono = Icons.Outlined.FavoriteBorder,
                    titulo = if (favoritosViewModel.favoritos.isEmpty()) {
                        "Aún no tienes favoritos"
                    } else {
                        "Tus productos favoritos ya no están disponibles"
                    },
                    detalle = if (favoritosViewModel.favoritos.isEmpty()) {
                        "Marca el corazón de un producto para guardarlo aquí."
                    } else {
                        "Los productos guardados ya no forman parte del catálogo."
                    },
                    accion = "Explorar catálogo",
                    onAccion = irCatalogo
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Text(
                                text = "Tus Favoritos",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = VapeOnTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Productos que guardaste para comprar después",
                                fontSize = 14.sp,
                                color = VapeOnTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    items(favoritosVisibles, key = { it.first.id }) { (favorito, producto) ->
                        TarjetaFavorito(
                            producto = producto,
                            sabores = sabores,
                            guardando = favoritosViewModel.operandoProductoId == producto.id,
                            onAbrir = { irDetalleProducto(producto) },
                            onQuitar = {
                                favoritosViewModel.eliminarFavorito(usuario, favorito)
                            },
                            onAgregar = {
                                carritoViewModel.consumirAviso()
                                carritoViewModel.agregarProducto(
                                    producto,
                                    saborParaAgregar(producto),
                                    context
                                )
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        carritoViewModel.aviso ?: "Producto agregado al carrito."
                                    )
                                }
                            }
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

/** Primer sabor con stock disponible; null si el producto no usa sabores. */
private fun saborParaAgregar(producto: Producto): String? {
    if (producto.stockPorSabor.isNotEmpty()) {
        return producto.stockPorSabor.entries.firstOrNull { it.value > 0 }?.key
    }
    return producto.sabor.split(',').map { it.trim() }.firstOrNull { it.isNotBlank() }
}

@Composable
private fun TarjetaFavorito(
    producto: Producto,
    sabores: List<SaborItem>,
    guardando: Boolean,
    onAbrir: () -> Unit,
    onQuitar: () -> Unit,
    onAgregar: () -> Unit
) {
    val imagen = remember(producto, sabores) { imagenProducto(producto, sabores) }
    val stock = producto.stockTotal
    val sabor = remember(producto) { saborParaAgregar(producto) }
    val usaSabores = producto.stockPorSabor.isNotEmpty() ||
        producto.sabor.split(',').any { it.isNotBlank() }
    val disponible = stock > 0 && producto.precio.isFinite() && producto.precio > 0 &&
        (!usaSabores || sabor != null)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(VapeOnProductImageBg)
                    .clickable(onClick = onAbrir)
            ) {
                if (!imagen.isNullOrBlank()) {
                    ImagenSaborView(
                        dataUri = imagen,
                        contentDescription = producto.nombre.ifEmpty { "Producto VapeON" },
                        contentScale = ContentScale.Crop,
                        maxDimensionPx = 512,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.logo_vapeon),
                        contentDescription = producto.nombre.ifEmpty { "Producto VapeON" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(
                    onClick = onQuitar,
                    enabled = !guardando,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(VapeOnBackground.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Quitar de favoritos",
                        tint = VapeOnRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Stock: ${if (stock > 0) "$stock unidades" else "Sin stock"}",
                    color = if (stock in 1..4) VapeOnRed else VapeOnGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = producto.marca.ifBlank { "No disponible" },
                    color = VapeOnTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = producto.nombre.ifBlank { "No disponible" },
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAbrir)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (producto.precio > 0) moneda(producto.precio) else "No disponible",
                    color = VapeOnTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAgregar,
                    enabled = disponible && !guardando,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text(
                        text = "Añadir al Carrito",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CargandoFavoritos() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = VapeOnGold, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("Cargando favoritos...", color = VapeOnTextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EstadoFavoritos(
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
