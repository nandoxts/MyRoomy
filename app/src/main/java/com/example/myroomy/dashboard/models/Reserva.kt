package com.example.myroomy.dashboard.models

import java.io.Serializable

data class Reserva(
    val id: Int = 0,
    val idUsuario: Int,
    val idHabitacion: Int,
    val fechaSolicitud: String,
    val estado: String = "Pendiente",
    val comentario: String = "",
    val fechaIngreso: String,
    val fechaSalida: String,
    val total: Double = 0.0,
    val metodoPago: String = "",
    val cantidadPersonas: Int = 1,
    // Estos vienen del JOIN
    val nombreUsuario: String = "",
    val urlFotoUsuario: String = "",
    val nombreHabitacion: String = "",
    val precio: Double = 0.0
) : Serializable
