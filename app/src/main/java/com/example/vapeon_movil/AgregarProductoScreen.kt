package com.example.vapeon_movil

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.services.*
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.launch


@Composable
fun AgregarProductoScreen(
    volver: () -> Unit
){

    val scope = rememberCoroutineScope()

    val productoService = remember {

        RetrofitClient.retrofit.create(
            ProductoService::class.java
        )

    }


    var nombre by remember {
        mutableStateOf("")
    }

    var marca by remember {
        mutableStateOf("")
    }

    var precio by remember {
        mutableStateOf("")
    }

    var stock by remember {
        mutableStateOf("")
    }

    var descripcion by remember {
        mutableStateOf("")
    }



    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)

    ){


        Text(
            text = "Agregar Producto",
            style = MaterialTheme.typography.titleLarge
        )


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        TextField(
            value = nombre,
            onValueChange = {
                nombre = it
            },
            label = {
                Text("Nombre")
            },
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(
            modifier = Modifier.height(10.dp)
        )


        TextField(
            value = marca,
            onValueChange = {
                marca = it
            },
            label = {
                Text("Marca")
            },
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(
            modifier = Modifier.height(10.dp)
        )


        TextField(
            value = precio,
            onValueChange = {
                precio = it
            },
            label = {
                Text("Precio")
            },
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(
            modifier = Modifier.height(10.dp)
        )


        TextField(
            value = stock,
            onValueChange = {
                stock = it
            },
            label = {
                Text("Stock")
            },
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(
            modifier = Modifier.height(10.dp)
        )


        TextField(
            value = descripcion,
            onValueChange = {
                descripcion = it
            },
            label = {
                Text("Descripción")
            },
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        Button(

            onClick = {


                val nuevoProducto = FirestoreProductoContainer(

                    fields = ProductoFields(

                        nombre = FirestoreStringValue(nombre),

                        marca = FirestoreStringValue(marca),

                        precio = FirestoreStringValue(precio),

                        stock = FirestoreStringValue(stock),

                        descripcion = FirestoreStringValue(descripcion)

                    )

                )


                scope.launch {

                    try {

                        productoService.crearProducto(
                            nuevoProducto
                        )


                        volver()


                    }catch(e: Exception){

                        println(
                            "Error creando producto: ${e.message}"
                        )

                    }


                }


            },

            modifier = Modifier.fillMaxWidth()

        ){

            Text(
                "Guardar Producto"
            )

        }



        Spacer(
            modifier = Modifier.height(10.dp)
        )


        TextButton(

            onClick = {

                volver()

            }

        ){

            Text(
                "Cancelar"
            )

        }


    }

}