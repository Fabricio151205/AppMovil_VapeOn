package com.example.vapeon_movil.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Convierte imágenes de galería o cámara en data URI JPEG comprimido.
 *
 * Firestore limita cada documento a ~1 MiB, por eso la imagen se reduce a un
 * máximo de 800 px y se comprime antes de guardarla como texto base64.
 */
object ImagenSabor {
    private const val MAX_DIMENSION = 800
    private const val CALIDAD_INICIAL = 65
    private const val CALIDAD_MINIMA = 35
    private const val MAX_CARACTERES_BASE64 = 700_000

    const val PREFIJO_DATA_URI = "data:image/jpeg;base64,"

    /** Lee la imagen del [uri] y la devuelve lista para guardar en Firestore. */
    suspend fun aDataUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodificar(context, uri)
                ?: error("No se pudo leer la imagen seleccionada.")
            try {
                var calidad = CALIDAD_INICIAL
                var base64 = comprimir(bitmap, calidad)
                while (base64.length > MAX_CARACTERES_BASE64 && calidad > CALIDAD_MINIMA) {
                    calidad -= 10
                    base64 = comprimir(bitmap, calidad)
                }
                check(base64.length <= MAX_CARACTERES_BASE64) {
                    "La imagen sigue siendo demasiado grande. Elige una foto más pequeña."
                }
                PREFIJO_DATA_URI + base64
            } finally {
                bitmap.recycle()
            }
        }
    }

    /** Crea un archivo temporal en caché y devuelve el Uri seguro para la cámara. */
    fun crearUriTemporal(context: Context): Uri {
        val carpeta = File(context.cacheDir, "sabor_images").apply { mkdirs() }
        val archivo = File.createTempFile("sabor_", ".jpg", carpeta)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    }

    private fun decodificar(context: Context, uri: Uri): Bitmap? {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // Necesario para poder comprimir el bitmap después.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val ancho = info.size.width
            val alto = info.size.height
            val mayor = maxOf(ancho, alto)
            if (mayor > MAX_DIMENSION) {
                val factor = MAX_DIMENSION.toFloat() / mayor
                decoder.setTargetSize(
                    (ancho * factor).toInt().coerceAtLeast(1),
                    (alto * factor).toInt().coerceAtLeast(1)
                )
            }
        }
    }

    private fun comprimir(bitmap: Bitmap, calidad: Int): String {
        val salida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
        return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
    }
}
