package com.example.vapeon_movil

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.FirestoreFieldsContainer
import com.example.vapeon_movil.services.FirestoreStringValue
import com.example.vapeon_movil.services.UsuarioFields
import com.example.vapeon_movil.utils.RetrofitClient
import com.example.vapeon_movil.services.UsuarioService
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    volverLogin: () -> Unit
){

    val scope = rememberCoroutineScope()
    val usuarioService = remember { 
        RetrofitClient.retrofit.create(UsuarioService::class.java) 
    }

    var nombre by remember {
        mutableStateOf("")
    }

    var apellidos by remember {
        mutableStateOf("")
    }

    var correo by remember {
        mutableStateOf("")
    }

    var telefono by remember {
        mutableStateOf("")
    }

    var fechaNacimiento by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordConfirm by remember {
        mutableStateOf("")
    }


    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center

    ){


        Text(
            text = "REGISTRO VAPEON"
        )


        Text(
            text = "Únete a la revolución. Enciende tu experiencia"
        )


        Spacer(
            modifier = Modifier.height(30.dp)
        )


        TextField(
            value = nombre,

            onValueChange = {
                nombre = it
            },

            label = {
                Text("Tu Nombre")
            },

            modifier = Modifier.fillMaxWidth()
        )


        Spacer(
            modifier = Modifier.height(15.dp)
        )


        TextField(

            value = apellidos,

            onValueChange = {
                apellidos = it
            },

            label = {
                Text("Tus Apellidos")
            },

            modifier = Modifier.fillMaxWidth()
        )


        Spacer(
            modifier = Modifier.height(15.dp)
        )


        TextField(

            value = correo,

            onValueChange = {
                correo = it
            },

            label = {
                Text("tu@correo.com")
            },

            modifier = Modifier.fillMaxWidth()
        )


        Spacer(
            modifier = Modifier.height(15.dp)
        )


        TextField(

            value = telefono,

            onValueChange = {
                telefono = it
            },

            label = {
                Text("987654321")
            },

            modifier = Modifier.fillMaxWidth()
        )


        Spacer(
            modifier = Modifier.height(15.dp)
        )


        TextField(

            value = fechaNacimiento,

            onValueChange = {
                fechaNacimiento = it
            },

            label = {
                Text("dd/mm/aaaa")
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
            modifier = Modifier.height(15.dp)
        )


        TextField(

            value = passwordConfirm,

            onValueChange = {
                passwordConfirm = it
            },

            label = {
                Text("Confirmar contraseña")
            },

            modifier = Modifier.fillMaxWidth()

        )


        Spacer(
            modifier = Modifier.height(15.dp)
        )


        val estadoContrasena =
            if(passwordConfirm == password)
                "Las contraseñas coinciden"
            else
                "Las contraseñas no coinciden"


        Text(
            text = estadoContrasena
        )


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        Button(
            onClick = {
                if (password == passwordConfirm) {

                    // Estructuramos el objeto según lo que pide la API REST de Firebase
                    val usuarioFirebaseFormat = FirestoreFieldsContainer(
                        fields = UsuarioFields(
                            nombre = FirestoreStringValue(nombre),
                            apellido = FirestoreStringValue(apellidos),
                            telefono = FirestoreStringValue(telefono),
                            fechaNacimiento = FirestoreStringValue(fechaNacimiento),
                            correo = FirestoreStringValue(correo),
                            password = FirestoreStringValue(password),
                            rol = FirestoreStringValue("CLIENTE")
                        )
                    )

                    // Ejecutamos la corrutina asíncrona estilo tu profesor
                    scope.launch {
                        try {
                            // Consumimos el servicio de la API REST
                            usuarioService.crearUsuario(usuarioFirebaseFormat)
                            println("¡Usuario registrado exitosamente en la API REST de Firebase Firestore!")

                            // Volvemos al Login
                            volverLogin()
                        } catch (e: Exception) {
                            println("Error en la API REST de Firebase: ${e.localizedMessage}")
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Crear cuenta")
        }


        TextButton(
            onClick = {

                volverLogin()

            }
        ){

            Text(
                text = "Ya tengo cuenta"

            )

        }


    }

}