package com.example.vapeon_movil.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun ProductCard(
    nombre:String,
    precio:String,
    esAdmin:Boolean = false,
    onClick: () -> Unit
){

    Card(

        modifier = Modifier
            .width(150.dp)
            .padding(8.dp)
            .clickable {

                onClick()

            }
    ){

        Column(

            modifier = Modifier.padding(15.dp)

        ){

            Text(
                text = nombre
            )


            Text(
                text = precio
            )


        }

    }
    if(esAdmin){

        Row(

            horizontalArrangement = Arrangement.SpaceEvenly,

            modifier = Modifier.fillMaxWidth()

        ){

            Button(
                onClick = {

                }
            ){

                Text("✏ Editar")

            }


            Button(
                onClick = {

                }
            ){

                Text("Eliminar")

            }

        }

    }

}