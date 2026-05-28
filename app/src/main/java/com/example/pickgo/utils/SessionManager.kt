package com.example.pickgo.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.pickgo.models.User

class SessionManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)
    
    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_TYPE = "user_type"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    fun saveSession(user: User) {
        prefs.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_TYPE, user.userType.name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    fun getSession(): User? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null
        
        val userId = prefs.getString(KEY_USER_ID, "") ?: ""
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val userTypeStr = prefs.getString(KEY_USER_TYPE, "CUSTOMER") ?: "CUSTOMER"
        
        return User(
            id = userId,
            email = email,
            userType = com.example.pickgo.models.UserType.valueOf(userTypeStr)
        )
    }
    
    fun clearSession() {
        prefs.edit().clear().apply()
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
}
