package com.example.vapeon_movil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.components.MarcaSection


@Composable
fun CatalogoScreen(
    irDetalleProducto: (String) -> Unit
) {

    var producto by remember {
        mutableStateOf("")
    }


    Column(

        modifier = Modifier

            .fillMaxSize()

            .padding(16.dp)

            .verticalScroll(
                rememberScrollState()
            )

    ){


        Text(

            text = "Catálogo de productos",

            style = MaterialTheme.typography.titleLarge

        )


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        TextField(

            value = producto,

            onValueChange = {

                producto = it

            },

            label = {

                Text(
                    "Buscar productos"
                )

            },

            modifier = Modifier.fillMaxWidth()

        )


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        Text(
            text = "Categorías"
        )


        Spacer(
            modifier = Modifier.height(10.dp)
        )


        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.SpaceEvenly

        ){

            Button(
                onClick = {}
            ){

                Text("Todos")

            }


            Button(
                onClick = {}
            ){

                Text("Nuevos")

            }


            Button(
                onClick = {}
            ){

                Text("Promos")

            }

        }



        Spacer(
            modifier = Modifier.height(25.dp)
        )



        MarcaSection(

            nombreMarca = "LifePood",

            productos = listOf(

                "Kit LifePood",

                "Batería LifePood",

                "Recarga LifePood"

            ),

            irDetalleProducto = irDetalleProducto

        )



        MarcaSection(

            nombreMarca = "Oxbar",

            productos = listOf(

                "Kit Oxbar",

                "Batería Oxbar",

                "Recarga Oxbar"

            ),

            irDetalleProducto = irDetalleProducto

        )



        MarcaSection(

            nombreMarca = "Nexa",

            productos = listOf(

                "Kit Nexa",

                "Batería Nexa",

                "Recarga Nexa"

            ),

            irDetalleProducto = irDetalleProducto

        )


    }


}