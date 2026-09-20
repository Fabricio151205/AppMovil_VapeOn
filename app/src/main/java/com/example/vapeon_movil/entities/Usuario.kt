package com.example.vapeon_movil.entities

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val telefono: String = "",
    val fechaNacimiento: String = "",
    val correo: String = "",
    val password: String = "",
    val rol: String = "CLIENTE"
)