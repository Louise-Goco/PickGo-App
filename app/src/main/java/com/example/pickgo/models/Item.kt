package com.example.pickgo.models

data class Item(
    val id: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val itemDescription: String = "",
    val itemPrice: Double = 0.0,
    val itemImage: String? = null,
    val itemCategory: String = "",
    val itemStatus: String = "available",
    val sellerId: String = "",
    val merchantName: String = "",
    val merchantId: String = ""
)

data class CartItem(
    val itemId: String,
    val itemName: String,
    val itemPrice: Double,
    val itemImage: String?,
    val merchantName: String,
    val merchantId: String,
    var quantity: Int = 1
) {
    val lineTotal: Double get() = itemPrice * quantity
}
