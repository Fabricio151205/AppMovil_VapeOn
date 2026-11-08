package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.ui.theme.VapeOnBackground
import com.example.vapeon_movil.ui.theme.VapeOnCardBorder
import com.example.vapeon_movil.ui.theme.VapeOnCardNotSelected
import com.example.vapeon_movil.ui.theme.VapeOnError
import com.example.vapeon_movil.ui.theme.VapeOnGold
import com.example.vapeon_movil.ui.theme.VapeOnInputBackground
import com.example.vapeon_movil.ui.theme.VapeOnRed
import com.example.vapeon_movil.ui.theme.VapeOnSelected
import com.example.vapeon_movil.ui.theme.VapeOnTextPrimary
import com.example.vapeon_movil.ui.theme.VapeOnTextSecondary
import com.example.vapeon_movil.ui.theme.alfaAccesible
import com.example.vapeon_movil.utils.moneda
import com.example.vapeon_movil.viewmodels.CarritoViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Selección del método de pago.
 *
 * El monto de efectivo se ingresa aquí mismo. Al pulsar PAGAR con efectivo se
 * registra el pedido directamente: ya no se repite el formulario en otra vista.
 */
@Composable
fun MetodoPagoScreen(
    carritoViewModel: CarritoViewModel? = null,
    montoEfectivo: String,
    cambiarMonto: (String) -> Unit,
    error: String? = null,
    volver: () -> Unit,
    pagarEfectivo: (String) -> Unit,
    yape: () -> Unit,
    tarjeta: () -> Unit
) {
    var metodoSeleccionado by rememberSaveable { mutableStateOf("") }
    val resumen = carritoViewModel?.resumen

    val subTotalVal = resumen?.subTotal ?: 0.0
    val deliveryVal = resumen?.delivery ?: 0.0
    val totalVal = resumen?.total ?: (subTotalVal + deliveryVal)

    // Sugerir el total exacto la primera vez, sin pisar lo que escriba el cliente.
    LaunchedEffect(totalVal) {
        if (montoEfectivo.isBlank() && totalVal > 0) {
            cambiarMonto(String.format(Locale.ROOT, "%.2f", totalVal))
        }
    }

    val montoNum = Pedido.leerMonto(montoEfectivo)?.setScale(2, RoundingMode.HALF_UP)
    val totalRedondeado = BigDecimal.valueOf(totalVal).setScale(2, RoundingMode.HALF_UP)
    val montoValido = montoNum != null && montoNum >= totalRedondeado
    val vueltoVal = if (montoValido) montoNum!!.subtract(totalRedondeado) else null
    val esEfectivo = metodoSeleccionado == "EFECTIVO"
    val puedePagar = metodoSeleccionado.isNotBlank() && (!esEfectivo || montoValido)

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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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

            Spacer(modifier = Modifier.height(12.dp))

            OpcionPago(
                titulo = "EFECTIVO",
                subtitulo = "Pago con entrega",
                seleccionado = esEfectivo,
                onClick = { metodoSeleccionado = "EFECTIVO" }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¿Con cuánto va a pagar?",
                        color = VapeOnTextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = montoEfectivo,
                        onValueChange = { nuevo ->
                            val limpio = nuevo.filter { it.isDigit() || it == '.' || it == ',' }.take(9)
                            cambiarMonto(limpio)
                        },
                        singleLine = true,
                        prefix = { Text("S/", color = VapeOnTextSecondary, fontSize = 12.sp) },
                        textStyle = TextStyle(
                            color = VapeOnTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = VapeOnInputBackground,
                            unfocusedContainerColor = VapeOnInputBackground,
                            focusedBorderColor = VapeOnGold,
                            unfocusedBorderColor = VapeOnCardBorder,
                            focusedTextColor = VapeOnTextPrimary,
                            unfocusedTextColor = VapeOnTextPrimary,
                            cursorColor = VapeOnGold
                        ),
                        modifier = Modifier.width(150.dp)
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
                        text = vueltoVal?.let { moneda(it) } ?: "—",
                        color = VapeOnGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (montoEfectivo.isNotBlank() && !montoValido) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "El monto ingresado es insuficiente.",
                        color = VapeOnError,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OpcionPago(
                titulo = "YAPE QR",
                subtitulo = "Escanea y paga al instante",
                seleccionado = metodoSeleccionado == "YAPE",
                onClick = { metodoSeleccionado = "YAPE" }
            ) {
                Text(
                    text = "Con esta opción deberá contar con la app YAPE y poder escanear el QR.",
                    color = VapeOnTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OpcionPago(
                titulo = "TARJETA",
                subtitulo = "Crédito o Débito",
                seleccionado = metodoSeleccionado == "TARJETA",
                onClick = { metodoSeleccionado = "TARJETA" }
            ) {
                Text(
                    text = "Con esta opción deberá verificar si es propietario de la tarjeta.",
                    color = VapeOnTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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

                    FilaResumen("Sub Total", moneda(subTotalVal))
                    Spacer(modifier = Modifier.height(8.dp))
                    FilaResumen("Delivery", moneda(deliveryVal))
                    Spacer(modifier = Modifier.height(8.dp))
                    FilaResumen("IGV (18%)", "Incluido")

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = VapeOnRed, thickness = 2.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TOTAL A PAGAR",
                            color = VapeOnTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = moneda(totalVal),
                            color = VapeOnTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    error?.takeIf { it.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = it, color = VapeOnError, fontSize = 12.sp, lineHeight = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            when (metodoSeleccionado) {
                                "EFECTIVO" -> pagarEfectivo(montoEfectivo)
                                "YAPE" -> yape()
                                "TARJETA" -> tarjeta()
                            }
                        },
                        enabled = puedePagar,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VapeOnRed,
                            contentColor = VapeOnTextPrimary,
                            disabledContainerColor = VapeOnRed.copy(alpha = 0.45f),
                            disabledContentColor = VapeOnTextPrimary.copy(alpha = 0.7f)
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

                    if (metodoSeleccionado.isBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selecciona un método de pago para continuar.",
                            color = VapeOnTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilaResumen(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = etiqueta, color = VapeOnTextSecondary, fontSize = 14.sp)
        Text(text = valor, color = VapeOnTextPrimary, fontSize = 14.sp)
    }
}

/** Tarjeta de método de pago con selector circular y contenido desplegable. */
@Composable
private fun OpcionPago(
    titulo: String,
    subtitulo: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) VapeOnSelected else VapeOnCardNotSelected
        ),
        border = BorderStroke(1.dp, if (seleccionado) VapeOnGold else VapeOnCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = seleccionado,
                        onClick = onClick,
                        role = Role.RadioButton
                    )
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        color = VapeOnTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitulo,
                        color = VapeOnTextSecondary,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (seleccionado) VapeOnGold else VapeOnCardBorder,
                            shape = CircleShape
                        )
                        .background(
                            if (seleccionado) VapeOnGold.copy(alpha = alfaAccesible(0.18f))
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (seleccionado) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(VapeOnGold)
                        )
                    }
                }
            }

            if (seleccionado) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp)
                ) {
                    HorizontalDivider(color = VapeOnGold.copy(alpha = 0.45f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    contenido()
                }
            }
        }
    }
}
