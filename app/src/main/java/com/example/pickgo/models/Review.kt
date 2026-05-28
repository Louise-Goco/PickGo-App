package com.example.pickgo.models

import java.util.Date

data class Review(
    val id: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val sellerId: String? = null,
    val riderId: String? = null,
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: Date = Date()
)

data class TipOption(val amount: Int, val label: String)