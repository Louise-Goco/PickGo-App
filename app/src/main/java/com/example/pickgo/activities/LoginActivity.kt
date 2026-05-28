package com.example.pickgo.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pickgo.activities.admin.AdminDashboardActivity
import com.example.pickgo.activities.customer.CustomerDashboardActivity
import com.example.pickgo.activities.rider.RiderDashboardActivity
import com.example.pickgo.activities.seller.SellerDashboardActivity
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.databinding.ActivityLoginBinding
import com.example.pickgo.utils.FirebaseManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sharedPrefs = getSharedPreferences("PickGoPrefs", Context.MODE_PRIVATE)

        checkRememberedUser()
        setupClickListeners()
    }

    private fun checkRememberedUser() {
        val remember = sharedPrefs.getBoolean("remember_me", false)
        if (remember) {
            val email = sharedPrefs.getString("user_email", "") ?: ""
            val password = sharedPrefs.getString("user_password", "") ?: ""
            if (email.isNotEmpty() && password.isNotEmpty()) {
                binding.emailInput.setText(email)
                binding.passwordInput.setText(password)
                binding.rememberCheckbox.isChecked = true
            }
        }
    }

    private fun setupClickListeners() {
        binding.submitBtn.setOnClickListener {
            performLogin()
        }

        binding.createAccountBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.forgotPasswordBtn.setOnClickListener {
            Toast.makeText(this, "Reset password link would be sent to your email", Toast.LENGTH_LONG).show()
        }

        binding.backHomeBtn.setOnClickListener {
            finish()
        }
    }

    private fun performLogin() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        if (email.isEmpty()) {
            showError("Email is required")
            return
        }

        if (password.isEmpty()) {
            showError("Password is required")
            return
        }

        binding.submitBtn.isEnabled = false
        showError("", false)

        lifecycleScope.launch {
            try {
                val result = firebaseManager.login(email, password)
                if (result.isSuccess) {
                    val user = result.getOrNull()!!

                    // Save credentials if remember me is checked
                    if (binding.rememberCheckbox.isChecked) {
                        sharedPrefs.edit().apply {
                            putBoolean("remember_me", true)
                            putString("user_email", email)
                            putString("user_password", password)
                            apply()
                        }
                    } else {
                        sharedPrefs.edit().clear().apply()
                    }

                    // Navigate based on user type
                    val intent = when (user.userType) {
                        com.example.pickgo.models.UserType.ADMIN -> Intent(this@LoginActivity, AdminDashboardActivity::class.java)
                        com.example.pickgo.models.UserType.RIDER -> Intent(this@LoginActivity, RiderDashboardActivity::class.java)
                        com.example.pickgo.models.UserType.SELLER -> Intent(this@LoginActivity, SellerDashboardActivity::class.java)
                        else -> Intent(this@LoginActivity, CustomerDashboardActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    showError("Invalid email or password")
                }
            } catch (e: Exception) {
                showError("Database error: ${e.message ?: "Please try again."}")
            } finally {
                binding.submitBtn.isEnabled = true
            }
        }
    }

    private fun showError(message: String, show: Boolean = true) {
        if (show && message.isNotEmpty()) {
            binding.errorBox.visibility = android.view.View.VISIBLE
            binding.errorText.text = message
        } else {
            binding.errorBox.visibility = android.view.View.GONE
        }
    }
}