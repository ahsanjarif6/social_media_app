package com.example.social_media

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)

        buttonLogin = findViewById(R.id.buttonLogin)
        buttonLogin.setOnClickListener {
            lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signInWith(Email) {
                        email = editTextEmail.text.toString()
                        password = editTextPassword.text.toString()
                    }
                    var intent = Intent(this@MainActivity, HomeActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Something is wrong. Try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        registerButton = findViewById(R.id.buttonRegister)
        registerButton.setOnClickListener {
            var intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
    }
}