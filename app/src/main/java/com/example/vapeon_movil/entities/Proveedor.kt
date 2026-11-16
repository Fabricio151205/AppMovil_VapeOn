package com.example.vapeon_movil.entities

/** Proveedor registrado por un administrador en la colección `proveedores`. */
data class Proveedor(
    val id: String = "",
    val razonSocial: String,
    val ruc: String,
    val contacto: String,
    val telefono: String,
    val correo: String,
    val direccion: String,
    val ciudad: String,
    val tipoProducto: String,
    val observaciones: String = "",
    val fechaRegistro: String
)
