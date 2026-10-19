package com.example.vapeon_movil

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.ui.PedidosAdminScreen
import com.example.vapeon_movil.ui.AdminHomeScreen
import com.example.vapeon_movil.ui.AgregarProductoScreen
import com.example.vapeon_movil.ui.CheckoutScreen
import com.example.vapeon_movil.ui.CarritoScreen
import com.example.vapeon_movil.ui.CatalogoScreen
import com.example.vapeon_movil.ui.DetalleProductoScreen
import com.example.vapeon_movil.ui.EditarProductoScreen
import com.example.vapeon_movil.ui.ForgotPasswordScreen
import com.example.vapeon_movil.ui.HistorialComprasScreen
import com.example.vapeon_movil.ui.DetallePedidoScreen
import com.example.vapeon_movil.ui.HomeScreen
import com.example.vapeon_movil.ui.LoginScreen
import com.example.vapeon_movil.ui.RegisterScreen
import com.example.vapeon_movil.viewmodels.PedidoViewModel
import com.example.vapeon_movil.viewmodels.CarritoViewModel
import com.example.vapeon_movil.viewmodels.ProductoViewModel
import com.example.vapeon_movil.viewmodels.UsuarioViewModel

@Composable
fun VapeONApp() {

    val context = LocalContext.current

    // ── ViewModels con scope de Activity ──────────────────────────────────────
    // Al instanciarse aquí (nivel raíz), sobreviven rotaciones y se comparten
    // entre todas las pantallas sin necesidad de recrearse.
    val usuarioViewModel: UsuarioViewModel  = viewModel()
    val productoViewModel: ProductoViewModel = viewModel()
    val carritoViewModel: CarritoViewModel  = viewModel()

    val pedidoViewModel: PedidoViewModel = viewModel()

    val usuarioActual = usuarioViewModel.usuarioActual
    val claveSesion = claveSesionNavegacion(usuarioActual)

    // Solo ruta, identificadores y propietario; nunca objetos de negocio ni credenciales.
    var pantallaActual by rememberSaveable { mutableStateOf("login") }
    var productoSeleccionadoId by rememberSaveable { mutableStateOf("") }
    var productoEditarId by rememberSaveable { mutableStateOf("") }
    var pedidoSeleccionadoId by rememberSaveable { mutableStateOf("") }
    var propietarioNavegacion by rememberSaveable { mutableStateOf<String?>(null) }

    // Resultados de formularios observados por la composición vigente, incluso tras una rotación.
    LaunchedEffect(usuarioViewModel.registroCompletado) {
        if (usuarioViewModel.registroCompletado) {
            if (pantallaActual == "registro") pantallaActual = "login"
            usuarioViewModel.consumirRegistroCompletado()
        }
    }
    LaunchedEffect(productoViewModel.altaCompletada, productoViewModel.edicionCompletada) {
        if (productoViewModel.altaCompletada && pantallaActual == "agregarProducto") pantallaActual = "catalogo"
        if (productoViewModel.edicionCompletada && pantallaActual == "editarProducto")
            pantallaActual = if (usuarioActual?.rol.equals("ADMIN", true)) "admin" else "catalogo"
        productoViewModel.consumirResultadosGuardado()
    }

    LaunchedEffect(claveSesion) {
        pedidoViewModel.actualizarUsuarioCheckout(usuarioActual)
        carritoViewModel.cargarCarrito(context, usuarioActual?.id)
        pedidoViewModel.actualizarUsuarioHistorial(usuarioActual)
        pedidoViewModel.actualizarUsuarioAdmin(usuarioActual)
        productoViewModel.actualizarSesionProducto(claveSesion)
        if (propietarioNavegacion != claveSesion) {
            productoSeleccionadoId = ""
            productoEditarId = ""
            pedidoSeleccionadoId = ""
            propietarioNavegacion = claveSesion
            if (pantallaActual !in RUTAS_PUBLICAS) pantallaActual = pantallaInicialSesion(usuarioActual)
        }
    }

    LaunchedEffect(claveSesion, productoViewModel.productos, productoViewModel.inventarioCargado) {
        if (productoViewModel.inventarioCargado) carritoViewModel.actualizarInventario(productoViewModel.productos)
    }

    fun iniciarNavegacionSesion() {
        pedidoViewModel.actualizarUsuarioCheckout(usuarioViewModel.usuarioActual)
        // También descarta selecciones si el mismo usuario vuelve a iniciar sesión.
        propietarioNavegacion = claveSesionNavegacion(usuarioViewModel.usuarioActual)
        productoSeleccionadoId = ""
        productoEditarId = ""
        pedidoSeleccionadoId = ""
        productoViewModel.actualizarSesionProducto(null)
        productoViewModel.actualizarSesionProducto(propietarioNavegacion)
    }

    fun cerrarSesion() {
        usuarioViewModel.cerrarSesion()
        productoSeleccionadoId = ""
        productoEditarId = ""
        pedidoSeleccionadoId = ""
        propietarioNavegacion = null
        productoViewModel.actualizarSesionProducto(null)
        pedidoViewModel.actualizarUsuarioCheckout(null)
        pedidoViewModel.actualizarUsuarioHistorial(null)
        pedidoViewModel.actualizarUsuarioAdmin(null)
        // Cargar sin usuario vacía solo la memoria; conserva el carrito y el intento de A.
        carritoViewModel.cargarCarrito(context, null)
        pantallaActual = "login"
    }

    fun seleccionarProducto(producto: Producto, editar: Boolean = false) {
        val propietario = claveSesionNavegacion(usuarioViewModel.usuarioActual) ?: return
        productoViewModel.seleccionarProductoParaRuta(propietario, producto)
        if (editar) productoEditarId = producto.id else productoSeleccionadoId = producto.id
        pantallaActual = if (editar) "editarProducto" else "detalle"
    }

    // Derivado del ViewModel — evita duplicar estado
    val esAdminUser = usuarioActual?.rol?.equals("ADMIN", ignoreCase = true) == true

    // La guardia se aplica antes de componer una pantalla, no después de mostrarla.
    val pantallaSegura = pantallaRestauradaSegura(pantallaActual, usuarioActual, propietarioNavegacion)
    key(claveSesion) {
    when (pantallaSegura) {

        // ── LOGIN ─────────────────────────────────────────────────────────────
        "login" -> {
            LoginScreen(
                usuarioViewModel = usuarioViewModel,
                irRegistro  = { pantallaActual = "registro" },
                irRecuperar = { pantallaActual = "recuperar" },
                irHome      = {
                    iniciarNavegacionSesion()
                    pantallaActual = if (pedidoViewModel.requiereComprobacion) "checkout" else "home"
                },
                irAdmin     = { iniciarNavegacionSesion(); pantallaActual = "admin" }
            )
        }

        // ── REGISTRO ──────────────────────────────────────────────────────────
        "registro" -> {
            RegisterScreen(
                usuarioViewModel = usuarioViewModel,
                volverLogin = { if (pantallaActual == "registro") pantallaActual = "login" }
            )
        }

        // ── RECUPERAR CONTRASEÑA ──────────────────────────────────────────────
        "recuperar" -> {
            ForgotPasswordScreen(
                volverLogin = { pantallaActual = "login" }
            )
        }

        // ── HOME (CLIENTE) ────────────────────────────────────────────────────
        "home" -> {
            HomeScreen(
                cerrarSesion = ::cerrarSesion,
                productoViewModel = productoViewModel,
                irDetalleProducto = { producto ->
                    seleccionarProducto(producto)
                },
                irCarrito = { pantallaActual = "carrito" },
                irHistorial = { pantallaActual = "historialCompras" }
            )
        }

        // ── PANEL ADMIN ───────────────────────────────────────────────────────
        "admin" -> {
            AdminHomeScreen(
                productoViewModel = productoViewModel,
                cerrarSesion = ::cerrarSesion,
                irPedidos = { pantallaActual = "pedidosAdmin" },
                irCatalogo = { pantallaActual = "catalogo" },
                irEditarProducto = { producto ->
                    seleccionarProducto(producto, editar = true)
                },
                irDetalleProducto = { producto ->
                    seleccionarProducto(producto)
                }
            )
        }

        // ── CATÁLOGO ──────────────────────────────────────────────────────────
        "catalogo" -> {
            CatalogoScreen(
                cerrarSesion = ::cerrarSesion,
                productoViewModel = productoViewModel,
                irDetalleProducto = { producto ->
                    seleccionarProducto(producto)
                },
                esAdmin           = esAdminUser,
                irAgregarProducto = { pantallaActual = "agregarProducto" },
                irEditarProducto  = { producto ->
                    seleccionarProducto(producto, editar = true)
                },
                irAdmin   = { pantallaActual = "admin" },
                irPedidos = { pantallaActual = "pedidosAdmin" },
                irCarrito = { pantallaActual = "carrito" },
                irHistorial = { pantallaActual = "historialCompras" }
            )
        }

        // ── DETALLE PRODUCTO ──────────────────────────────────────────────────
        "detalle" -> {
            ProductoDeNavegacion(
                usuario = usuarioActual, id = productoSeleccionadoId, productoViewModel = productoViewModel,
                volver = { pantallaActual = if (esAdminUser) "catalogo" else "home" }
            ) { producto ->
                DetalleProductoScreen(
                    producto = producto,
                    esAdmin = esAdminUser,
                    volver = { pantallaActual = if (esAdminUser) "catalogo" else "home" },
                    irEditarProducto = { seleccionarProducto(it, editar = true) },
                    // Solo CLIENTE puede agregar al carrito (ADMIN no tiene acceso).
                    onAgregarAlCarrito = if (!esAdminUser) {
                        {
                            carritoViewModel.agregarProducto(producto)
                            pantallaActual = "carrito"
                        }
                    } else null
                )
            }
        }

        // ── CARRITO (solo CLIENTE) ────────────────────────────────────────────
        "carrito" -> {
            if (esAdminUser) {
                pantallaActual = "admin"
            } else {
                CarritoScreen(
                    carritoViewModel = carritoViewModel,
                    volver           = { pantallaActual = "home" },
                    irCheckout = {
                        if (!carritoViewModel.puedeComprar) return@CarritoScreen
                        pedidoViewModel.iniciarCheckout()
                        pantallaActual = "checkout"
                    }
                )
            }
        }

        "pedidosAdmin" -> {
            PedidosAdminScreen(
                usuario = usuarioViewModel.usuarioActual,
                pedidoViewModel = pedidoViewModel,
                volver = { pantallaActual = "admin" },
                irDetallePedido = { id ->
                    pedidoSeleccionadoId = id
                    pantallaActual = "detallePedidoAdmin"
                }
            )
        }

        "detallePedidoAdmin" -> {
            DetallePedidoScreen(
                usuario = usuarioViewModel.usuarioActual,
                pedidoId = pedidoSeleccionadoId,
                pedidoViewModel = pedidoViewModel,
                volver = { pantallaActual = "pedidosAdmin" }
            )
        }

        "historialCompras" -> {
            HistorialComprasScreen(
                usuario = usuarioViewModel.usuarioActual,
                pedidoViewModel = pedidoViewModel,
                volver = { pantallaActual = "home" },
                irCarrito = { pantallaActual = "carrito" },
                irHistorial = { pantallaActual = "historialCompras" },
                cerrarSesion = ::cerrarSesion,
                irDetallePedido = { id ->
                    pedidoSeleccionadoId = id
                    pantallaActual = "detallePedido"
                }
            )
        }

        "detallePedido" -> {
            DetallePedidoScreen(
                usuario = usuarioViewModel.usuarioActual,
                pedidoId = pedidoSeleccionadoId,
                pedidoViewModel = pedidoViewModel,
                volver = { pantallaActual = "historialCompras" }
            )
        }

        "checkout" -> {
            com.example.vapeon_movil.ui.theme.VapeON_MovilTheme {
            CheckoutScreen(
                usuario = usuarioViewModel.usuarioActual,
                carritoViewModel = carritoViewModel,
                productoViewModel = productoViewModel,
                pedidoViewModel = pedidoViewModel,
                volver = { pantallaActual = "carrito" },
                irHome = { pantallaActual = "home" }
            )
            }
        }

        // ── AGREGAR PRODUCTO (ADMIN) ───────────────────────────────────────────
        "agregarProducto" -> {
            AgregarProductoScreen(
                productoViewModel = productoViewModel,
                volver = { pantallaActual = "catalogo" }
            )
        }

        // ── EDITAR PRODUCTO (ADMIN) ────────────────────────────────────────────
        "editarProducto" -> {
            ProductoDeNavegacion(
                usuario = usuarioActual, id = productoEditarId, productoViewModel = productoViewModel,
                volver = { pantallaActual = "catalogo" }
            ) { producto ->
                EditarProductoScreen(
                    producto = producto,
                    productoViewModel = productoViewModel,
                    volver = { pantallaActual = if (esAdminUser) "admin" else "catalogo" }
                )
            }
        }
    }
    }
}

