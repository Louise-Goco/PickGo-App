package com.example.pickgo.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pickgo.databinding.ActivityRegisterBinding
import com.example.pickgo.models.User
import com.example.pickgo.models.UserType
import com.example.pickgo.utils.FirebaseManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseManager: FirebaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.submitBtn.setOnClickListener {
            performRegistration()
        }

        binding.signInBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.backHomeBtn.setOnClickListener {
            finish()
        }
    }

    private fun performRegistration() {
        val firstName = binding.firstNameInput.text.toString().trim()
        val lastName = binding.lastNameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()
        val termsAccepted = binding.termsCheckbox.isChecked

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            showError("All fields are required.")
            return
        }

        if (password != confirmPassword) {
            showError("Passwords do not match.")
            return
        }

        if (!termsAccepted) {
            showError("You must agree to the terms and conditions.")
            return
        }

        binding.submitBtn.isEnabled = false
        clearMessages()

        val user = User(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phoneNumber = phone,
            password = password,
            userType = UserType.CUSTOMER,
            isVerified = false
        )

        lifecycleScope.launch {
            try {
                val result = firebaseManager.registerCustomer(user)
                if (result.isSuccess) {
                    showSuccess("Registration successful! You can now log in.")
                    binding.firstNameInput.text?.clear()
                    binding.lastNameInput.text?.clear()
                    binding.emailInput.text?.clear()
                    binding.phoneInput.text?.clear()
                    binding.passwordInput.text?.clear()
                    binding.confirmPasswordInput.text?.clear()
                    binding.termsCheckbox.isChecked = false
                } else {
                    showError(result.exceptionOrNull()?.message ?: "Registration failed.")
                }
            } catch (e: Exception) {
                showError("Database error: ${e.message ?: "Please try again."}")
            } finally {
                binding.submitBtn.isEnabled = true
            }
        }
    }

    private fun showError(message: String) {
        binding.errorBox.visibility = android.view.View.VISIBLE
        binding.errorText.text = message
        binding.successBox.visibility = android.view.View.GONE
    }

    private fun showSuccess(message: String) {
        binding.successBox.visibility = android.view.View.VISIBLE
        binding.successText.text = message
        binding.errorBox.visibility = android.view.View.GONE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun clearMessages() {
        binding.errorBox.visibility = android.view.View.GONE
        binding.successBox.visibility = android.view.View.GONE
    }
}