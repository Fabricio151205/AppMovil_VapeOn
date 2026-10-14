package com.example.vapeon_movil.services.dto

/**
 * Primitivo compartido por todos los DTOs que se comunican con la REST API de Firestore.
 *
 * La API de Google Cloud Firestore envuelve cada valor de texto dentro de un objeto
 * con clave "stringValue":
 *
 * Ejemplo:
 * {
 *   "fields": {
 *     "nombre": { "stringValue": "Kit LifePod" }
 *   }
 * }
 */
data class FirestoreStringValue(
    val stringValue: String
)

/** Unión de valores usada por los nuevos documentos de pedidos. */
data class FirestoreValor(
    val stringValue: String? = null,
    val integerValue: String? = null,
    val doubleValue: Double? = null,
    val timestampValue: String? = null,
    val arrayValue: FirestoreArray? = null,
    val mapValue: FirestoreMapa? = null
)

data class FirestoreArray(val values: List<FirestoreValor>)
data class FirestoreMapa(val fields: Map<String, FirestoreValor>)