private val RUTAS_PUBLICAS = setOf("login", "registro", "recuperar")
private val RUTAS_CLIENTE = setOf("home", "carrito", "checkout", "historialCompras", "detallePedido")
private val RUTAS_ADMIN = setOf("admin", "agregarProducto", "editarProducto", "pedidosAdmin", "detallePedidoAdmin")
private val RUTAS_COMPARTIDAS = setOf("catalogo", "detalle")

internal fun claveSesionNavegacion(usuario: Usuario?): String? {
    if (usuario == null || usuario.id.isBlank()) return null
    val rol = usuario.rol.uppercase(java.util.Locale.ROOT)
    return if (rol == "ADMIN" || rol == "CLIENTE") "${usuario.id}:$rol" else null
}

private fun pantallaInicialSesion(usuario: Usuario?): String = when {
    claveSesionNavegacion(usuario) == null -> "login"
    usuario?.rol.equals("ADMIN", true) -> "admin"
    else -> "home"
}

/** Una ruta guardada no autentica al usuario ni conserva permisos por sí sola. */
internal fun pantallaRestauradaSegura(pantalla: String, usuario: Usuario?, propietario: String?): String {
    if (pantalla in RUTAS_PUBLICAS) return pantalla
    val claveActual = claveSesionNavegacion(usuario) ?: return "login"
    if (propietario != claveActual) return pantallaInicialSesion(usuario)
    val permitida = pantalla in RUTAS_COMPARTIDAS ||
        (usuario?.rol.equals("ADMIN", true) && pantalla in RUTAS_ADMIN) ||
        (usuario?.rol.equals("CLIENTE", true) && pantalla in RUTAS_CLIENTE)
    return if (permitida) pantalla else pantallaInicialSesion(usuario)
}

