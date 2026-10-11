package com.example.vapeon_movil

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.ui.AdminHomeScreen
import com.example.vapeon_movil.ui.AgregarProductoScreen
import com.example.vapeon_movil.ui.CatalogoScreen
import com.example.vapeon_movil.ui.DetalleProductoScreen
import com.example.vapeon_movil.ui.EditarProductoScreen
import com.example.vapeon_movil.ui.ForgotPasswordScreen
import com.example.vapeon_movil.ui.HomeScreen
import com.example.vapeon_movil.ui.LoginScreen
import com.example.vapeon_movil.ui.RegisterScreen
import com.example.vapeon_movil.viewmodels.ProductoViewModel
import com.example.vapeon_movil.viewmodels.UsuarioViewModel

@Composable
fun VapeONApp() {

    // ── ViewModels con scope de Activity ──────────────────────────────────────
    // Al instanciarse aquí (nivel raíz), sobreviven rotaciones y se comparten
    // entre todas las pantallas sin necesidad de recrearse.
    val usuarioViewModel: UsuarioViewModel = viewModel()
    val productoViewModel: ProductoViewModel = viewModel()

    // ── Estado de navegación ──────────────────────────────────────────────────
    var pantallaActual by remember { mutableStateOf("login") }
    var productoSeleccionado by remember { mutableStateOf<Producto?>(null) }
    var productoEditar by remember { mutableStateOf<Producto?>(null) }

    // Derivado del ViewModel — evita duplicar estado
    val esAdminUser = usuarioViewModel.esAdmin

    when (pantallaActual) {

        // ── LOGIN ─────────────────────────────────────────────────────────────
        "login" -> {
            LoginScreen(
                usuarioViewModel = usuarioViewModel,
                irRegistro = { pantallaActual = "registro" },
                irRecuperar = { pantallaActual = "recuperar" },
                irHome = { pantallaActual = "home" },
                irAdmin = { pantallaActual = "admin" }
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
                irDetalleProducto = { producto ->
                    productoSeleccionado = producto
                    pantallaActual = "detalle"
                }
            )
        }

        // ── PANEL ADMIN ───────────────────────────────────────────────────────
        "admin" -> {
            AdminHomeScreen(
                irCatalogo = { pantallaActual = "catalogo" },
                irEditarProducto = { producto ->
                    productoEditar = producto
                    pantallaActual = "editarProducto"
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
                esAdmin = esAdminUser,
                irAgregarProducto = { pantallaActual = "agregarProducto" },
                irEditarProducto = { producto ->
                    productoEditar = producto
                    pantallaActual = "editarProducto"
                },
                // Solo para admins: pulsando "VAPEON" en la barra superior regresa al panel
                irAdmin = { pantallaActual = "admin" }
            )
        }

        // ── DETALLE PRODUCTO ──────────────────────────────────────────────────
        "detalle" -> {
            DetalleProductoScreen(
                producto = productoSeleccionado!!,
                esAdmin = esAdminUser,
                volver = {
                    pantallaActual = if (esAdminUser) "catalogo" else "home"
                },
                irEditarProducto = { producto ->
                    productoEditar = producto
                    pantallaActual = "editarProducto"
                }
            )
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
                volver = {
                    pantallaActual = if (esAdminUser) "admin" else "catalogo"
                }
            )
        }
    }
}