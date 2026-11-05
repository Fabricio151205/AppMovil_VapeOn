package com.example.vapeon_movil.components

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.ImagenSabor
import kotlinx.coroutines.launch

/**
 * Checkbox + selector de imagen para el formulario de sabores.
 *
 * Permite tomar una foto con la cámara o elegirla de la galería. La imagen se
 * comprime y se entrega al padre como data URI base64 listo para Firestore.
 */
@Composable
fun SelectorImagenSabor(
    imagenDataUri: String?,
    onImagenCambiada: (String?) -> Unit,
    procesandoImagen: Boolean,
    onProcesandoCambia: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    textoCheckbox: String = "Agregar imagen del sabor",
    textoAyuda: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activo by rememberSaveable { mutableStateOf(imagenDataUri != null) }
    var errorImagen by rememberSaveable { mutableStateOf("") }
    var uriCamara by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(imagenDataUri) {
        if (imagenDataUri != null) activo = true
    }

    fun aplicarImagen(uri: Uri) {
        scope.launch {
            onProcesandoCambia(true)
            errorImagen = ""
            ImagenSabor.aDataUri(context, uri)
                .onSuccess {
                    onImagenCambiada(it)
                    activo = true
                }
                .onFailure {
                    errorImagen = it.message ?: "No se pudo procesar la imagen."
                }
            onProcesandoCambia(false)
        }
    }

    val galeriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(::aplicarImagen) }

    val camaraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito -> if (exito) uriCamara?.let(::aplicarImagen) }

    fun abrirCamara() {
        runCatching { ImagenSabor.crearUriTemporal(context) }
            .onSuccess { uri ->
                uriCamara = uri
                camaraLauncher.launch(uri)
            }
            .onFailure { errorImagen = "No se pudo abrir la cámara." }
    }

    val permisoCamaraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) abrirCamara()
        else errorImagen = "Se necesita permiso de cámara para tomar la foto."
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = activo,
                onCheckedChange = { marcado ->
                    activo = marcado
                    errorImagen = ""
                    if (!marcado) onImagenCambiada(null)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = VapeOnGold,
                    uncheckedColor = VapeOnTextSecondary,
                    checkmarkColor = VapeOnBackground
                )
            )
            Text(
                text = textoCheckbox,
                color = VapeOnTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (activo) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (imagenDataUri != null) {
                        ImagenSaborView(
                            dataUri = imagenDataUri,
                            contentDescription = "Vista previa de la imagen del sabor",
                            maxDimensionPx = 800,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(VapeOnInputBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            if (procesandoImagen) {
                                CircularProgressIndicator(
                                    color = VapeOnGold,
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = VapeOnTextSecondary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Sin imagen seleccionada",
                                        color = VapeOnTextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                galeriaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !procesandoImagen,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, VapeOnGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VapeOnGold),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galería", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { permisoCamaraLauncher.launch(Manifest.permission.CAMERA) },
                            enabled = !procesandoImagen,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, VapeOnGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VapeOnGold),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cámara", fontSize = 13.sp)
                        }
                    }

                    if (imagenDataUri != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                onImagenCambiada(null)
                                errorImagen = ""
                            },
                            enabled = !procesandoImagen
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = VapeOnRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Quitar imagen", color = VapeOnRed, fontSize = 13.sp)
                        }
                    }

                    if (procesandoImagen) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Procesando imagen...",
                            color = VapeOnGold,
                            fontSize = 13.sp
                        )
                    }

                    if (errorImagen.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorImagen,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (textoAyuda != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = textoAyuda,
                            color = VapeOnTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
