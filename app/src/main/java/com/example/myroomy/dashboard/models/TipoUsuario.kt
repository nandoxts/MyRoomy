package com.example.myroomy.dashboard.models

enum class TipoUsuario(val valor: String) {
    ADMIN("admin"),
    CLIENTE("cliente");

    companion object {
        fun from(valor: String): TipoUsuario {
            return values().firstOrNull { it.valor.equals(valor, ignoreCase = true) }
                ?: CLIENTE
        }
    }
}
