package com.example.vapeon_movil

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


@Composable
fun AdminHomeScreen(){

    Column(

        modifier = Modifier.fillMaxSize(),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center

    ){

        Text(
            text = "HOME ADMINISTRADOR"
        )


        Text(
            text = "Gestionar productos"
        )

    }

}