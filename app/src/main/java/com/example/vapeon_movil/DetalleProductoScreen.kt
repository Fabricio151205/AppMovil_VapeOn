package com.example.vapeon_movil

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun DetalleProductoScreen(

    nombreProducto: String

){


    Column(

        modifier = Modifier

            .fillMaxSize()

            .padding(20.dp),


        horizontalAlignment = Alignment.CenterHorizontally

    ){



        Text(

            text = "Imagen del producto",

            modifier = Modifier.padding(20.dp)

        )



        Spacer(

            modifier = Modifier.height(20.dp)

        )



        Text(

            text = nombreProducto,

            style = MaterialTheme.typography.titleLarge

        )



        Spacer(

            modifier = Modifier.height(20.dp)

        )



        Text(

            text = "Descripción del producto"

        )



        Spacer(

            modifier = Modifier.height(10.dp)

        )



        Text(

            text = "Características del producto"

        )



        Spacer(

            modifier = Modifier.height(20.dp)

        )



        Text(

            text = "Precio: S/80"

        )



        Text(

            text = "Stock disponible"

        )



        Spacer(

            modifier = Modifier.height(20.dp)

        )



        Text(

            text = "Seleccionar opción"

        )



        Row(

            horizontalArrangement = Arrangement.SpaceEvenly,

            modifier = Modifier.fillMaxWidth()

        ){

            Button(
                onClick = {}
            ){

                Text("Manzana")

            }



            Button(
                onClick = {}
            ){

                Text("Uva")

            }



            Button(
                onClick = {}
            ){

                Text("Mango")

            }


        }



        Spacer(

            modifier = Modifier.height(30.dp)

        )



        Button(

            onClick = {},

            modifier = Modifier.fillMaxWidth()

        ){

            Text(

                text = "Agregar al carrito"

            )

        }



    }


}