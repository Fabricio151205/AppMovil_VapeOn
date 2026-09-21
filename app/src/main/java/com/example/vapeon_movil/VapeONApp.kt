package com.example.vapeon_movil


import androidx.compose.runtime.*
import com.example.vapeon_movil.entities.Producto


@Composable
fun VapeONApp(){
    var pantallaActual by remember {

        mutableStateOf("login")

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
                    pantallaActual = "home"
                },
                irAdmin = {
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
            HomeScreen()
        }
        "admin" -> {
            AdminHomeScreen(
                irCatalogo = {
                    pantallaActual = "catalogo"
                }
            )
        }
        "catalogo" -> {
            CatalogoScreen(
                irDetalleProducto = {
                        producto ->
                    productoSeleccionado = producto
                    pantallaActual = "detalle"
                },

                esAdmin = true,
                irAgregarProducto = {
                    pantallaActual = "agregarProducto"
                },

                irEditarProducto = {

                        producto ->

                    productoEditar = producto

                    pantallaActual = "editarProducto"

                }
            )
        }
        "detalle" -> {
            DetalleProductoScreen(
                producto = productoSeleccionado!!,
                volver = {
                    pantallaActual = "catalogo"
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

                    pantallaActual = "catalogo"

                }

            )

        }
    }
}