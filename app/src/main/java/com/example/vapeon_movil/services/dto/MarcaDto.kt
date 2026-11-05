package com.example.vapeon_movil.services.dto

import com.example.vapeon_movil.entities.Marca

data class MarcaCampos(
    val nombre: FirestoreValue? = null
)

data class MarcaDocumento(
    val name: String? = null,
    val fields: MarcaCampos? = null,
    val createTime: String? = null,
    val updateTime: String? = null
) {
    fun toEntity(): Marca = Marca(
        id = name?.substringAfterLast("/").orEmpty(),
        nombre = fields?.nombre?.asString().orEmpty()
    )
}

data class MarcaContainer(val fields: MarcaCampos) {
    companion object {
        fun fromEntity(marca: Marca) = MarcaContainer(
            MarcaCampos(nombre = FirestoreValue(stringValue = marca.nombre.trim()))
        )
    }
}

data class MarcaListResponse(
    val documents: List<MarcaDocumento>? = emptyList(),
    val nextPageToken: String? = null
)
