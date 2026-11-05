package com.example.vapeon_movil.utils

import com.example.vapeon_movil.R

/**
 * Resuelve la imagen por defecto del producto cuando no hay una foto Base64 registrada en Firestore.
 */
fun resolverImagenProducto(nombreOContexto: String, saborSeleccionado: String?): Int {
    return R.drawable.logo_vapeon
}
