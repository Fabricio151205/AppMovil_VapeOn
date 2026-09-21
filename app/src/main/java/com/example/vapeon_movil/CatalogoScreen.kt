package com.example.vapeon_movil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.components.MarcaSection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import kotlinx.coroutines.launch
import com.example.vapeon_movil.services.ProductoService
import com.example.vapeon_movil.utils.RetrofitClient
import androidx.compose.runtime.LaunchedEffect
import com.example.vapeon_movil.entities.Producto


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    irDetalleProducto: (Producto) -> Unit,
    esAdmin: Boolean = false,
    irAgregarProducto: () -> Unit = {},
    irEditarProducto: (Producto) -> Unit = {}
) {

    var producto by remember { mutableStateOf("")    }


    var productosFirebase by remember { mutableStateOf(emptyList<Producto>())    }

    // conexión con productos Firebase
    val productoService = remember {RetrofitClient.retrofit.create(ProductoService::class.java) }

    // estado de carga
    var cargando by remember { mutableStateOf(true) }

    // Estado para controlar si el menú lateral está abierto o cerrado
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // Coroutine scope necesario para abrir/cerrar el menú de forma animada
    val scope = rememberCoroutineScope()

    fun eliminarProductoFirebase(producto: Producto){

        scope.launch {

            try {

                productoService.eliminarProducto(producto.id)


                productosFirebase =
                    productosFirebase.filter {
                        it.id != producto.id
                    }


            } catch(e: Exception){

                println(
                    "Error eliminando producto: ${e.message}"
                )

            }

        }

    }

    // Lista con las opciones de tu menú hamburguesa
    val opcionesMenu = listOf("Roles", "Catálogo", "Pedidos", "Pagos", "Clientes", "Proveedores")

    LaunchedEffect(Unit) {

        scope.launch {

            try {

                val respuesta = productoService.listarProductos()


                productosFirebase = respuesta.documents?.map {

                    Producto(

                        id = it.name.substringAfterLast("/"),

                        nombre = it.fields.nombre?.stringValue ?: "",

                        marca = it.fields.marca?.stringValue ?: "",

                        precio = it.fields.precio?.stringValue ?: "",

                        stock = it.fields.stock?.stringValue ?: "",

                        descripcion = it.fields.descripcion?.stringValue ?: ""

                    )

                } ?: emptyList()



            } catch(e: Exception){

                println(
                    "Error productos: ${e.message}"
                )

            }

        }

    }

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


                Text(

                    text = "Catálogo de productos",

                    style = MaterialTheme.typography.titleLarge

                )
                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = producto,
                    onValueChange = { producto = it },
                    label = { Text("Buscar Productos") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Spacer(
                    modifier = Modifier.height(20.dp)
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

                if(esAdmin){

                    Button(

                        onClick = {

                            irAgregarProducto()

                        },

                        modifier = Modifier.fillMaxWidth()

                    ){

                        Text(
                            text = "+ Agregar producto"
                        )

                    }


                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                }

                Spacer(
                    modifier = Modifier.height(25.dp)
                )
                MarcaSection(

                    nombreMarca = "LifePood",

                    productos = productosFirebase.filter{
                      it.marca == "LifePood"
                    },

                    irDetalleProducto = irDetalleProducto,

                    esAdmin = true,

                    eliminarProducto = {
                        eliminarProductoFirebase(it)

                    },

                    editarProducto = {
                        irEditarProducto(it)
                    }

                )



                MarcaSection(

                    nombreMarca = "Oxbar",

                    productos = productosFirebase.filter{
                        it.marca == "Oxbar"
                    },


                    irDetalleProducto = irDetalleProducto,

                    esAdmin = true,

                    eliminarProducto = {
                        eliminarProductoFirebase(it)
                    },
                    editarProducto = {
                        irEditarProducto(it)
                    }

                )



                MarcaSection(

                    nombreMarca = "Nexa",

                    productos = productosFirebase.filter{
                        it.marca == "Nexa"
                    },


                    irDetalleProducto = irDetalleProducto,

                    esAdmin = true,

                    eliminarProducto = {
                        eliminarProductoFirebase(it)
                    },
                    editarProducto = {
                        irEditarProducto(it)
                    }

                )
            }
        }
    }






}