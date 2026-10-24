package com.example.vapeon_movil.services.dto

/**
 * Primitivo utilizado para valores de texto en Firestore.
 */
data class FirestoreStringValue(
    val stringValue: String? = null
)

/**
 * Envoltorio flexible para leer o escribir campos que pueden ser texto o números en Firestore.
 */
data class FirestoreValue(
    val stringValue: String? = null,
    val doubleValue: Double? = null,
    val integerValue: String? = null
) {
    /** Extrae el valor como String seguro para la UI */
    fun asString(): String =
        stringValue
            ?: doubleValue?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
            ?: integerValue
            ?: ""

    /** Extrae el valor como Double para cálculos de precio */
    fun asDouble(): Double =
        doubleValue ?: integerValue?.toDoubleOrNull() ?: stringValue?.toDoubleOrNull() ?: 0.0

    /** Extrae el valor como Int para stock */
    fun asInt(): Int =
        integerValue?.toIntOrNull() ?: doubleValue?.toInt() ?: stringValue?.toIntOrNull() ?: 0
}

/** Unión de valores usada por los nuevos documentos de pedidos. */
data class FirestoreValor(
    val stringValue: String? = null,
    val integerValue: String? = null,
    val doubleValue: Double? = null,
    val timestampValue: String? = null,
    val arrayValue: FirestoreArray? = null,
    val mapValue: FirestoreMapa? = null
) {
    /** Extrae el valor como String. */
    fun asString(): String =
        stringValue
            ?: doubleValue?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
            ?: integerValue
            ?: ""

    /** Extrae el valor como Double. */
    fun asDouble(): Double =
        doubleValue ?: integerValue?.toDoubleOrNull() ?: stringValue?.toDoubleOrNull() ?: 0.0

    /** Extrae el valor como Int. */
    fun asInt(): Int =
        integerValue?.toIntOrNull() ?: doubleValue?.toInt() ?: stringValue?.toIntOrNull() ?: 0
}

/**
 * Representa un array de valores de Firestore.
 */
data class FirestoreArray(
    val values: List<FirestoreValor> = emptyList()
)

/**
 * Representa un mapa de campos de Firestore.
 */
data class FirestoreMapa(
    val fields: Map<String, FirestoreValor> = emptyMap()
)
