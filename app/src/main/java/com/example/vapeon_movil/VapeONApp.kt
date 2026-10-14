package com.example.vapeon_movil

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.ui.AdminHomeScreen
import com.example.vapeon_movil.ui.AgregarProductoScreen
import com.example.vapeon_movil.ui.CheckoutScreen
import com.example.vapeon_movil.ui.CarritoScreen
import com.example.vapeon_movil.ui.CatalogoScreen
import com.example.vapeon_movil.ui.DetalleProductoScreen
import com.example.vapeon_movil.ui.EditarProductoScreen
import com.example.vapeon_movil.ui.ForgotPasswordScreen
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

    LaunchedEffect(usuarioViewModel.usuarioActual?.id) {
        carritoViewModel.cargarCarrito(context, usuarioViewModel.usuarioActual?.id)
    }

    // ── Estado de navegación ──────────────────────────────────────────────────
    var pantallaActual       by remember { mutableStateOf("login") }
    var productoSeleccionado by remember { mutableStateOf<Producto?>(null) }
    var productoEditar       by remember { mutableStateOf<Producto?>(null) }

    // Derivado del ViewModel — evita duplicar estado
    val esAdminUser = usuarioViewModel.esAdmin

    when (pantallaActual) {

        // ── LOGIN ─────────────────────────────────────────────────────────────
        "login" -> {
            LoginScreen(
                usuarioViewModel = usuarioViewModel,
                irRegistro  = { pantallaActual = "registro" },
                irRecuperar = { pantallaActual = "recuperar" },
                irHome      = {
                    pantallaActual = if (pedidoViewModel.requiereComprobacion) "checkout" else "home"
                },
                irAdmin     = { pantallaActual = "admin" }
            )
        }

        // ── REGISTRO ──────────────────────────────────────────────────────────
        "registro" -> {
            RegisterScreen(
                volverLogin = { pantallaActual = "login" }
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
                productoViewModel = productoViewModel,
                irDetalleProducto = { producto ->
                    productoSeleccionado = producto
                    pantallaActual = "detalle"
                },
                irCarrito = { pantallaActual = "carrito" }
            )
        }

        // ── PANEL ADMIN ───────────────────────────────────────────────────────
        "admin" -> {
            AdminHomeScreen(
                irCatalogo = { pantallaActual = "catalogo" },
                irEditarProducto = { producto ->
                    productoEditar = producto
                    pantallaActual = "editarProducto"
                },
                irDetalleProducto = { producto ->
                    productoSeleccionado = producto
                    pantallaActual = "detalle"
                }
            )
        }

        // ── CATÁLOGO ──────────────────────────────────────────────────────────
        "catalogo" -> {
            CatalogoScreen(
                productoViewModel = productoViewModel,
                irDetalleProducto = { producto ->
                    productoSeleccionado = producto
                    pantallaActual = "detalle"
                },
                esAdmin           = esAdminUser,
                irAgregarProducto = { pantallaActual = "agregarProducto" },
                irEditarProducto  = { producto ->
                    productoEditar = producto
                    pantallaActual = "editarProducto"
                },
                irAdmin   = { pantallaActual = "admin" },
                irCarrito = { pantallaActual = "carrito" }
            )
        }

        // ── DETALLE PRODUCTO ──────────────────────────────────────────────────
        "detalle" -> {
            DetalleProductoScreen(
                producto         = productoSeleccionado!!,
                esAdmin          = esAdminUser,
                volver           = {
                    pantallaActual = if (esAdminUser) "catalogo" else "home"
                },
                irEditarProducto = { producto ->
                    productoEditar = producto
                    pantallaActual = "editarProducto"
                },
                // Solo CLIENTE puede agregar al carrito (ADMIN no tiene acceso)
                onAgregarAlCarrito = if (!esAdminUser) {
                    {
                        carritoViewModel.agregarProducto(productoSeleccionado!!)
                        pantallaActual = "carrito"
                    }
                } else null
            )
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
                        pedidoViewModel.iniciarCheckout()
                        pantallaActual = "checkout"
                    }
                )
            }
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
                volver = { pantallaActual = "catalogo" }
            )
        }

        // ── EDITAR PRODUCTO (ADMIN) ────────────────────────────────────────────
        "editarProducto" -> {
            EditarProductoScreen(
                producto = productoEditar!!,
                volver   = {
                    pantallaActual = if (esAdminUser) "admin" else "catalogo"
                }
            )
        }
    }
}
