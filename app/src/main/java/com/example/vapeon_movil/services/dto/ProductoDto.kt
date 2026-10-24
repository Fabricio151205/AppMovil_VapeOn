package com.example.vapeon_movil.services.dto

// ─────────────────────────────────────────────────────────────────────────────
// DTO de transporte para la colección "productos" de Firestore REST API
// ─────────────────────────────────────────────────────────────────────────────

/** Envuelve los campos del producto para enviarlos en POST/PATCH a Firestore */
data class ProductoContainer(
    val fields: ProductoCampos
)

/**
 * Campos del documento "producto" en Firestore.
 * Todos son opcionales (nullable) y usan FirestoreValue para soportar tanto números como texto.
 */
data class ProductoCampos(
    val nombre: FirestoreValue? = null,
    val marca: FirestoreValue? = null,
    val precio: FirestoreValue? = null,
    val stock: FirestoreValue? = null,
    val descripcion: FirestoreValue? = null,
    val sabor: FirestoreValue? = null,
    val stockPorSabor: FirestoreValor? = null
)

fun Map<String, Int>.aFirestoreMapa(): FirestoreValor {
    val campos = this.mapValues { (_, cantidad) ->
        // 🟢 Únicamente integerValue dentro de mapValue para cumplir con la API REST de Firestore
        FirestoreValor(integerValue = cantidad.toString())
    }
    return FirestoreValor(mapValue = FirestoreMapa(fields = campos))
}

fun FirestoreValor?.aStockMap(): Map<String, Int> {
    if (this == null) return emptyMap()
    val mapa = this.mapValue ?: return emptyMap()
    if (mapa.fields.isEmpty()) return emptyMap()
    return mapa.fields.mapValues { (_, valor) ->
        valor.asInt()
    }
}

/** Respuesta de listado: array de documentos de producto */
data class ProductoListResponse(
    val documents: List<ProductoDocumento>?,
    val nextPageToken: String? = null
)

/** Documento individual de producto tal como lo devuelve Firestore REST */
data class ProductoDocumento(
    val name: String,       // Path completo: "projects/.../documents/productos/{id}"
    val fields: ProductoCampos,
    val updateTime: String? = null
)
