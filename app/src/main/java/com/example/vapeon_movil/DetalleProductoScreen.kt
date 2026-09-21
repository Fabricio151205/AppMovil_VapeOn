package com.example.vapeon_movil

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Producto


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleProductoScreen(

    producto: Producto,
    volver: () -> Unit

) {
    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Detalle del producto")
                },

                navigationIcon = {

                    IconButton(
                        onClick = {
                            volver()
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar"
                        )

                    }

                }

            )

        }

    ) { paddingValues ->

        Column(

            modifier = Modifier

                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),


            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Text(
                text = "Imagen del producto",
                modifier = Modifier.padding(20.dp)
            )



            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "Marca: ${producto.marca}")

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = producto.descripcion)

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "Precio: S/${producto.precio}")

            Text(text = "Stock disponible: ${producto.stock}")

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "Seleccionar opción")



            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {

                Button(onClick = {}) {
                    Text("Manzana")
                }

                Button(onClick = {}
                ) {
                    Text("Uva")
                }

                Button(onClick = {}
                ) {
                    Text("Mango")
                }

            }



            Spacer(modifier = Modifier.height(30.dp))



            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Agregar al carrito"
                )
            }


        }
    }
}