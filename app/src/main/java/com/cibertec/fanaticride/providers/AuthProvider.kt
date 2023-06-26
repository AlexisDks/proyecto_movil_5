package com.cibertec.fanaticride.providers

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class AuthProvider {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun login(correo: String, contrasena: String): Task<AuthResult> {
        return  auth.signInWithEmailAndPassword(correo, contrasena)
    }

    fun registrar(correo: String, contrasena: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(correo, contrasena)
    }

    fun obtenerId(): String {
        return auth.currentUser?.uid?: ""
    }

    fun cerrarSesion(): Boolean {
        var existe = false
        if(auth.currentUser != null) {
            existe = true
        }
        return existe
    }
}