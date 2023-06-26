package com.cibertec.fanaticride.colecciones

import com.beust.klaxon.Klaxon

private val klaxon = Klaxon()

data class Cliente (
    val id: String? = null,
    val nombre: String ? = null,
    val apellido: String ? = null,
    val correo: String ? = null,
    val celular: String ? = null,
    val imagen: String ? = null
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<Cliente>(json)
    }
}