package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AccesibilidadScreen(volver: () -> Unit) {
    val controlador = LocalControladorAccesibilidad.current
    val ajustes = controlador?.ajustes ?: LocalAjustesAccesibilidad.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun actualizar(nuevo: AjustesAccesibilidad) {
        controlador?.actualizar(nuevo)
    }

    BackHandler(onBack = volver)

    VapeON_MovilTheme {
        Scaffold(
            containerColor = VapeOnBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                EncabezadoAccesibilidad(volver = volver)

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Accesibilidad",
                    color = VapeOnTextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Personaliza la apariencia de VAPEON",
                    color = VapeOnTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                VistaPreviaAccesibilidad(ajustes = ajustes)

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "TAMAÑO DEL TEXTO",
                    color = VapeOnTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ajustes.tamanoTexto.etiqueta,
                                color = VapeOnGold,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(ajustes.tamanoTexto.escala * 100).roundToInt()}%",
                                color = VapeOnTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotonTamano(
                                texto = "A-",
                                descripcion = "Reducir tamaño de texto",
                                habilitado = ajustes.tamanoTexto != TamanoTexto.PEQUENO,
                                onClick = {
                                    val indice = (ajustes.tamanoTexto.ordinal - 1).coerceAtLeast(0)
                                    actualizar(ajustes.copy(tamanoTexto = TamanoTexto.values()[indice]))
                                }
                            )
                            Slider(
                                value = ajustes.tamanoTexto.ordinal.toFloat(),
                                onValueChange = { nuevo ->
                                    val indice = nuevo.roundToInt().coerceIn(0, TamanoTexto.values().lastIndex)
                                    actualizar(ajustes.copy(tamanoTexto = TamanoTexto.values()[indice]))
                                },
                                valueRange = 0f..TamanoTexto.values().lastIndex.toFloat(),
                                steps = TamanoTexto.values().size - 2,
                                colors = SliderDefaults.colors(
                                    thumbColor = VapeOnGold,
                                    activeTrackColor = VapeOnGold,
                                    inactiveTrackColor = VapeOnCardBorder,
                                    activeTickColor = VapeOnBackground,
                                    inactiveTickColor = VapeOnTextMuted
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            BotonTamano(
                                texto = "A+",
                                descripcion = "Aumentar tamaño de texto",
                                habilitado = ajustes.tamanoTexto != TamanoTexto.GRANDE,
                                onClick = {
                                    val indice = (ajustes.tamanoTexto.ordinal + 1)
                                        .coerceAtMost(TamanoTexto.values().lastIndex)
                                    actualizar(ajustes.copy(tamanoTexto = TamanoTexto.values()[indice]))
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TamanoTexto.values().forEach { opcion ->
                                val seleccionado = ajustes.tamanoTexto == opcion
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (seleccionado) VapeOnGold else VapeOnSurfaceVariant,
                                    border = if (seleccionado) null else BorderStroke(1.dp, VapeOnCardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { actualizar(ajustes.copy(tamanoTexto = opcion)) }
                                ) {
                                    Text(
                                        text = opcion.etiqueta,
                                        color = if (seleccionado) VapeOnBackground else VapeOnTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "OPCIONES DE VISUALIZACIÓN",
                    color = VapeOnTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OpcionAccesibilidad(
                            icono = Icons.Default.TextFields,
                            titulo = "Texto en negrita",
                            descripcion = "Aumenta el grosor de todas las letras.",
                            activo = ajustes.textoNegrita,
                            onCambiar = { actualizar(ajustes.copy(textoNegrita = it)) }
                        )
                        HorizontalDivider(color = VapeOnDivider)
                        OpcionAccesibilidad(
                            icono = Icons.Default.Contrast,
                            titulo = "Contraste alto",
                            descripcion = "Refuerza la diferencia entre textos y fondos.",
                            activo = ajustes.altoContraste,
                            onCambiar = { actualizar(ajustes.copy(altoContraste = it)) }
                        )
                        HorizontalDivider(color = VapeOnDivider)
                        OpcionAccesibilidad(
                            icono = Icons.Default.Layers,
                            titulo = "Reducir transparencia",
                            descripcion = "Muestra los elementos con colores sólidos.",
                            activo = ajustes.reducirTransparencia,
                            onCambiar = { actualizar(ajustes.copy(reducirTransparencia = it)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        controlador?.restaurarPredeterminados()
                        scope.launch {
                            snackbarHostState.showSnackbar("Valores predeterminados restaurados")
                        }
                    },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, VapeOnGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VapeOnGold),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = VapeOnGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESTAURAR VALORES PREDETERMINADOS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.3.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Los cambios se guardan automáticamente en este dispositivo.",
                    color = VapeOnTextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun EncabezadoAccesibilidad(volver: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = volver) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver al perfil",
                tint = VapeOnTextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "VAPEON",
            color = VapeOnGold,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(VapeOnRed),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccessibilityNew,
                contentDescription = "Accesibilidad",
                tint = VapeOnTextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun VistaPreviaAccesibilidad(ajustes: AjustesAccesibilidad) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FormatSize,
                    contentDescription = null,
                    tint = VapeOnGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VISTA PREVIA",
                    color = VapeOnGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "VapeON se adapta a ti",
                color = VapeOnTextPrimary,
                fontSize = 18.sp,
                fontWeight = if (ajustes.textoNegrita) FontWeight.ExtraBold else FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Así se verán los textos de la aplicación con la configuración actual.",
                color = VapeOnTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            // Muestra que reacciona a "Reducir transparencia": 35 % de alfa por
            // defecto y opaca cuando el ajuste está activo.
            Surface(
                shape = RoundedCornerShape(50),
                color = VapeOnGold.copy(alpha = alfaAccesible(0.35f))
            ) {
                Text(
                    text = "Muestra de transparencia",
                    color = VapeOnTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun BotonTamano(
    texto: String,
    descripcion: String,
    habilitado: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        border = BorderStroke(
            1.dp,
            if (habilitado) VapeOnGold else VapeOnCardBorder
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = VapeOnGold,
            disabledContentColor = VapeOnTextMuted
        ),
        modifier = Modifier
            .width(52.dp)
            .height(42.dp)
    ) {
        Text(
            text = texto,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OpcionAccesibilidad(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    activo: Boolean,
    onCambiar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCambiar(!activo) }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VapeOnSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = VapeOnGold,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                color = VapeOnTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = descripcion,
                color = VapeOnTextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = activo,
            onCheckedChange = onCambiar,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VapeOnBackground,
                checkedTrackColor = VapeOnGold,
                uncheckedThumbColor = VapeOnTextSecondary,
                uncheckedTrackColor = VapeOnCardBorder,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
