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
        const val KEY_USER_FIRST_NAME = "user_first_name"
        const val KEY_USER_LAST_NAME = "user_last_name"
        const val KEY_USER_PHONE = "user_phone"
    }
    
    fun saveSession(user: User) {
        prefs.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_TYPE, user.userType.name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_FIRST_NAME, user.firstName)
            putString(KEY_USER_LAST_NAME, user.lastName)
            putString(KEY_USER_PHONE, user.phoneNumber)
            apply()
        }
    }
    
    fun getSession(): User? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null
        
        val userId = prefs.getString(KEY_USER_ID, "") ?: ""
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val userTypeStr = prefs.getString(KEY_USER_TYPE, "CUSTOMER") ?: "CUSTOMER"
        val firstName = prefs.getString(KEY_USER_FIRST_NAME, "") ?: ""
        val lastName = prefs.getString(KEY_USER_LAST_NAME, "") ?: ""
        val phoneNumber = prefs.getString(KEY_USER_PHONE, "") ?: ""
        
        return User(
            id = userId,
            email = email,
            userType = com.example.pickgo.models.UserType.valueOf(userTypeStr),
            firstName = firstName,
            lastName = lastName,
            phoneNumber = phoneNumber
        )
    }
    
    fun clearSession() {
        prefs.edit().clear().apply()
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
}
