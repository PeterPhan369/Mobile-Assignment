package com.example.mobileassignment

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileassignment.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.signInButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập Email và Mã số cá nhân", Toast.LENGTH_SHORT).show()
            } else {
                // Future: Integrate with backend login API
                Toast.makeText(this, "Đăng nhập thành công với: $email", Toast.LENGTH_SHORT).show()

                // Navigate to MainActivity after successful sign-in
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Close SignInActivity so user can't go back with back button
            }
        }
    }
}