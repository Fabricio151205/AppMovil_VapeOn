package com.example.vapeon_movil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    irCatalogo: () -> Unit = {},
    irCarrito: () -> Unit = {},
    esAdmin: Boolean = false,
    onPerfilClick: () -> Unit = {},
    onOpcionMenuClick: (String) -> Unit = {},
    onTituloClick: () -> Unit = {},
    irHistorial: () -> Unit = {},
    irPedidos: () -> Unit = {},
    cerrarSesion: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val opcionesMenu = if (esAdmin) listOf(
        "Roles",
        "Catálogo",
        "Pedidos",
        "Pagos",
        "Clientes",
        "Proveedores",
        "Cerrar sesión"
    ) else listOf(
        "Catálogo",
        "Carrito",
        "Historial de compras",
        "Cerrar sesión"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = VapeOnSurface,
                drawerContentColor = VapeOnTextPrimary
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VAPEON ",
                        style = MaterialTheme.typography.titleLarge,
                        color = VapeOnGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (esAdmin) "ADMIN" else "CLIENTE",
                        style = MaterialTheme.typography.titleLarge,
                        color = VapeOnTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(color = VapeOnDivider, modifier = Modifier.padding(vertical = 8.dp))

                opcionesMenu.forEach { opcion ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = if (opcion in listOf("Catálogo", "Carrito", "Pedidos", "Historial de compras", "Cerrar sesión")) opcion else "$opcion (No disponible)",
                                color = VapeOnTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            when (opcion) {
                                "Catálogo" -> irCatalogo()
                                "Carrito"  -> irCarrito()
                                "Historial de compras" -> if (!esAdmin) irHistorial()
                                "Pedidos" -> if (esAdmin) irPedidos()
                                "Cerrar sesión" -> cerrarSesion()
                                else -> {
                                    onOpcionMenuClick(opcion)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Sección $opcion en desarrollo")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "VAPEON",
                            color = VapeOnGold,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.clickable { onTituloClick() }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menú Principal",
                                tint = VapeOnTextPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    },
                    actions = {
                        // Ícono de Carrito de Compras (Exclusivo para CLIENTE)
                        if (!esAdmin) {
                            IconButton(onClick = irCarrito) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Carrito de Compras",
                                    tint = VapeOnTextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Avatar circular de perfil del usuario/administrador
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp, start = 4.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VapeOnRedDark)
                                .clickable { scope.launch { snackbarHostState.showSnackbar("Perfil no disponible") } },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint = VapeOnTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = VapeOnBackground
                    )
                )
            },
            containerColor = VapeOnBackground
        ) { paddingValues ->
            content(paddingValues)
        }
    }
}
