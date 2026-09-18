package com.example.vapeon_movil


import androidx.compose.runtime.*
import androidx.compose.material3.*
import com.example.vapeon_movil.Data.Models.RegisterScreen


@Composable
fun VapeONApp(){

    var pantallaActual by remember {
        mutableStateOf("login")
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
            )

        }


        "recuperar" -> {

            ForgotPasswordScreen(
                volverLogin = {

                    pantallaActual = "login"
            )

        }


        "home" -> {

            HomeScreen()

        }


        "admin" -> {

            AdminHomeScreen()

        }


    }

}