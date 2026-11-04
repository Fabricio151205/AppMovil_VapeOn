package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.R
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.moneda
import com.example.vapeon_movil.viewmodels.CarritoViewModel

@Composable
fun DatosEntregaScreen(
    usuario: Usuario?,
    carritoViewModel: CarritoViewModel? = null,
    volver: () -> Unit,
    continuar: () -> Unit
) {
    val items = carritoViewModel?.items.orEmpty()
    val resumen = carritoViewModel?.resumen

    val clienteNombre = if (usuario != null && usuario.nombre.isNotBlank()) {
        "${usuario.nombre} ${usuario.apellido}".trim()
    } else "Fabricio Alvarez"

    val clienteTelefono = usuario?.telefono?.takeIf { it.isNotBlank() } ?: "999 123 456"

    val hoy = remember { java.time.LocalDateTime.now() }
    val fechaTexto = remember(hoy) { hoy.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) }
    val horaTexto = remember(hoy) { hoy.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = VapeOnBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ENCABEZADO SUPERIOR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = volver) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Datos de Entrega",
                    color = VapeOnTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ESTRUCTURA COMPLETA DE LA BOLETA CON DENTADOS TROQUELADOS
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // BORDE DECORATIVO SUPERIOR DE BOLETA (ARREGLO BOLETA)
                Image(
                    painter = painterResource(id = R.drawable.arreglo_boleta),
                    contentDescription = "Borde de Boleta Superior",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                )

                // CUERPO CENTRAL DE LA BOLETA CON FONDO VAPEON SURFACE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VapeOnSurface)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // TÍTULO BOLETA
                    Text(
                        text = "BOLETA ELECTRONICA N° 0001",
                        color = VapeOnTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = VapeOnGold, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // DATOS DEL DÍA Y HORA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Fecha: $fechaTexto", color = VapeOnTextPrimary, fontSize = 13.sp)
                        Text(text = "Hora: $horaTexto", color = VapeOnTextPrimary, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Cliente: $clienteNombre", color = VapeOnTextPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Telefono: $clienteTelefono", color = VapeOnTextPrimary, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = VapeOnGold, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // CABECERA SECCIÓN PRODUCTOS
                    Text(
                        text = "PRODUCTOS DE LA COMPRA",
                        color = VapeOnGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Productos", color = VapeOnTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(2f))
                        Text(text = "Cant.", color = VapeOnTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                        Text(text = "Precio", color = VapeOnTextSecondary, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = VapeOnInputBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // TABLA DE PRODUCTOS DINÁMICOS DESDE EL CARRITO
                    if (items.isNotEmpty()) {
                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(
                                        text = item.nombre,
                                        color = VapeOnTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (!item.saborSeleccionado.isNullOrBlank()) {
                                        Text(
                                            text = "Sabor: ${item.saborSeleccionado}",
                                            color = VapeOnTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Text(
                                    text = item.cantidad.toString(),
                                    color = VapeOnTextPrimary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.8f)
                                )
                                Text(
                                    text = moneda(item.precioUnit * item.cantidad),
                                    color = VapeOnTextPrimary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                        }
                    } else {
                        // Muestra por defecto si el carrito está vacío en simulación
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Vasito Snowwolf,\nDesechable", color = VapeOnTextPrimary, fontSize = 12.sp, modifier = Modifier.weight(2f))
                            Text(text = "1", color = VapeOnTextPrimary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                            Text(text = "S/. 20.00", color = VapeOnTextPrimary, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Kit Completo,\nBateria + Pod", color = VapeOnTextPrimary, fontSize = 12.sp, modifier = Modifier.weight(2f))
                            Text(text = "2", color = VapeOnTextPrimary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                            Text(text = "S/. 70.00", color = VapeOnTextPrimary, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = VapeOnGold, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // TOTALES DINÁMICOS
                    val subTotalValor = resumen?.subTotal ?: 90.0
                    val igvValor = resumen?.igv ?: 0.0
                    val deliveryValor = resumen?.delivery ?: 5.0
                    val totalValor = resumen?.total ?: 100.0

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Sub Total:", color = VapeOnTextPrimary, fontSize = 13.sp)
                        Text(text = moneda(subTotalValor), color = VapeOnTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "IGV (18%):", color = VapeOnTextPrimary, fontSize = 13.sp)
                        Text(text = moneda(igvValor), color = VapeOnTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Delivery:", color = VapeOnTextPrimary, fontSize = 13.sp)
                        Text(text = moneda(deliveryValor), color = VapeOnTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Total a pagar:", color = VapeOnTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = moneda(totalValor), color = VapeOnTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // BORDE DECORATIVO INFERIOR DE BOLETA (ARREGLO PEDIDO)
                Image(
                    painter = painterResource(id = R.drawable.arreglo_pedido),
                    contentDescription = "Borde de Boleta Inferior",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(30.dp))

            // BOTÓN CONTINUAR
            Button(
                onClick = continuar,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VapeOnRed,
                    contentColor = VapeOnTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "CONTINUAR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Contenedor visual común para checkout */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasoCheckout(
    titulo: String,
    volver: () -> Unit,
    bloqueado: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit
) {
    BackHandler { if (!bloqueado) volver() }
    VapeON_MovilTheme {
        Scaffold(
            containerColor = VapeOnBackground,
            topBar = {
                TopAppBar(
                    title = { Text("VAPEON", color = VapeOnGold, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = volver, enabled = !bloqueado) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = VapeOnTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(titulo, style = MaterialTheme.typography.headlineSmall, color = VapeOnGold)
                contenido()
            }
        }
    }
}
