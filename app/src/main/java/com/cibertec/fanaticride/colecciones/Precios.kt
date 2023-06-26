package com.cibertec.fanaticride.colecciones

import com.beust.klaxon.*

private val klaxon = Klaxon()

data class Precios (
    val km: Double? = null,
    val min: Double? = null,
    val valorMin: Double? = null,
    val diferencia: Double? = null
) {
    public fun toJson() = klaxon.toJsonObject(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<Precios>(json)
    }
}