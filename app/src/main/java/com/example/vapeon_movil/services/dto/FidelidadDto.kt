package com.example.vapeon_movil.services.dto

import com.example.vapeon_movil.entities.*
import java.math.BigDecimal
import java.math.RoundingMode

data class FidelidadDocumento(
    val name: String,
    val fields: Map<String, FirestoreValor>,
    val createTime: String? = null,
    val updateTime: String? = null
)

data class FidelidadLista(
    val documents: List<FidelidadDocumento>? = null,
    val nextPageToken: String? = null
)

data class FidelidadPrecondicion(val exists: Boolean? = null, val updateTime: String? = null)
data class FidelidadMascara(val fieldPaths: List<String>)
data class FidelidadEscritura(
    val update: FidelidadDocumento,
    val currentDocument: FidelidadPrecondicion,
    val updateMask: FidelidadMascara? = null
)
data class FidelidadCommit(val writes: List<FidelidadEscritura>)
data class FidelidadResultadoEscritura(val updateTime: String? = null)
data class FidelidadCommitRespuesta(
    val commitTime: String,
    val writeResults: List<FidelidadResultadoEscritura> = emptyList()
)

private fun texto(valor: String) = FirestoreValor(stringValue = valor)
private fun entero(valor: Int) = FirestoreValor(integerValue = valor.toString())
private fun dinero(valor: BigDecimal) = FirestoreValor(doubleValue = valor.toDouble())

fun ConfiguracionFidelidad.aDocumento(ruta: String) = FidelidadDocumento(
    "$ruta/configuracionFidelidad/actual",
    mapOf(
        "productoId" to texto(productoId), "nombre" to texto(nombre), "marca" to texto(marca),
        "categoria" to texto(categoria), "precioReferencia" to dinero(precioReferencia),
        "configuradoPor" to texto(configuradoPor), "fechaConfiguracion" to texto(fechaConfiguracion)
    )
)

fun FidelidadCliente.aDocumento(ruta: String) = FidelidadDocumento(
    "$ruta/fidelidades/$usuarioId",
    mapOf(
        "usuarioId" to texto(usuarioId), "puntosActuales" to entero(puntosActuales),
        "numeroCiclo" to entero(numeroCiclo), "fechaActualizacion" to texto(fechaActualizacion)
    ) + listOfNotNull(recompensaPendienteId?.let { "recompensaPendienteId" to texto(it) }).toMap()
)

fun EventoFidelidad.aDocumento(ruta: String) = FidelidadDocumento(
    "$ruta/eventosFidelidad/$pedidoId",
    mapOf(
        "pedidoId" to texto(pedidoId), "usuarioId" to texto(usuarioId),
        "numeroCiclo" to entero(numeroCiclo), "fechaPedido" to texto(fechaPedido),
        "fechaAcreditacion" to texto(fechaAcreditacion), "resultado" to texto(resultado)
    )
)

fun RecompensaFidelidad.aDocumento(ruta: String) = FidelidadDocumento(
    "$ruta/recompensasFidelidad/$id",
    mapOf(
        "usuarioId" to texto(usuarioId), "numeroCiclo" to entero(numeroCiclo),
        "estado" to texto(estado.name), "fechaGeneracion" to texto(fechaGeneracion)
    ) + listOfNotNull(
        productoId?.let { "productoId" to texto(it) },
        nombreProducto?.let { "nombreProducto" to texto(it) },
        marcaProducto?.let { "marcaProducto" to texto(it) },
        categoriaProducto?.let { "categoriaProducto" to texto(it) },
        precioReferencia?.let { "precioReferencia" to dinero(it) },
        fechaEntrega?.let { "fechaEntrega" to texto(it) },
        entregadaPor?.let { "entregadaPor" to texto(it) }
    ).toMap()
)

private fun Map<String, FirestoreValor>.requerido(campo: String): String =
    get(campo)?.stringValue?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Documento de fidelidad incompleto: $campo")
private fun Map<String, FirestoreValor>.entero(campo: String): Int =
    get(campo)?.integerValue?.toIntOrNull()
        ?: throw IllegalArgumentException("Documento de fidelidad inválido: $campo")
private fun Map<String, FirestoreValor>.dinero(campo: String): BigDecimal =
    (get(campo)?.doubleValue?.takeIf { it.isFinite() }?.let { BigDecimal.valueOf(it) }
        ?: get(campo)?.integerValue?.toBigDecimalOrNull())?.setScale(2, RoundingMode.UNNECESSARY)
        ?: throw IllegalArgumentException("Documento de fidelidad inválido: $campo")

fun FidelidadDocumento.aConfiguracion() = ConfiguracionFidelidad(
    fields.requerido("productoId"), fields.requerido("nombre"), fields.requerido("marca"),
    fields["categoria"]?.stringValue.orEmpty(), fields.dinero("precioReferencia"),
    fields.requerido("configuradoPor"), fields.requerido("fechaConfiguracion"), updateTime
)

fun FidelidadDocumento.aFidelidad(): FidelidadCliente {
    val usuarioId = fields.requerido("usuarioId")
    require(name.substringAfterLast('/') == usuarioId)
    return FidelidadCliente(
        usuarioId, fields.entero("puntosActuales"), fields.entero("numeroCiclo"),
        fields["recompensaPendienteId"]?.stringValue?.takeIf { it.isNotBlank() },
        fields.requerido("fechaActualizacion"), updateTime
    )
}

fun FidelidadDocumento.aRecompensa() = RecompensaFidelidad(
    id = name.substringAfterLast('/'), usuarioId = fields.requerido("usuarioId"),
    numeroCiclo = fields.entero("numeroCiclo"),
    estado = EstadoRecompensaFidelidad.valueOf(fields.requerido("estado")),
    productoId = fields["productoId"]?.stringValue,
    nombreProducto = fields["nombreProducto"]?.stringValue,
    marcaProducto = fields["marcaProducto"]?.stringValue,
    categoriaProducto = fields["categoriaProducto"]?.stringValue,
    precioReferencia = fields["precioReferencia"]?.let { fields.dinero("precioReferencia") },
    fechaGeneracion = fields.requerido("fechaGeneracion"),
    fechaEntrega = fields["fechaEntrega"]?.stringValue,
    entregadaPor = fields["entregadaPor"]?.stringValue,
    updateTime = updateTime
)

internal const val RUTA_DOCUMENTOS_FIRESTORE =
    "projects/dbvapeon/databases/(default)/documents"
