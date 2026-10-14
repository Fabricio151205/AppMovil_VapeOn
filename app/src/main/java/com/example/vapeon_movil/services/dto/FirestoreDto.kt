package com.example.vapeon_movil.services.dto

/**
 * Primitivo compartido por todos los DTOs que se comunican con la REST API de Firestore.
 *
 * Soporta valores de texto ("stringValue"), números decimales ("doubleValue") y enteros ("integerValue").
 */
data class FirestoreStringValue(
    val stringValue: String? = null
)

<<<<<<< HEAD
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
=======
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
>>>>>>> 3b66a1336bdbf532f309b511ba726a497abce3d9
