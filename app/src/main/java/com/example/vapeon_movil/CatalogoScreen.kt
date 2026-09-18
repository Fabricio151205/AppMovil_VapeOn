package com.example.vapeon_movil

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen() {
    var producto by remember { mutableStateOf("") }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val opcionesMenu = listOf("Roles", "Catálogo", "Pedidos", "Pagos", "Clientes", "Proveedores")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "🛠️ Panel VapeON",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                opcionesMenu.forEach { opcion ->
                    NavigationDrawerItem(
                        label = { Text(text = opcion) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
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

                Button(onClick = { /* Acción para Todos */ }) {
                    Text(text = "Todos")
                }

                Spacer(modifier = Modifier.height(15.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "LifePood", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { /* Editar */ }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar")
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.logo_vapeon),
                    contentDescription = "Logo de VapeON",
                    modifier = Modifier.size(250.dp).align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Oxbar", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { /* Editar */ }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar")
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.logo_vapeon),
                    contentDescription = "Logo de VapeON",
                    modifier = Modifier.size(250.dp).align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
