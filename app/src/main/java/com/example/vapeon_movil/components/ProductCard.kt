package com.example.vapeon_movil.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons


@Composable
fun ProductCard(
    nombre: String,
    precio: String,
    esAdmin: Boolean = false,
    onClick: () -> Unit,
    onEliminar: () -> Unit = {},
    onEditar: () -> Unit = {}
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

            modifier = Modifier
                .padding(12.dp)

        ){

            Text(
                text = nombre
            )


            Text(
                text = precio
            )


            if(esAdmin){

                Spacer(
                    modifier = Modifier.height(8.dp)
                )


                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.End

                ){

                    IconButton(

                        onClick = {

                            onEditar()

                        }

                    ){

                        Icon(

                            imageVector = Icons.Default.Edit,

                            contentDescription = "Editar"

                        )

                    }


                    IconButton(

                        onClick = {

                            onEliminar()

                        }

                    ){

                        Icon(

                            imageVector = Icons.Default.Delete,

                            contentDescription = "Eliminar"

                        )

                    }

                }

            }


        }

    }

}