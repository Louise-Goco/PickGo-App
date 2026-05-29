package com.example.pickgo.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.example.pickgo.R
import com.example.pickgo.utils.FirebaseManager
import kotlinx.coroutines.launch

class AdminSeedActivity : AppCompatActivity() {
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var messageText: TextView
    private lateinit var createButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_seed)

        firebaseManager = FirebaseManager(this)
        messageText = findViewById(R.id.messageText)
        createButton = findViewById(R.id.createButton)

        createButton.setOnClickListener {
            createAdminUser()
        }
    }

    private fun createAdminUser() {
        createButton.isEnabled = false
        messageText.visibility = android.view.View.GONE

        lifecycleScope.launch {
            try {
                val result = firebaseManager.createAdminUser("admin@gmail.com", "admin123")
                if (result.isSuccess) {
                    showMessage("Admin user created successfully! You can log in at login page.", true)
                } else {
                    showMessage("Error: ${result.exceptionOrNull()?.message}", false)
                }
            } catch (e: Exception) {
                showMessage("Error: ${e.message}", false)
            } finally {
                createButton.isEnabled = true
            }
        }
    }

    private fun showMessage(message: String, isSuccess: Boolean) {
        messageText.text = message
        messageText.visibility = android.view.View.VISIBLE
        messageText.setBackgroundColor(
            if (isSuccess) getColor(R.color.success_bg)
            else getColor(R.color.error_bg)
        )
        messageText.setTextColor(
            if (isSuccess) getColor(R.color.success_text)
            else getColor(R.color.error_text)
        )
    }
}