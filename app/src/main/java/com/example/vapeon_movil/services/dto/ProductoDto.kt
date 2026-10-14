package com.example.vapeon_movil.services.dto

// ─────────────────────────────────────────────────────────────────────────────
// DTOs de transporte para la colección "productos" de Firestore REST API
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
    val descripcion: FirestoreValue? = null
)

/** Respuesta de listado: array de documentos de producto */
data class ProductoListResponse(
    val documents: List<ProductoDocumento>?
)

/** Documento individual de producto tal como lo devuelve Firestore REST */
data class ProductoDocumento(
    val name: String,       // Path completo: "projects/.../documents/productos/{id}"
    val fields: ProductoCampos
)
