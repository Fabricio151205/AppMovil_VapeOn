package com.example.vapeon_movil.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.vapeon_movil.ui.theme.VapeOnInputBackground
import com.example.vapeon_movil.ui.theme.VapeOnTextSecondary
import com.example.vapeon_movil.utils.ImagenSabor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Muestra una imagen de sabor almacenada como data URI base64.
 *
 * Decodifica con muestreo ([maxDimensionPx]) para que las miniaturas del listado
 * no consuman memoria de más.
 */
@Composable
fun ImagenSaborView(
    dataUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    maxDimensionPx: Int = 512
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, dataUri, maxDimensionPx) {
        value = withContext(Dispatchers.IO) {
            dataUri?.let { decodificarDataUri(it, maxDimensionPx) }
        }
    }
    val actual = bitmap
    if (actual != null) {
        Image(
            bitmap = actual,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(VapeOnInputBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = contentDescription,
                tint = VapeOnTextSecondary
            )
        }
    }
}

private fun decodificarDataUri(dataUri: String, maxDimensionPx: Int): ImageBitmap? {
    if (!dataUri.startsWith(ImagenSabor.PREFIJO_DATA_URI)) return null
    val base64 = dataUri.removePrefix(ImagenSabor.PREFIJO_DATA_URI)
    val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull() ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val opciones = BitmapFactory.Options().apply {
        inSampleSize = calcularSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx)
    }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones) ?: return null
    return bitmap.asImageBitmap()
}

private fun calcularSampleSize(ancho: Int, alto: Int, maxDimensionPx: Int): Int {
    var sample = 1
    var mayor = maxOf(ancho, alto)
    while (mayor / 2 >= maxDimensionPx) {
        mayor /= 2
        sample *= 2
    }
    return sample
}
