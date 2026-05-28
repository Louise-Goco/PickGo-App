package com.example.pickgo.models.seller

data class Seller(
    val id: String = "",
    val sellerId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val merchantId: String = "",
    val sellerStatus: String = "active",
    val sellerRating: Double = 0.0,
    val createdAt: String = ""
)

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
    val merchDeliveryRange: Int = 5
)

data class SellerOrder(
    val id: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val sellerId: String = "",
    val merchantName: String = "",
    val customerName: String = "",
    val customerPhone: String? = null,
    val orderTotal: Double = 0.0,
    val orderStatus: String = "pending",
    val deliveryAddress: String = "",
    val paymentMethod: String = "COD",
    val orderDate: String = "",
    val items: List<SellerOrderItem> = emptyList()
)

data class SellerOrderItem(
    val id: String = "",
    val orderId: String = "",
    val foodName: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0
) {
    val subtotal: Double get() = price * quantity
}

data class SellerItem(
    val id: String = "",
    val itemId: String = "",
    val sellerId: String = "",
    val itemName: String = "",
    val itemDescription: String = "",
    val itemPrice: Double = 0.0,
    val itemCategory: String = "",
    val itemImage: String? = null,
    val itemStatus: String = "available"
)

data class SellerReview(
    val id: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val sellerId: String = "",
    val itemId: String? = null,
    val itemName: String? = null,
    val customerName: String = "",
    val customerInitials: String = "C",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: String = ""
)

data class SellerPayout(
    val id: String = "",
    val sellerId: String = "",
    val amount: Double = 0.0,
    val bankName: String = "",
    val accountNumber: String = "",
    val accountName: String = "",
    val payoutStatus: String = "pending",
    val requestDate: String = ""
)

data class PayoutRequest(
    val id: String = "",
    val userId: String = "",
    val userType: String = "seller",
    val amount: Double = 0.0,
    val bankName: String = "",
    val accountName: String = "",
    val accountNumber: String = "",
    val payoutStatus: String = "pending",
    val requestDate: String = ""
)

data class SellerAnalytics(
    val totalRevenue: Double = 0.0,
    val successRate: Int = 0,
    val totalOrders: Int = 0,
    val todaysOrders: Int = 0,
    val activeListings: Int = 0,
    val pendingPayouts: Double = 0.0,
    val storeRating: Double = 0.0,
    val revenueTrend: Map<String, Double> = emptyMap(),
    val bestSellers: List<BestSellerItem> = emptyList()
)

data class BestSellerItem(
    val itemName: String,
    val quantitySold: Int,
    val totalSales: Double
)
