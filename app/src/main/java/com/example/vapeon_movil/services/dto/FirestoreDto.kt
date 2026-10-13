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
