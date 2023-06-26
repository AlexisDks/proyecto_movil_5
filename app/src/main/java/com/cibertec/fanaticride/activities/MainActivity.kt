package com.cibertec.fanaticride.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import com.cibertec.fanaticride.R
import com.cibertec.fanaticride.databinding.ActivityMainBinding
import com.cibertec.fanaticride.providers.AuthProvider

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val authProvider = AuthProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            val correo = binding.textFieldEmail.text.toString()
            val contrasena = binding.textFieldPassword.text.toString()

            if(isValidFormulario(correo, contrasena)) {
                authProvider.login(correo, contrasena).addOnCompleteListener {
                    if(it.isSuccessful) {
                        irAlMapa()
                    } else {
                        Toast.makeText(this@MainActivity, "Error al iniciar sesion", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun irAlMapa() {
        val intent = Intent(this, MapActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun isValidFormulario(correo: String, contrasena: String): Boolean {
        if(correo.isEmpty()) {
            mostrarToast("Ingresar contraseña")
            return false
        }

        if(contrasena.isEmpty()) {
            mostrarToast("Ingresar contraseña")
            return false
        }
        return true
    }

    private fun mostrarToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}