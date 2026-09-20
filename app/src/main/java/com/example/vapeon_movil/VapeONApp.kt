package com.example.vapeon_movil


import androidx.compose.runtime.*


@Composable
fun VapeONApp(){


    var pantallaActual by remember {

        mutableStateOf("login")

    }


    var productoSeleccionado by remember {

        mutableStateOf("")

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


                }

            )


        }



        "detalle" -> {


            DetalleProductoScreen(

                nombreProducto = productoSeleccionado

            )


        }



    }


}