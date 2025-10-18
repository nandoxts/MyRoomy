package com.example.myroomy.dashboard.models

import java.io.Serializable

data class Habitacion(
    val id: Int = 0,
    val nombre: String,
    val descripcion: String,
    val categoria: String,
    val imagen: String,
    val estado: String,
    val precio: Double,
    val servicios: List<String>,
    val numeroHabitacion: String,
    val piso: Int
) : Serializable
