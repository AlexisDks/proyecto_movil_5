package com.cibertec.fanaticride.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cibertec.fanaticride.colecciones.Cliente
import com.cibertec.fanaticride.databinding.ActivityRegisterBinding
import com.cibertec.fanaticride.providers.AuthProvider
import com.cibertec.fanaticride.providers.ClienteProvider

class RegisterActivity: AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val authProvider = AuthProvider()
    private val clienteProvider = ClienteProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)

        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        binding.btnGoToLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.btnRegister.setOnClickListener {
            val nombre = binding.textFieldName.text.toString()
            val apellido = binding.textFieldLastname.text.toString()
            val correo = binding.textFieldEmail.text.toString()
            val celular = binding.textFieldPhone.text.toString()
            val contraseña = binding.textFieldPassword.text.toString()
            val confirmacion = binding.textFieldConfirmPassword.text.toString()

            if(isValidFormulario(nombre, apellido, correo, celular, contraseña, confirmacion)) {
                authProvider.registrar(correo, contraseña).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val cliente = Cliente(
                            id =  authProvider.obtenerId(),
                            nombre = nombre,
                            apellido = apellido,
                            celular = celular,
                            correo = correo
                        )
                        clienteProvider.insertar(cliente).addOnCompleteListener {
                            if(it.isSuccessful) {
                                Toast.makeText(this@RegisterActivity, "Registro Exitoso", Toast.LENGTH_SHORT).show()
                                irAlMapa()
                            }
                            else {
                                Toast.makeText(this@RegisterActivity, "Hubo un error Almacenado los datos del usuario ${it.exception.toString()}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@RegisterActivity, "Registro Fallido ${it.exception.toString()}", Toast.LENGTH_LONG).show()
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

    private fun isValidFormulario(nombre: String, apellido: String, correo: String, celular: String, contraseña: String, confirmacion: String): Boolean {
        if(nombre.isEmpty()){
            mostrarToast("Ingresar Nombre")
            return false
        }

        if(apellido.isEmpty()) {
            mostrarToast("Ingresar Apellido")
            return false
        }

        if(correo.isEmpty()) {
            mostrarToast("Ingresar Correo")
            return false
        }

        if(celular.isEmpty()) {
            mostrarToast("Ingresar Celular")
            return false
        }

        if(celular.length in 10..8) {
            mostrarToast("El numero debe ser de 9 digitos")
            return false
        }

        if(contraseña.isEmpty() && confirmacion.isEmpty()) {
            mostrarToast("Ingresar Contraseña")
            return false
        }

        if(confirmacion != confirmacion) {
            mostrarToast("Las contraseñas no coinciden")
            return false
        }

        if(contraseña.length < 6) {
            mostrarToast("La contraseña es menor a 6 digitos")
            return false
        }

        return true
    }

    private fun mostrarToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}