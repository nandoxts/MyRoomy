package com.example.myroomy.dashboard.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String,
    val tipo: TipoUsuario,
    val clave: String,
    val estado: Int,
    val urlFoto: String? = null
) : Parcelable
