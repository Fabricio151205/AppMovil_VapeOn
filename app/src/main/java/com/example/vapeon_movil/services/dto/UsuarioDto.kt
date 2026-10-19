package com.example.vapeon_movil.services.dto

// ─────────────────────────────────────────────────────────────────────────────
// DTOs de transporte para la colección "usuarios" de Firestore REST API
// ─────────────────────────────────────────────────────────────────────────────

/** Envuelve los campos del usuario para enviarlos en POST/PATCH a Firestore */
data class UsuarioContainer(
    val fields: UsuarioCampos
)

/** Campos del documento "usuario" en Firestore (campos opcionales para evitar NullPointerException) */
data class UsuarioCampos(
    val nombre: FirestoreStringValue? = null,
    val apellido: FirestoreStringValue? = null,
    val telefono: FirestoreStringValue? = null,
    val fechaNacimiento: FirestoreStringValue? = null,
    val correo: FirestoreStringValue? = null,
    val password: FirestoreStringValue? = null,
    val rol: FirestoreStringValue? = null
)

/** Respuesta de listado: array de documentos de usuario */
data class UsuarioListResponse(
    val documents: List<UsuarioDocumento>?,
    val nextPageToken: String? = null
)

/** Documento individual de usuario tal como lo devuelve Firestore REST */
data class UsuarioDocumento(
    val name: String,       // Path completo: "projects/.../documents/usuarios/{id}"
    val fields: UsuarioCampos
)
