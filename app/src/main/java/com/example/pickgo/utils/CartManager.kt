package com.example.pickgo.utils


import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.pickgo.models.CartItem

class CartManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cart_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val cartKey = "cart_items"

    fun getCartItems(): List<CartItem> {
        val json = prefs.getString(cartKey, "[]")
        val type = object : TypeToken<List<CartItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addItem(item: CartItem): List<CartItem> {
        val items = getCartItems().toMutableList()
        val existingIndex = items.indexOfFirst { it.itemId == item.itemId }

        if (existingIndex != -1) {
            val existing = items[existingIndex]
            items[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            items.add(item)
        }

        saveCartItems(items)
        return items
    }

    fun updateQuantity(itemId: String, change: Int): List<CartItem> {
        val items = getCartItems().toMutableList()
        val index = items.indexOfFirst { it.itemId == itemId }

        if (index != -1) {
            val item = items[index]
            val newQuantity = item.quantity + change
            if (newQuantity <= 0) {
                items.removeAt(index)
            } else {
                items[index] = item.copy(quantity = newQuantity)
            }
        }

        saveCartItems(items)
        return items
    }

    fun removeItem(itemId: String): List<CartItem> {
        val items = getCartItems().toMutableList()
        items.removeAll { it.itemId == itemId }
        saveCartItems(items)
        return items
    }

    fun clearCart() {
        saveCartItems(emptyList())
    }

    fun getItemCount(): Int {
        return getCartItems().sumOf { it.quantity }
    }

    fun getSubtotal(): Double {
        return getCartItems().sumOf { it.lineTotal }
    }

    private fun saveCartItems(items: List<CartItem>) {
        val json = gson.toJson(items)
        prefs.edit().putString(cartKey, json).apply()
    }
}