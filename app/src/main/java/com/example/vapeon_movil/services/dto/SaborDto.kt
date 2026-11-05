package com.example.vapeon_movil.services.dto

import com.example.vapeon_movil.entities.SaborItem

/**
 * DTO para la transferencia de datos de la entidad Sabor con Firestore REST API.
 */
data class SaborCampos(
    val nombre: FirestoreValue? = null,
    val marca: FirestoreValue? = null,
    /** Compatibilidad de lectura con documentos antiguos; no se escribe en altas o ediciones nuevas. */
    val stock: FirestoreValue? = null,
    /** Imagen del sabor como data URI base64. */
    val imagenUri: FirestoreValue? = null,
    /** Campo legado de imagen; solo se usa como respaldo de lectura. */
    val imagen: FirestoreValue? = null
)

data class SaborDocumento(
    val name: String? = null,
    val fields: SaborCampos? = null,
    val createTime: String? = null,
    val updateTime: String? = null
) {
    fun toEntity(): SaborItem {
        val docId = name?.substringAfterLast("/") ?: ""
        val f = fields
        val img = (f?.imagenUri?.asString().orEmpty()).ifBlank { f?.imagen?.asString().orEmpty() }
        return SaborItem(
            id = docId,
            nombre = f?.nombre?.asString() ?: "",
            marca = f?.marca?.asString() ?: "",
            stock = f?.stock?.asInt() ?: 0,
            imagenUri = img.takeIf { it.isNotBlank() }
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
                    // Si la imagen es null, el campo se omite. Junto con updateMask,
                    // esto permite borrar la imagen existente al editar.
                    imagenUri = saborItem.imagenUri
                        ?.takeIf { it.isNotBlank() }
                        ?.let { FirestoreValue(stringValue = it) }
                )
            )
        }
    }
}

data class SaborListResponse(
    val documents: List<SaborDocumento>? = emptyList(),
    val nextPageToken: String? = null
)
