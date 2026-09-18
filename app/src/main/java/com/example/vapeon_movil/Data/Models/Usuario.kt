package com.example.vapeon_movil.Data.Models

data class Usuario(
    val nombre: String,
    val apellido: String,
    val telefono: String,
    val fechaNacimiento: String,
    val correo: String,
    val password: String,
    val rol: String
)