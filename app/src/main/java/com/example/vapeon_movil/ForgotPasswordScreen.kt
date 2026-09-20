package com.example.vapeon_movil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment

@Composable
fun ForgotPasswordScreen(
    volverLogin: () -> Unit
){

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){

        Text(
            text = "RECUPERAR CONTRASEÑA"
        )


        Button(
            onClick = {

            }
        ){

            Text(
                text = "Enviar correo"
            )

        }


        TextButton(
            onClick = {

                volverLogin()

            }
        ){

            Text(
                text = "Volver"

            )

        }

    }

}