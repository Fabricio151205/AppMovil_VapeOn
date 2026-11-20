package com.example.vapeon_movil.services.dto

import com.example.vapeon_movil.entities.Inversionista
import java.time.Instant

internal val CAMPOS_INVERSIONISTA = listOf(
    "nombre", "tipoDocumento", "documento", "telefono", "correo", "observaciones"
)

data class InversionistaContainer(val fields: Map<String, FirestoreValor>)
data class InversionistaDocumento(
    val name: String,
    val fields: Map<String, FirestoreValor>,
    val updateTime: String
)
data class InversionistaPagina(
    val documents: List<InversionistaDocumento>? = null,
    val nextPageToken: String? = null
)

fun Inversionista.aFirestoreInversionista(): InversionistaContainer = InversionistaContainer(mapOf(
    "nombre" to FirestoreValor(stringValue = nombre),
    "tipoDocumento" to FirestoreValor(stringValue = tipoDocumento),
    "documento" to FirestoreValor(stringValue = documento),
    "telefono" to FirestoreValor(stringValue = telefono),
    "correo" to FirestoreValor(stringValue = correo),
    "observaciones" to FirestoreValor(stringValue = observaciones),
    "fechaRegistro" to FirestoreValor(timestampValue = fechaRegistro)
))

fun InversionistaDocumento.aInversionista(): Inversionista {
    fun texto(campo: String) = fields[campo]?.stringValue.orEmpty()
    val id = name.substringAfterLast('/')
    require(id.isNotBlank() && texto("nombre").isNotBlank()) { "Inversionista incompleto." }
    require(texto("tipoDocumento") in listOf("DNI", "RUC") && texto("documento").isNotBlank()) {
        "Documento de inversionista incompleto."
    }
    val fecha = requireNotNull(fields["fechaRegistro"]?.timestampValue) { "Fecha de registro ausente." }
    Instant.parse(fecha)
    require(updateTime.isNotBlank()) { "No se pudo verificar la versión del registro." }
    return Inversionista(id, texto("nombre"), texto("tipoDocumento"), texto("documento"),
        texto("telefono"), texto("correo"), texto("observaciones"), fecha)
}
