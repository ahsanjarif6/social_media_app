package com.example.social_media

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class RegisterActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonRegister = findViewById(R.id.buttonRegister)

        buttonRegister.setOnClickListener {

            CoroutineScope(Dispatchers.Main).launch {
                try{
                    val result = SupabaseClient.client.auth.signUpWith(Email) {
                        this.email = editTextEmail.text.toString()
                        this.password = editTextPassword.text.toString()
                    }
                    Toast.makeText(this@RegisterActivity, "Please check your email", Toast.LENGTH_SHORT).show()

                }catch (e: Exception){
                    Log.e("RegisterActivity-signup", "Error signing up: ${e.localizedMessage}")
                    Toast.makeText(this@RegisterActivity,"Something is wrong. Try again.",Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

}