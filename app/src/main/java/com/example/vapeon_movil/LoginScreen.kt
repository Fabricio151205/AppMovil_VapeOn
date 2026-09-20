package com.example.vapeon_movil

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun LoginScreen(
    irRegistro: () -> Unit,
    irRecuperar: () -> Unit,
    irHome: () -> Unit,
    irAdmin: () -> Unit
){

    var correo by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }


    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center

    ) {


        Text(
            text = "🔥 VAPEON",
        )


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        Text(
            text = "BIENVENIDO A VAPEON"
        )


        Text(
            text = "Ingresa para continuar tu experiencia"
        )


        Spacer(
            modifier = Modifier.height(30.dp)
        )


        TextField(

            value = correo,

            onValueChange = {
                correo = it
            },

            label = {
                Text("Usuario o Correo")
            },

            modifier = Modifier.fillMaxWidth()

        )


        Spacer(
            modifier = Modifier.height(15.dp)
        )


        TextField(

            value = password,

            onValueChange = {
                password = it
            },

            label = {
                Text("Contraseña")
            },

            modifier = Modifier.fillMaxWidth()

        )


        Spacer(
            modifier = Modifier.height(10.dp)
        )


        TextButton(
            onClick = {

                irRecuperar()

            }
        ){

            Text(
                text = "¿Olvidaste tu contraseña?"
            )

        }


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        Button(

            onClick = {

                if(correo == "admin@vapeon.com" && password == "1234") {
                    irAdmin()
                }else {(correo == "cliente@gmail.com" && password == "1234")
                    irHome()
                }

            },

            modifier = Modifier.fillMaxWidth()

        ){

            Text(
                text = "INICIAR SESIÓN"
            )

        }


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        TextButton(
            onClick = {

                irRegistro()

            }
        ){

            Text(
                text = "¿No tienes cuenta? Crear nuevo Usuario"
            )

        }

    }

}