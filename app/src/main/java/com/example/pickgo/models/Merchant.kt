package com.example.pickgo.models

data class Merchant(
    val id: String = "",
    val merchId: String = "",
    val merchName: String = "",
    val merchType: String = "",
    val merchDescription: String = "",
    val merchLogo: String? = null,
    val merchBanner: String? = null,
    val merchStatus: String = "active",
    val merchOpeningTime: String = "08:00",
    val merchClosingTime: String = "20:00",
    val rating: Double = 0.0
)