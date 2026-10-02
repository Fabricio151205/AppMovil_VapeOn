package com.example.vapeon_movil


import androidx.compose.runtime.*
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


@Composable
fun VapeONApp(){
    var pantallaActual by remember {
        mutableStateOf("login")
    }
    var esAdminUser by remember {
        mutableStateOf(true) // Por defecto true para facilitar navegación administrativa durante pruebas
    }
    var productoSeleccionado by remember {
        mutableStateOf<Producto?>(null)
    }
    var productoEditar by remember {
        mutableStateOf<Producto?>(null)
    }
    when(pantallaActual){
        "login" -> {
            LoginScreen(
                irRegistro = {
                    pantallaActual = "registro"
                },
                irRecuperar = {
                    pantallaActual = "recuperar"
                },
                irHome = {
                    esAdminUser = false
                    pantallaActual = "home"
                },
                irAdmin = {
                    esAdminUser = true
                    pantallaActual = "admin"
                }
            )
        }
        "registro" -> {
            RegisterScreen(
                volverLogin = {
                    pantallaActual = "login"
                }
            )
        }
        "recuperar" -> {
            ForgotPasswordScreen(
                volverLogin = {
                    pantallaActual = "login"
                }
            )
        }
        "home" -> {
            HomeScreen(
                irDetalleProducto = { producto ->
                    productoSeleccionado = producto
                    pantallaActual = "detalle"
                }
            )
        }
        "admin" -> {
            AdminHomeScreen(
                irCatalogo = {
                    pantallaActual = "catalogo"
                },
                irEditarProducto = { producto ->
                    productoEditar = producto
                    pantallaActual = "editarProducto"
                }
            )
        }
        "catalogo" -> {
            CatalogoScreen(
                irDetalleProducto = { producto ->
                    productoSeleccionado = producto
                    pantallaActual = "detalle"
                },

                esAdmin = esAdminUser,
                irAgregarProducto = {
                    pantallaActual = "agregarProducto"
                },

                irEditarProducto = { producto ->
                    productoEditar = producto
                    pantallaActual = "editarProducto"
                },

                // Solo para admins: pulsando "VAPEON" en la barra superior regresa al panel de admin
                irAdmin = {
                    pantallaActual = "admin"
                }
            )
        }
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
        "agregarProducto" -> {

            AgregarProductoScreen(

                volver = {

                    pantallaActual = "catalogo"

                }

            )

        }
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