package com.example.vapeon_movil.services.dto

import com.example.vapeon_movil.entities.Proveedor
import java.time.Instant

data class ProveedorContainer(val fields: Map<String, FirestoreValor>)

data class ProveedorDocumento(
    val name: String,
    val fields: Map<String, FirestoreValor>,
    val createTime: String? = null,
    val updateTime: String? = null
)

data class ProveedorListResponse(
    val documents: List<ProveedorDocumento>? = emptyList(),
    val nextPageToken: String? = null
)

fun Proveedor.aFirestore(): ProveedorContainer {
    fun texto(valor: String) = FirestoreValor(stringValue = valor.trim())
    return ProveedorContainer(linkedMapOf(
        "razonSocial" to texto(razonSocial),
        "ruc" to texto(ruc),
        "contacto" to texto(contacto),
        "telefono" to texto(telefono),
        "correo" to texto(correo),
        "direccion" to texto(direccion),
        "ciudad" to texto(ciudad),
        "tipoProducto" to texto(tipoProducto),
        "observaciones" to texto(observaciones),
        "fechaRegistro" to FirestoreValor(timestampValue = fechaRegistro)
    ))
}

/** Lee defensivamente el documento y nunca toma el ID desde un campo editable. */
fun ProveedorDocumento.aProveedor(): Proveedor {
    fun requerido(campo: String): String = fields[campo]?.stringValue?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Proveedor incompleto: $campo.")
    val id = name.substringAfterLast('/').takeIf { it.isNotBlank() && '/' !in it }
        ?: throw IllegalArgumentException("ID de proveedor inválido.")
    val fecha = fields["fechaRegistro"]?.timestampValue
        ?: throw IllegalArgumentException("Fecha de registro inválida.")
    Instant.parse(fecha)
    return Proveedor(
        id = id,
        razonSocial = requerido("razonSocial"),
        ruc = requerido("ruc"),
        contacto = requerido("contacto"),
        telefono = requerido("telefono"),
        correo = requerido("correo"),
        direccion = requerido("direccion"),
        ciudad = requerido("ciudad"),
        tipoProducto = requerido("tipoProducto"),
        observaciones = fields["observaciones"]?.stringValue.orEmpty().trim(),
        fechaRegistro = fecha
    )
}
