package com.example.vapeon_movil.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.components.ProductCard


@Composable
fun MarcaSection(

    nombreMarca: String,

    productos: List<String>,

    irDetalleProducto: (String) -> Unit

){


    Column(

        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)

    ){


        Text(

            text = nombreMarca

        )


        Spacer(

            modifier = Modifier.height(10.dp)

        )


        LazyRow{


            items(productos){ producto ->


                ProductCard(

                    nombre = producto,

                    precio = "S/80",

                    onClick = {
                        irDetalleProducto(producto)
                    }



                )


            }


        }


    }


}