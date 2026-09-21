package com.example.vapeon_movil

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.utils.RetrofitClient
import com.example.vapeon_movil.services.UsuarioService
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    irRegistro: () -> Unit,
    irRecuperar: () -> Unit,
    irHome: () -> Unit,
    irAdmin: () -> Unit
){

    val scope = rememberCoroutineScope()
    val usuarioService = remember { 
        RetrofitClient.retrofit.create(UsuarioService::class.java) 
    }

    var correo by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    // Variable para manejar estados de error visibles en consola o textos
    var mensajeError by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }


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

                cargando = true
                mensajeError = ""

                scope.launch {

                    try {

                        val respuesta = usuarioService.listarUsuarios()

                        val listaUsuarios = respuesta.documents


                        val usuarioValido = listaUsuarios?.find { doc ->

                            doc.fields.correo.stringValue == correo &&
                                    doc.fields.password.stringValue == password

                        }


                        cargando = false


                        if (usuarioValido != null) {


                            val rol = usuarioValido.fields.rol.stringValue


                            if (rol == "ADMIN") {

                                irAdmin()

                            } else {

                                irHome()

                            }


                        } else {


                            mensajeError = "Correo o contraseña incorrectos"

                            println(mensajeError)


                        }


                    } catch (e: Exception) {


                        cargando = false


                        mensajeError =
                            "Error de conexión: ${e.localizedMessage}"


                        println(mensajeError)


                    }

                }

            },


            enabled = !cargando,


            modifier = Modifier.fillMaxWidth()

        ) {


            if (cargando) {


                CircularProgressIndicator(

                    modifier = Modifier.size(24.dp),

                    color = MaterialTheme.colorScheme.onPrimary

                )


            } else {


                Text(
                    text = "INICIAR SESIÓN"
                )


            }

        }

        if (mensajeError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = mensajeError, color = MaterialTheme.colorScheme.error)
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