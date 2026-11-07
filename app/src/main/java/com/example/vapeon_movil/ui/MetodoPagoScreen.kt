package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.moneda
import com.example.vapeon_movil.viewmodels.CarritoViewModel

@Composable
fun MetodoPagoScreen(
    carritoViewModel: CarritoViewModel? = null,
    volver: () -> Unit,
    efectivo: () -> Unit,
    tarjeta: () -> Unit,
    yape: () -> Unit
) {
    var metodoSeleccionado by remember { mutableStateOf("TARJETA") }
    val resumen = carritoViewModel?.resumen

    val subTotalVal = resumen?.subTotal ?: 150.0
    val deliveryVal = resumen?.delivery ?: 5.0
    val igvVal = resumen?.igv ?: 0.0
    val totalVal = resumen?.total ?: (subTotalVal + deliveryVal + igvVal)

    var montoEfectivo by remember(totalVal) { mutableStateOf(String.format(java.util.Locale.ROOT, "%.2f", totalVal + 50.0)) }
    val montoNum = montoEfectivo.toDoubleOrNull() ?: totalVal
    val vueltoVal = (montoNum - totalVal).coerceAtLeast(0.0)

    BackHandler(onBack = volver)

    Surface(
        color = VapeOnBackground,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ENCABEZADO SUPERIOR CON FLECHA Y TÍTULO
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
                    text = "¿Cómo desea pagar?",
                    color = VapeOnTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // OPCIÓN 1: EFECTIVO
            // ==========================================
            val esEfectivo = metodoSeleccionado == "EFECTIVO"
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (esEfectivo) VapeOnSelected else VapeOnCardNotSelected
                ),
                border = BorderStroke(1.dp, VapeOnCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { metodoSeleccionado = "EFECTIVO" }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EFECTIVO",
                                color = VapeOnTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pago con entrega",
                                color = VapeOnTextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        // Botón Selector / Radio Indicator
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = VapeOnCardBorder,
                                    shape = CircleShape
                                )
                                .background(if (esEfectivo) VapeOnCardBorder.copy(alpha = alfaAccesible(0.3f)) else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { metodoSeleccionado = "EFECTIVO" },
                            contentAlignment = Alignment.Center
                        ) {
                            if (esEfectivo) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(VapeOnCardBorder)
                                )
                            }
                        }
                    }

                    if (esEfectivo) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = VapeOnGold, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "¿Con cuánto va a pagar?",
                                color = VapeOnTextPrimary,
                                fontSize = 13.sp
                            )

                            OutlinedTextField(
                                value = montoEfectivo,
                                onValueChange = { montoEfectivo = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = VapeOnInputBackground,
                                    unfocusedContainerColor = VapeOnInputBackground,
                                    focusedBorderColor = VapeOnCardBorder,
                                    unfocusedBorderColor = VapeOnCardBorder,
                                    focusedTextColor = VapeOnTextPrimary,
                                    unfocusedTextColor = VapeOnTextPrimary,
                                    cursorColor = VapeOnCardBorder
                                ),
                                modifier = Modifier.width(120.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vuelto:",
                                color = VapeOnTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = moneda(vueltoVal),
                                color = VapeOnTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // OPCIÓN 2: YAPE QR
            // ==========================================
            val esYape = metodoSeleccionado == "YAPE"
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (esYape) VapeOnSelected else VapeOnCardNotSelected
                ),
                border = BorderStroke(1.dp, VapeOnCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { metodoSeleccionado = "YAPE" }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "YAPE QR",
                                color = VapeOnTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Escanea y paga al instante",
                                color = VapeOnTextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        // Botón Selector / Radio Indicator
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = VapeOnCardBorder,
                                    shape = CircleShape
                                )
                                .background(if (esYape) VapeOnCardBorder.copy(alpha = alfaAccesible(0.3f)) else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { metodoSeleccionado = "YAPE" },
                            contentAlignment = Alignment.Center
                        ) {
                            if (esYape) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(VapeOnCardBorder)
                                )
                            }
                        }
                    }

                    if (esYape) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = VapeOnGold, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Con esta opcion debera contar con un la app YAPE y poder escanear el QR.",
                            color = VapeOnTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // OPCIÓN 3: TARJETA
            // ==========================================
            val esTarjeta = metodoSeleccionado == "TARJETA"
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (esTarjeta) VapeOnSelected else VapeOnCardNotSelected
                ),
                border = BorderStroke(1.dp, VapeOnCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { metodoSeleccionado = "TARJETA" }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TARJETA",
                                color = VapeOnTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Credito o Debito",
                                color = VapeOnTextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        // Botón Selector / Radio Indicator
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = VapeOnCardBorder,
                                    shape = CircleShape
                                )
                                .background(if (esTarjeta) VapeOnCardBorder.copy(alpha = alfaAccesible(0.3f)) else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { metodoSeleccionado = "TARJETA" },
                            contentAlignment = Alignment.Center
                        ) {
                            if (esTarjeta) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(VapeOnCardBorder)
                                )
                            }
                        }
                    }

                    if (esTarjeta) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = VapeOnGold, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Con esta opcion debera verificar si es propietario de la tarjeta",
                            color = VapeOnTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // RESUMEN DEL PAGO Y BOTÓN PAGAR
            // ==========================================
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = VapeOnCardNotSelected),
                border = BorderStroke(1.dp, VapeOnCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Resumen del Pago",
                        color = VapeOnTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Sub Total", color = VapeOnTextSecondary, fontSize = 14.sp)
                        Text(text = moneda(subTotalVal), color = VapeOnTextPrimary, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Delivery", color = VapeOnTextSecondary, fontSize = 14.sp)
                        Text(text = moneda(deliveryVal), color = VapeOnTextPrimary, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "IGV (18%)", color = VapeOnTextSecondary, fontSize = 14.sp)
                        Text(text = moneda(igvVal), color = VapeOnTextPrimary, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = VapeOnRed, thickness = 2.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "TOTAL A PAGAR", color = VapeOnTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(text = moneda(totalVal), color = VapeOnTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            when (metodoSeleccionado) {
                                "EFECTIVO" -> efectivo()
                                "YAPE" -> yape()
                                "TARJETA" -> tarjeta()
                            }
                        },
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
                            text = "PAGAR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
