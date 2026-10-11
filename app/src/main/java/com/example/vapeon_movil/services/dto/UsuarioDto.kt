package com.example.vapeon_movil.services.dto

// ─────────────────────────────────────────────────────────────────────────────
// DTOs de transporte para la colección "usuarios" de Firestore REST API
// ─────────────────────────────────────────────────────────────────────────────

/** Envuelve los campos del usuario para enviarlos en POST/PATCH a Firestore */
data class UsuarioContainer(
    val fields: UsuarioCampos
)

/** Campos del documento "usuario" en Firestore */
data class UsuarioCampos(
    val nombre: FirestoreStringValue,
    val apellido: FirestoreStringValue,
    val telefono: FirestoreStringValue,
    val fechaNacimiento: FirestoreStringValue,
    val correo: FirestoreStringValue,
    val password: FirestoreStringValue,
    val rol: FirestoreStringValue
)

/** Respuesta de listado: array de documentos de usuario */
data class UsuarioListResponse(
    val documents: List<UsuarioDocumento>?
)

/** Documento individual de usuario tal como lo devuelve Firestore REST */
data class UsuarioDocumento(
    val name: String,       // Path completo: "projects/.../documents/usuarios/{id}"
    val fields: UsuarioCampos
)
