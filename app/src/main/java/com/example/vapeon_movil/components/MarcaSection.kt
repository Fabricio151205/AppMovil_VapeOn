package com.example.vapeon_movil.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Producto


@Composable
fun MarcaSection(

    nombreMarca: String,

    productos: List<Producto>,

    irDetalleProducto: (Producto) -> Unit,

    esAdmin: Boolean = false,

    eliminarProducto: (Producto) -> Unit = {},

    editarProducto: (Producto) -> Unit = {}

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

                    nombre = producto.nombre,

                    precio = "S/${producto.precio}",

                    esAdmin = esAdmin,

                    onClick = {

                        irDetalleProducto(producto)

                    },


                    onEliminar = {

                        eliminarProducto(producto)

                    },


                    onEditar = {

                        editarProducto(producto)

                    }

                )


            }

        }

    }

}