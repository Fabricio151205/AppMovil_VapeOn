package com.example.vapeon_movil


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(irCatalogo: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val scope = rememberCoroutineScope()

    val opcionesMenu = listOf(

        "Roles",
        "Catálogo",
        "Pedidos",
        "Pagos",
        "Clientes",
        "Proveedores"

    )


    var pantallaAdmin by remember {
        mutableStateOf("inicio")
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
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

                            pantallaAdmin = opcion
                            // Aquí pones la acción para cuando den clic a cada opción
                            scope.launch { drawerState.close() } // Cierra el menú al dar clic
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("🔥 VAPEON") },
                    navigationIcon = {
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

            ) {
                when(pantallaAdmin){


                    "Catálogo" -> {

                            irCatalogo()





                    }


                    else -> {

                        Column(

                            modifier = Modifier.fillMaxSize(),

                            horizontalAlignment = Alignment.CenterHorizontally,

                            verticalArrangement = Arrangement.Center

                        ){

                            Text(
                                text = "HOME ADMINISTRADOR"
                            )

                            Text(
                                text = "Panel de gestión VapeON"
                            )

                        }

                    }

                }
                Spacer(modifier = Modifier.padding(5.dp))

            }
        }
    }
}
