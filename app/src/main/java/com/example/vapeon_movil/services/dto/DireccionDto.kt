package com.example.vapeon_movil.services.dto

import com.example.vapeon_movil.entities.Direccion
import java.time.Instant

data class DireccionContainer(val fields: Map<String, FirestoreValor>)
data class DireccionDocumento(val name: String, val fields: Map<String, FirestoreValor>)
data class DireccionConsulta(val structuredQuery: DireccionConsultaEstructurada)
data class DireccionConsultaEstructurada(val from: List<DireccionColeccion>, val where: DireccionFiltro)
data class DireccionColeccion(val collectionId: String = "direcciones")
data class DireccionFiltro(val fieldFilter: DireccionFiltroCampo)
data class DireccionFiltroCampo(val field: DireccionCampo, val op: String, val value: FirestoreValor)
data class DireccionCampo(val fieldPath: String)
data class DireccionConsultaRespuesta(val document: DireccionDocumento? = null)

fun consultaDireccionesDelUsuario(usuarioId: String): DireccionConsulta {
    require(usuarioId.isNotBlank()) { "Inicia sesión como cliente para consultar tus direcciones." }
    return DireccionConsulta(
        DireccionConsultaEstructurada(
            from = listOf(DireccionColeccion()),
            where = DireccionFiltro(
                DireccionFiltroCampo(
                    DireccionCampo("usuarioId"),
                    "EQUAL",
                    FirestoreValor(stringValue = usuarioId)
                )
            )
        )
    )
}

fun Direccion.aFirestore(): DireccionContainer {
    fun texto(valor: String) = FirestoreValor(stringValue = valor)
    val campos = mutableMapOf(
        "id" to texto(id),
        "usuarioId" to texto(usuarioId),
        "alias" to texto(alias),
        "direccion" to texto(direccion),
        "referencia" to texto(referencia),
        "fechaCreacion" to FirestoreValor(timestampValue = fechaCreacion)
    )
    latitud?.let { campos["latitud"] = FirestoreValor(doubleValue = it) }
    longitud?.let { campos["longitud"] = FirestoreValor(doubleValue = it) }
    return DireccionContainer(campos)
}

/** Comprueba el propietario antes de interpretar o exponer los demás campos. */
fun DireccionDocumento.aDireccionDelUsuario(usuarioId: String): Direccion {
    require(usuarioId.isNotBlank() && fields["usuarioId"]?.stringValue == usuarioId) {
        "No tienes acceso a esta dirección."
    }
    fun texto(campo: String): String = requireNotNull(fields[campo]?.stringValue) {
        "La dirección tiene datos incompletos."
    }
    val id = texto("id")
    require(id == name.substringAfterLast('/') && '/' !in id) { "ID de dirección inválido." }
    val alias = texto("alias").trim()
    val direccion = texto("direccion").trim()
    require(alias.isNotBlank() && direccion.isNotBlank()) { "La dirección tiene datos incompletos." }
    val fecha = requireNotNull(fields["fechaCreacion"]?.timestampValue) { "Fecha de dirección inválida." }
    Instant.parse(fecha)

    // Compatibilidad: las direcciones antiguas no tienen coordenadas.
    val latitud = fields["latitud"]?.let { it.doubleValue ?: it.integerValue?.toDouble() }
    val longitud = fields["longitud"]?.let { it.doubleValue ?: it.integerValue?.toDouble() }
    require((latitud == null) == (longitud == null)) { "La dirección tiene datos incompletos." }

    return Direccion(
        id = id,
        usuarioId = usuarioId,
        alias = alias,
        direccion = direccion,
        referencia = fields["referencia"]?.stringValue.orEmpty().trim(),
        fechaCreacion = fecha,
        latitud = latitud,
        longitud = longitud
    )
}
