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
 * Todos son opcionales (nullable) para permitir actualizaciones parciales con PATCH.
 */
data class ProductoCampos(
    val nombre: FirestoreStringValue? = null,
    val marca: FirestoreStringValue? = null,
    val precio: FirestoreStringValue? = null,
    val stock: FirestoreStringValue? = null,
    val descripcion: FirestoreStringValue? = null
)

/** Respuesta de listado: array de documentos de producto */
data class ProductoListResponse(
    val documents: List<ProductoDocumento>?
)

/** Documento individual de producto tal como lo devuelve Firestore REST */
data class ProductoDocumento(
    val name: String,       // Path completo: "projects/.../documents/productos/{id}"
    val fields: ProductoCampos,
    val updateTime: String? = null
)
