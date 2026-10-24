package com.example.vapeon_movil.services.dto

import com.example.vapeon_movil.entities.SaborItem

/**
 * DTO para la transferencia de datos de la entidad Sabor con Firestore REST API.
 */
data class SaborCampos(
    val nombre: FirestoreValue? = null,
    val marca: FirestoreValue? = null,
    val stock: FirestoreValue? = null
)

data class SaborDocumento(
    val name: String? = null,
    val fields: SaborCampos? = null,
    val createTime: String? = null,
    val updateTime: String? = null
) {
    fun toEntity(): SaborItem {
        val docId = name?.substringAfterLast("/") ?: ""
        return SaborItem(
            id = docId,
            nombre = fields?.nombre?.asString() ?: "",
            marca = fields?.marca?.asString() ?: "",
            stock = fields?.stock?.asInt() ?: 0
        )
    }
}

data class SaborContainer(
    val fields: SaborCampos
) {
    companion object {
        fun fromEntity(saborItem: SaborItem): SaborContainer {
            return SaborContainer(
                fields = SaborCampos(
                    nombre = FirestoreValue(stringValue = saborItem.nombre),
                    marca = FirestoreValue(stringValue = saborItem.marca),
                    stock = FirestoreValue(integerValue = saborItem.stock.toString())
                )
            )
        }
    }
}

data class SaborListResponse(
    val documents: List<SaborDocumento>? = emptyList(),
    val nextPageToken: String? = null
)
