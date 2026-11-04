package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.vapeon_movil.viewmodels.PedidoViewModel

@Composable
fun DatosEntregaScreen(
    usuario: Usuario?,
    carritoViewModel: CarritoViewModel? = null,
    pedidoViewModel: PedidoViewModel? = null,
    volver: () -> Unit,
    continuar: () -> Unit
) {
    val items = carritoViewModel?.items.orEmpty()
    val resumen = carritoViewModel?.resumen

    val clienteNombre = "${usuario?.nombre.orEmpty()} ${usuario?.apellido.orEmpty()}".trim().ifBlank { "Sin nombre registrado" }
    val clienteTelefono = usuario?.telefono?.takeIf { it.isNotBlank() } ?: "Sin teléfono registrado"

    val hoy = remember { java.time.LocalDateTime.now() }
    val fechaTexto = remember(hoy) { hoy.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) }
    val horaTexto = remember(hoy) { hoy.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }

    // Cargar la caja global para calcular el contador correlativo de boletas en la aplicación
    LaunchedEffect(usuario) {
        pedidoViewModel?.cargarCaja(usuario)
    }

    val numeroBoletaInt = (pedidoViewModel?.pedidosCaja?.size ?: 0) + 1
    val numeroBoletaTexto = remember(numeroBoletaInt) { String.format(java.util.Locale.ROOT, "BOLETA ELECTRONICA N° %04d", numeroBoletaInt) }

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

            // CONTENEDOR DE BOLETA DINÁMICO QUE SE EXPANDE SEGÚN EL CONTENIDO
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // IMAGEN DE FONDO DE BOLETA QUE SE ADAPTA AL TAMAÑO EXACTO DEL PADRE
                Image(
                    painter = painterResource(id = R.drawable.arreglo_boleta),
                    contentDescription = "Fondo de Boleta Dinámico",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize()
                )

                // CONTENIDO DE LA BOLETA CON ESPACIADO INTERNO CUIDADO
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    // TÍTULO BOLETA INCREMENTAL
                    Text(
                        text = numeroBoletaTexto,
                        color = VapeOnGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = VapeOnGold, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // DATOS DEL DÍA Y HORA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Fecha: $fechaTexto", color = VapeOnTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(text = "Hora: $horaTexto", color = VapeOnTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Cliente: $clienteNombre", color = VapeOnTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Telefono: $clienteTelefono", color = VapeOnTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = VapeOnGold, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // CABECERA SECCIÓN PRODUCTOS
                    Text(
                        text = "PRODUCTOS DE LA COMPRA",
                        color = VapeOnGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Productos", color = VapeOnTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        Text(text = "Cant.", color = VapeOnTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                        Text(text = "Precio", color = VapeOnTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = VapeOnInputBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // TABLA DE PRODUCTOS REALES DESDE EL CARRITO
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(
                                    text = item.nombre,
                                    color = VapeOnTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!item.saborSeleccionado.isNullOrBlank()) {
                                    Text(
                                        text = "Sabor: ${item.saborSeleccionado}",
                                        color = VapeOnTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                text = item.cantidad.toString(),
                                color = VapeOnTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.8f)
                            )
                            Text(
                                text = moneda(item.precioUnit * item.cantidad),
                                color = VapeOnTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = VapeOnGold, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // TOTALES REALES DESDE EL CARRITO
                    val subTotalValor = resumen?.subTotal ?: 0.0
                    val igvValor = resumen?.igv ?: 0.0
                    val deliveryValor = resumen?.delivery ?: 0.0
                    val totalValor = resumen?.total ?: 0.0

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Sub Total:", color = VapeOnTextPrimary, fontSize = 14.sp)
                        Text(text = moneda(subTotalValor), color = VapeOnTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "IGV (18%):", color = VapeOnTextPrimary, fontSize = 14.sp)
                        Text(text = moneda(igvValor), color = VapeOnTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Delivery:", color = VapeOnTextPrimary, fontSize = 14.sp)
                        Text(text = moneda(deliveryValor), color = VapeOnTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Total a pagar:", color = VapeOnGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = moneda(totalValor), color = VapeOnGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
