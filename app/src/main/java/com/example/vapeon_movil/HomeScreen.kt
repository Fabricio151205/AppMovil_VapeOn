package com.example.vapeon_movil

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.components.ProductCard


@Composable
fun HomeScreen(){

    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)

    ){

        Text(
            text = "VapeON"
        )


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        TextField(

            value = "",

            onValueChange = {},

            label = {
                Text("Buscar productos")
            },

            modifier = Modifier.fillMaxWidth()

        )


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        Text(
            text = "Categorías"
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
            modifier = Modifier.height(20.dp)
        )


        Text(
            text = "Productos destacados"
        )


        ProductCard(
            nombre = "LifePood",
            precio = "S/80",
            onClick = {
                //aca abrimos detalle producto
            }
        )


        ProductCard(
            nombre = "Oxbar",
            precio = "S/60",
            onClick = {
                //aca abrimos detalle producto
            }
        )


    }

}