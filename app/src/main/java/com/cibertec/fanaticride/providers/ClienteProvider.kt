package com.cibertec.fanaticride.providers

import com.cibertec.fanaticride.colecciones.Cliente
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ClienteProvider {
    val conexion = Firebase.firestore.collection("Clientes")

    fun insertar(cliente: Cliente): Task<Void> {
        return conexion.document(cliente.id!!).set(cliente)
    }
}