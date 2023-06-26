package com.cibertec.fanaticride.providers

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ConfiguracionProvider {
    val collecion = Firebase.firestore.collection("Configuracion")

    fun obtenerPrecios(): Task<DocumentSnapshot> {
        return collecion.document("precios").get().addOnFailureListener {
            exception ->
                Log.d("FIREBASE", "ERROR: ${exception.message}")
        }
    }
}