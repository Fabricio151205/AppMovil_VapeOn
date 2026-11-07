package com.example.vapeon_movil.ui

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.BuildConfig
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.componerDireccionLegible
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private sealed interface EstadoUbicacion {
    data object Cargando : EstadoUbicacion
    data class Listo(val coordenadas: LatLng) : EstadoUbicacion
    data object NoEncontrada : EstadoUbicacion
}

/**
 * Vista de mapa a pantalla completa para una dirección guardada.
 *
 * La dirección se convierte a coordenadas con [Geocoder] y se muestra con el
 * SDK de Google Maps dentro de la misma aplicación, sin abrir apps externas.
 */
@Composable
fun VerUbicacionScreen(
    direccion: String,
    volver: () -> Unit,
    latitud: Double? = null,
    longitud: Double? = null
) {
    val context = LocalContext.current
    var reintento by remember { mutableIntStateOf(0) }
    val coordenadasGuardadas = remember(latitud, longitud) {
        if (latitud != null && longitud != null &&
            latitud.isFinite() && longitud.isFinite() &&
            latitud in -90.0..90.0 && longitud in -180.0..180.0
        ) LatLng(latitud, longitud) else null
    }
    var estado by remember(direccion, coordenadasGuardadas) {
        mutableStateOf<EstadoUbicacion>(
            coordenadasGuardadas?.let { EstadoUbicacion.Listo(it) } ?: EstadoUbicacion.Cargando
        )
    }

    LaunchedEffect(direccion, coordenadasGuardadas, reintento) {
        val guardadas = coordenadasGuardadas
        if (guardadas != null) {
            estado = EstadoUbicacion.Listo(guardadas)
            return@LaunchedEffect
        }
        estado = EstadoUbicacion.Cargando
        val coordenadas = geocodificarDireccion(context, direccion)
        estado = if (coordenadas != null) {
            EstadoUbicacion.Listo(coordenadas)
        } else {
            EstadoUbicacion.NoEncontrada
        }
    }

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
                        text = "Ubicación de entrega",
                        color = VapeOnTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = direccion,
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (val actual = estado) {
                    EstadoUbicacion.Cargando -> CargandoUbicacion()
                    EstadoUbicacion.NoEncontrada -> UbicacionNoEncontrada(
                        direccion = direccion,
                        onReintentar = { reintento++ }
                    )
                    is EstadoUbicacion.Listo -> {
                        if (BuildConfig.MAPS_API_KEY_CONFIGURED) {
                            MapaUbicacion(
                                coordenadas = actual.coordenadas,
                                direccion = direccion,
                                exacta = coordenadasGuardadas != null
                            )
                        } else {
                            FaltaApiKey()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapaUbicacion(
    coordenadas: LatLng,
    direccion: String,
    exacta: Boolean
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(coordenadas, 16f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = MarkerState(position = coordenadas),
                title = direccion,
                snippet = "Dirección de entrega"
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
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
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = VapeOnGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = direccion,
                        color = VapeOnTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (exacta) "Ubicación exacta confirmada" else "Vista aproximada de la ubicación",
                        color = VapeOnTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CargandoUbicacion() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = VapeOnGold)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Buscando la ubicación…",
                color = VapeOnTextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun UbicacionNoEncontrada(
    direccion: String,
    onReintentar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = null,
                tint = VapeOnTextSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No pudimos ubicar esta dirección",
                color = VapeOnTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Revisa que \"$direccion\" esté completa e inténtalo de nuevo.",
                color = VapeOnTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onReintentar,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VapeOnRed,
                    contentColor = VapeOnTextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Reintentar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun FaltaApiKey() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Configura Google Maps",
                color = VapeOnTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Agrega tu API key en el archivo local.properties del proyecto:",
                color = VapeOnTextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(VapeOnInputBackground)
                    .padding(12.dp)
            ) {
                Text(
                    text = "MAPS_API_KEY=TU_CLAVE",
                    color = VapeOnTextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Habilita \"Maps SDK for Android\" en Google Cloud Console y vuelve a compilar la app.",
                color = VapeOnTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Suppress("DEPRECATION")
internal suspend fun geocodificarDireccion(
    context: Context,
    direccion: String
): LatLng? = withContext(Dispatchers.IO) {
    if (direccion.isBlank() || !Geocoder.isPresent()) return@withContext null

    val geocoder = Geocoder(context, Locale("es", "PE"))
    val consultas = listOf(
        "$direccion, Lima, Perú",
        "$direccion, Perú",
        direccion
    ).distinct()

    for (consulta in consultas) {
        val resultado = runCatching {
            geocoder.getFromLocationName(consulta, 1)
        }.getOrNull()?.firstOrNull()

        if (resultado != null) {
            return@withContext LatLng(resultado.latitude, resultado.longitude)
        }
    }
    null
}

/**
 * Traduce un punto del mapa a un texto de calle legible.
 *
 * Se usa cuando el cliente toca el mapa para confirmar el punto exacto: el
 * nombre de la vía resultante reemplaza el texto escrito en el formulario.
 */
@Suppress("DEPRECATION")
internal suspend fun geocodificarCoordenadas(
    context: Context,
    latitud: Double,
    longitud: Double
): String? = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext null

    val geocoder = Geocoder(context, Locale("es", "PE"))
    val resultado = runCatching {
        geocoder.getFromLocation(latitud, longitud, 1)
    }.getOrNull()?.firstOrNull() ?: return@withContext null

    componerDireccionLegible(
        lineaPrincipal = resultado.getAddressLine(0),
        calle = resultado.thoroughfare,
        numero = resultado.subThoroughfare,
        distrito = resultado.subLocality ?: resultado.locality,
        ciudad = resultado.locality ?: resultado.subAdminArea ?: resultado.adminArea
    )
}
