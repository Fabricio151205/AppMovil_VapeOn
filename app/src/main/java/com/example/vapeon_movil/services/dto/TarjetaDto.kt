package com.example.vapeon_movil.services.dto

import com.example.vapeon_movil.entities.Tarjeta
import java.time.Instant

data class TarjetaContainer(val fields: Map<String, FirestoreValor>)
data class TarjetaDocumento(val name: String, val fields: Map<String, FirestoreValor>)
data class TarjetaConsulta(val structuredQuery: TarjetaConsultaEstructurada)
data class TarjetaConsultaEstructurada(val from: List<TarjetaColeccion>, val where: TarjetaFiltro)
data class TarjetaColeccion(val collectionId: String = "tarjetas")
data class TarjetaFiltro(val fieldFilter: TarjetaFiltroCampo)
data class TarjetaFiltroCampo(val field: TarjetaCampo, val op: String, val value: FirestoreValor)
data class TarjetaCampo(val fieldPath: String)
data class TarjetaConsultaRespuesta(val document: TarjetaDocumento? = null)

fun consultaTarjetasDelUsuario(usuarioId: String): TarjetaConsulta {
    require(usuarioId.isNotBlank()) { "Inicia sesión como cliente para consultar tus tarjetas." }
    return TarjetaConsulta(TarjetaConsultaEstructurada(listOf(TarjetaColeccion()),
        TarjetaFiltro(TarjetaFiltroCampo(TarjetaCampo("usuarioId"), "EQUAL",
            FirestoreValor(stringValue = usuarioId)))))
}

fun Tarjeta.aFirestore(): TarjetaContainer {
    fun texto(valor: String) = FirestoreValor(stringValue = valor)
    return TarjetaContainer(mapOf(
        "id" to texto(id), "usuarioId" to texto(usuarioId), "titular" to texto(titular),
        "marca" to texto(marca), "ultimos4" to texto(ultimos4),
        "vencimientoMes" to FirestoreValor(integerValue = vencimientoMes.toString()),
        "vencimientoAnio" to FirestoreValor(integerValue = vencimientoAnio.toString()),
        "fechaCreacion" to FirestoreValor(timestampValue = fechaCreacion), "modalidad" to texto(modalidad)
    ))
}

/** Comprueba el propietario antes de interpretar o exponer los demás campos. */
fun TarjetaDocumento.aTarjetaDelUsuario(usuarioId: String): Tarjeta {
    require(usuarioId.isNotBlank() && fields["usuarioId"]?.stringValue == usuarioId) {
        "No tienes acceso a esta tarjeta."
    }
    fun texto(campo: String) = requireNotNull(fields[campo]?.stringValue?.takeIf { it.isNotBlank() }) {
        "La tarjeta tiene datos incompletos."
    }
    val id = texto("id")
    require(id == name.substringAfterLast('/') && '/' !in id) { "ID de tarjeta inválido." }
    val mes = fields["vencimientoMes"]?.integerValue?.toIntOrNull()
    val anio = fields["vencimientoAnio"]?.integerValue?.toIntOrNull()
    require(mes != null && mes in 1..12 && anio != null && anio in 1..9999) { "Vencimiento inválido." }
    val ultimos = texto("ultimos4")
    require(Regex("[0-9]{4}").matches(ultimos)) { "Últimos dígitos inválidos." }
    val fecha = requireNotNull(fields["fechaCreacion"]?.timestampValue) { "Fecha de tarjeta inválida." }
    Instant.parse(fecha)
    require(texto("modalidad") == "SIMULADO") { "Modalidad de tarjeta inválida." }
    // Las tarjetas que vencieron después del registro siguen siendo visibles, pero no seleccionables.
    return Tarjeta(id, usuarioId, texto("titular"), texto("marca"), ultimos, mes, anio, fecha)
}
