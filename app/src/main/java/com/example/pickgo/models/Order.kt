package com.example.pickgo.models

import java.util.Date

data class Order(
    val id: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val sellerId: String = "",
    val merchantName: String = "",
    val merchantId: String = "",
    val orderTotal: Double = 0.0,
    val orderStatus: String = "pending",
    val deliveryAddress: String = "",
    val paymentMethod: String = "COD",
    val riderId: String? = null,
    val riderEarnings: Double = 0.0,
    val orderDate: Date = Date(),
    val items: List<OrderItem> = emptyList()
)

data class OrderItem(
    val id: String = "",
    val orderId: String = "",
    val foodName: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0
) {
    val subtotal: Double get() = price * quantity
}

enum class OrderStatus(val displayName: String) {
    PENDING("Pending"),
    PREPARING("Preparing"),
    READY_FOR_PICKUP("Ready for Pickup"),
    ON_THE_WAY("On the Way"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    companion object {
        fun fromString(value: String): OrderStatus = values().find { it.name.equals(value, ignoreCase = true) } ?: PENDING
    }
}

data class Address(
    val id: String = "",
    val userId: String = "",
    val label: String = "",
    val addressLine1: String = "",
    val city: String = "",
    val isDefault: Boolean = false
)