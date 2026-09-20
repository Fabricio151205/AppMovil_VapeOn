package com.example.vapeon_movil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.components.MarcaSection
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    irDetalleProducto: (String) -> Unit
) {
    var producto by remember { mutableStateOf("") }

    // Estado para controlar si el menú lateral está abierto o cerrado
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // Coroutine scope necesario para abrir/cerrar el menú de forma animada
    val scope = rememberCoroutineScope()

    // Lista con las opciones de tu menú hamburguesa
    val opcionesMenu = listOf("Roles", "Catálogo", "Pedidos", "Pagos", "Clientes", "Proveedores")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Este es el diseño interno del panel lateral que se desliza
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Menu VapeON",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Generamos cada una de las opciones del menú
                opcionesMenu.forEach { opcion ->
                    NavigationDrawerItem(
                        label = { Text(text = opcion) },
                        selected = false,
                        onClick = {
                            // Aquí pones la acción para cuando den clic a cada opción
                            scope.launch { drawerState.close() } // Cierra el menú al dar clic
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        // El Scaffold contiene la barra de arriba y el contenido principal
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("🔥 VAPEON") },
                    navigationIcon = {
                        // El botón clásico de hamburguesa para abrir el menú
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menú"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            // Contenido de tu pantalla principal (le añadimos scroll por si hay muchas imágenes)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                TextField(
                    value = producto,
                    onValueChange = { producto = it },
                    label = { Text("Buscar Productos") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

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
    }






}