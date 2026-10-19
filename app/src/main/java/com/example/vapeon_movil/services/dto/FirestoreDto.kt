package com.example.vapeon_movil.services.dto

/**
 * Primitivo utilizado para valores de texto en Firestore.
 */
data class FirestoreStringValue(
    val stringValue: String? = null
)

/**
 * Valor flexible utilizado por Firestore.
 *
 * Permite leer diferentes tipos de datos:
 * - texto
 * - números decimales
 * - números enteros
 * - fechas
 * - arrays
 * - mapas
 */
data class FirestoreValue(
    val stringValue: String? = null,
    val integerValue: String? = null,
    val doubleValue: Double? = null,
    val timestampValue: String? = null,
    val arrayValue: FirestoreArray? = null,
    val mapValue: FirestoreMapa? = null
) {

    /**
     * Extrae el valor como String.
     */
    fun asString(): String =
        stringValue
            ?: doubleValue?.let {
                if (it % 1.0 == 0.0) {
                    it.toLong().toString()
                } else {
                    it.toString()
                }
            }
            ?: integerValue
            ?: ""

    /**
     * Extrae el valor como Double.
     */
    fun asDouble(): Double =
        doubleValue
            ?: integerValue?.toDoubleOrNull()
            ?: stringValue?.toDoubleOrNull()
            ?: 0.0

    /**
     * Extrae el valor como Int.
     */
    fun asInt(): Int =
        integerValue?.toIntOrNull()
            ?: doubleValue?.toInt()
            ?: stringValue?.toIntOrNull()
            ?: 0
}

/**
 * Alias para mantener compatibilidad con el código
 * que utiliza el nombre FirestoreValor.
 */
typealias FirestoreValor = FirestoreValue

/**
 * Representa un array de valores de Firestore.
 */
data class FirestoreArray(
    val values: List<FirestoreValor>
)

/**
 * Representa un mapa de campos de Firestore.
 */
data class FirestoreMapa(
    val fields: Map<String, FirestoreValor>
)