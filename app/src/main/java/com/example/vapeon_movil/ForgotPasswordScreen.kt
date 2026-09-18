package com.example.vapeon_movil

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