package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.BuildConfig
import com.example.vapeon_movil.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.withTimeoutOrNull

/** Centro de Lima usado solo cuando el texto no se puede geocodificar. */
private val CENTRO_POR_DEFECTO = LatLng(-12.0464, -77.0428)

/**
 * Selector del punto exacto de entrega.
 *
 * Abre el mapa centrado en la dirección escrita por el cliente (geocodificada)
 * y permite tocar cualquier punto para corregir la ubicación. Al confirmar
 * devuelve las coordenadas exactas al formulario, que las guarda junto con la
 * dirección. Se muestra como capa a pantalla completa para no perder los datos
 * que el cliente ya escribió.
 */
@Composable
fun SeleccionarUbicacionScreen(
    direccion: String,
    latitudInicial: Double?,
    longitudInicial: Double?,
    onConfirmar: (latitud: Double, longitud: Double, direccionDetectada: String?) -> Unit,
    volver: () -> Unit
) {
    val context = LocalContext.current
    val inicial = remember(latitudInicial, longitudInicial) {
        if (latitudInicial != null && longitudInicial != null &&
            latitudInicial.isFinite() && longitudInicial.isFinite() &&
            latitudInicial in -90.0..90.0 && longitudInicial in -180.0..180.0
        ) LatLng(latitudInicial, longitudInicial) else null
    }

    var latitud by rememberSaveable { mutableStateOf(inicial?.latitude) }
    var longitud by rememberSaveable { mutableStateOf(inicial?.longitude) }
    var buscando by rememberSaveable { mutableStateOf(false) }
    var sinResultado by rememberSaveable { mutableStateOf(false) }
    var geocodificacionHecha by rememberSaveable { mutableStateOf(inicial != null) }
    var reintento by remember { mutableIntStateOf(0) }
    var resolviendoDireccion by rememberSaveable { mutableStateOf(false) }
    var direccionResuelta by rememberSaveable { mutableStateOf<String?>(null) }

    val seleccion = if (latitud != null && longitud != null) {
        LatLng(latitud!!, longitud!!)
    } else null

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            seleccion ?: CENTRO_POR_DEFECTO,
            if (seleccion != null) 17f else 12f
        )
    }

    LaunchedEffect(direccion, reintento) {
        if (geocodificacionHecha && reintento == 0) return@LaunchedEffect
        if (direccion.isBlank()) {
            buscando = false
            sinResultado = false
            return@LaunchedEffect
        }
        buscando = true
        sinResultado = false
        val encontrada = geocodificarDireccion(context, direccion)
        if (encontrada != null) {
            latitud = encontrada.latitude
            longitud = encontrada.longitude
            geocodificacionHecha = true
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(encontrada, 17f))
        } else {
            sinResultado = true
        }
        buscando = false
    }

    // Al mover el punto, se busca el nombre de la calle para actualizar el formulario.
    LaunchedEffect(latitud, longitud) {
        val la = latitud
        val lo = longitud
        if (la == null || lo == null) {
            direccionResuelta = null
            resolviendoDireccion = false
            return@LaunchedEffect
        }
        resolviendoDireccion = true
        direccionResuelta = withTimeoutOrNull(8_000) {
            geocodificarCoordenadas(context, la, lo)
        }
        resolviendoDireccion = false
    }

    BackHandler(onBack = volver)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VapeOnBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Confirma el punto exacto",
                        color = VapeOnTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = direccion.ifBlank { "Sin dirección escrita" },
                        color = VapeOnTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (!BuildConfig.MAPS_API_KEY_CONFIGURED) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        FaltaApiKey()
                    }
                } else {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapClick = { punto ->
                            latitud = punto.latitude
                            longitud = punto.longitude
                            sinResultado = false
                        }
                    ) {
                        seleccion?.let { punto ->
                            Marker(
                                state = remember(punto) { MarkerState(position = punto) },
                                title = "Punto de entrega",
                                snippet = direccion.ifBlank { "Ubicación seleccionada" }
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = VapeOnCardBackground.copy(alpha = 0.95f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (buscando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = VapeOnGold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = VapeOnGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    buscando -> "Buscando la dirección…"
                                    sinResultado -> "No encontramos la dirección"
                                    seleccion == null -> "Marca el punto de entrega"
                                    else -> "Punto marcado"
                                },
                                color = VapeOnTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = when {
                                    buscando -> "Espera un momento."
                                    sinResultado -> "Navega por el mapa y toca el lugar correcto."
                                    seleccion == null -> "Toca el mapa donde debe llegar el pedido."
                                    else -> "Toca otro punto si necesitas ajustarlo."
                                },
                                color = VapeOnTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (seleccion != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = VapeOnCardBackground.copy(alpha = 0.95f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Dirección del punto",
                                    color = VapeOnGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when {
                                        resolviendoDireccion -> "Buscando el nombre de la calle…"
                                        !direccionResuelta.isNullOrBlank() -> direccionResuelta!!
                                        else -> "No se pudo obtener el nombre. " +
                                            "Se usará el texto que escribiste."
                                    },
                                    color = VapeOnTextPrimary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (sinResultado && direccion.isNotBlank()) {
                        TextButton(
                            onClick = { reintento++ },
                            enabled = !buscando,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = VapeOnGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buscar la dirección otra vez", color = VapeOnGold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Button(
                        onClick = {
                            seleccion?.let { punto ->
                                onConfirmar(
                                    punto.latitude,
                                    punto.longitude,
                                    direccionResuelta?.takeIf { it.isNotBlank() }
                                )
                            }
                        },
                        enabled = seleccion != null && !buscando && !resolviendoDireccion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VapeOnRed,
                            contentColor = VapeOnTextPrimary,
                            disabledContainerColor = VapeOnRedDark,
                            disabledContentColor = VapeOnTextSecondary
                        )
                    ) {
                        Text(
                            "USAR ESTA UBICACIÓN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
