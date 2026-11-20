package com.example.vapeon_movil.entities

data class Inversionista(
    val id: String = "",
    val nombre: String = "",
    val tipoDocumento: String = "DNI",
    val documento: String = "",
    val telefono: String = "",
    val correo: String = "",
    val observaciones: String = "",
    val fechaRegistro: String = ""
)
