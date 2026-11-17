package com.example.vapeon_movil.services.dto

import com.example.vapeon_movil.entities.Favorito
import com.example.vapeon_movil.entities.PopularidadProducto
import java.time.Instant

data class FavoritoContainer(val fields: Map<String, FirestoreValor>)
data class FavoritoDocumento(val name: String, val fields: Map<String, FirestoreValor>)
data class FavoritoConsulta(val structuredQuery: FavoritoConsultaEstructurada)
data class FavoritoConsultaEstructurada(val from: List<FavoritoColeccion>, val where: FavoritoFiltro? = null)
data class FavoritoColeccion(val collectionId: String = "favoritos")
data class FavoritoFiltro(val fieldFilter: FavoritoFiltroCampo)
data class FavoritoFiltroCampo(val field: FavoritoCampo, val op: String, val value: FirestoreValor)
data class FavoritoCampo(val fieldPath: String)
data class FavoritoConsultaRespuesta(val document: FavoritoDocumento? = null)

fun consultaFavoritosDelUsuario(usuarioId: String): FavoritoConsulta {
    require(usuarioId.isNotBlank()) { "Inicia sesión como cliente para consultar tus favoritos." }
    return FavoritoConsulta(
        FavoritoConsultaEstructurada(
            from = listOf(FavoritoColeccion()),
            where = FavoritoFiltro(
                FavoritoFiltroCampo(
                    FavoritoCampo("usuarioId"),
                    "EQUAL",
                    FirestoreValor(stringValue = usuarioId)
                )
            )
        )
    )
}

/** Consulta administrativa sin filtro por usuario; el conteo se agrega por producto. */
fun consultaTodosLosFavoritos(): FavoritoConsulta = FavoritoConsulta(
    FavoritoConsultaEstructurada(from = listOf(FavoritoColeccion()))
)

/**
 * Extrae solo el producto del favorito. No lee ni expone `usuarioId`.
 * Los documentos sin producto válido se descartan durante la agregación.
 */
fun FavoritoDocumento.productoIdParaRanking(): String {
    val productoId = requireNotNull(fields["productoId"]?.stringValue) {
        "El favorito no tiene un producto válido."
    }.trim()
    require(productoId.isNotBlank() && '/' !in productoId) {
        "El favorito no tiene un producto válido."
    }
    return productoId
}

data class RankingFavoritos(
    val popularidad: List<PopularidadProducto>,
    val invalidos: Int
)

/** Agrupa los documentos por producto; el resultado no conserva identificadores de clientes. */
fun List<FavoritoDocumento>.agregarPorProducto(): RankingFavoritos {
    var invalidos = 0
    val conteo = mutableMapOf<String, Int>()
    forEach { documento ->
        try {
            val productoId = documento.productoIdParaRanking()
            conteo[productoId] = (conteo[productoId] ?: 0) + 1
        } catch (_: Exception) {
            invalidos++
        }
    }
    val popularidad = conteo.map { (productoId, favoritos) ->
        PopularidadProducto(productoId, favoritos)
    }.sortedWith(
        compareByDescending<PopularidadProducto> { it.favoritos }.thenBy { it.productoId }
    )
    return RankingFavoritos(popularidad, invalidos)
}

fun Favorito.aFirestore(): FavoritoContainer {
    val campos = mapOf(
        "id" to FirestoreValor(stringValue = id),
        "usuarioId" to FirestoreValor(stringValue = usuarioId),
        "productoId" to FirestoreValor(stringValue = productoId),
        "fechaCreacion" to FirestoreValor(timestampValue = fechaCreacion)
    )
    return FavoritoContainer(campos)
}

/** Comprueba el propietario antes de interpretar o exponer los demás campos. */
fun FavoritoDocumento.aFavoritoDelUsuario(usuarioId: String): Favorito {
    require(usuarioId.isNotBlank() && fields["usuarioId"]?.stringValue == usuarioId) {
        "No tienes acceso a este favorito."
    }
    fun texto(campo: String): String = requireNotNull(fields[campo]?.stringValue) {
        "El favorito tiene datos incompletos."
    }
    val id = texto("id")
    require(id == name.substringAfterLast('/') && '/' !in id) { "ID de favorito inválido." }
    val productoId = texto("productoId").trim()
    require(productoId.isNotBlank()) { "El favorito tiene datos incompletos." }
    val fecha = requireNotNull(fields["fechaCreacion"]?.timestampValue) {
        "Fecha de favorito inválida."
    }
    require(runCatching { Instant.parse(fecha) }.isSuccess) { "Fecha de favorito inválida." }

    return Favorito(
        id = id,
        usuarioId = usuarioId,
        productoId = productoId,
        fechaCreacion = fecha
    )
}