@Composable
private fun ProductoDeNavegacion(
    usuario: Usuario?, id: String, productoViewModel: ProductoViewModel,
    volver: () -> Unit, contenido: @Composable (Producto) -> Unit
) {
    val claveSesion = claveSesionNavegacion(usuario)
    LaunchedEffect(claveSesion, id) {
        if (claveSesion != null) productoViewModel.recuperarProductoParaRuta(claveSesion, id)
    }
    val producto = productoViewModel.productoParaRuta
    when {
        claveSesion == null || id.isBlank() -> SituacionProducto("No se pudo recuperar la selección de esta sesión.", volver)
        productoViewModel.propietarioProductoRuta == claveSesion && producto?.id == id -> contenido(producto)
        productoViewModel.errorProductoRuta != null && productoViewModel.propietarioProductoRuta == claveSesion ->
            SituacionProducto(productoViewModel.errorProductoRuta.orEmpty(), volver)
        else -> VapeON_MovilTheme {
            BackHandler(onBack = volver)
            Surface(color = VapeOnBackground, modifier = Modifier.fillMaxSize()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Recuperando producto…")
                    Button(onClick = volver) { Text("Volver") }
                }
            }
        }
    }
}

@Composable
private fun SituacionProducto(mensaje: String, volver: () -> Unit) {
    BackHandler(onBack = volver)
    VapeON_MovilTheme {
        Surface(color = VapeOnBackground, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(mensaje, color = MaterialTheme.colorScheme.error)
                Button(onClick = volver) { Text("Volver") }
            }
        }
    }
}
