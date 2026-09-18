package com.example.vapeon_movil.Data.Models

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun RegisterScreen(){

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
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),

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
            text = "REGISTRATE"
        )

        Text(
            text = "Únete a la revolucion. Enciende tu experiencia"
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
            modifier = Modifier.height(10.dp)
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
                Text("••••••••")
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
                Text("••••••••")
            },
            modifier = Modifier.fillMaxWidth()
        )

        val estadoContrasena = if (passwordConfirm == password) "Las contraseñas coinciden"
        else "Las contraseñas no coinciden"

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        Text(
            text = estadoContrasena
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {
            },
            modifier = Modifier.fillMaxWidth()
        ){
            Text(
                text = "REGISTRARSE"
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "¿Tienes una cuenta? Inicia Sesion Aquí"
        )
    }
}
