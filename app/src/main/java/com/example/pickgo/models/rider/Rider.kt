package com.example.pickgo.models.rider

data class Rider(
    val id: String = "",
    val riderId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val vehicleType: String = "Motorcycle",
    val plateNumber: String = "",
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val bankAccountName: String = "",
    val riderStatus: String = "offline",
    val riderRating: Double = 0.0,
    val riderPhoto: String? = null,
    val stationCity: String = "",
    val createdAt: String = ""
)

data class DeliveryOrder(
    val id: String = "",
    val orderId: String = "",
    val batchId: String? = null,
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val merchantId: String = "",
    val merchantName: String = "",
    val merchantAddress: String = "",
    val merchantPhone: String = "",
    val deliveryAddress: String = "",
    val orderTotal: Double = 0.0,
    val riderEarnings: Double = 0.0,
    val orderStatus: String = "pending",
    val paymentMethod: String = "COD",
    val orderDate: String = "",
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

data class RiderEarnings(
    val todayTrips: Int = 0,
    val todayEarnings: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val withdrawableBalance: Double = 0.0,
    val trips: List<DeliveryOrder> = emptyList(),
    val payouts: List<PayoutRequest> = emptyList()
)

data class PayoutRequest(
    val id: String = "",
    val userId: String = "",
    val userType: String = "rider",
    val amount: Double = 0.0,
    val bankName: String = "",
    val accountNumber: String = "",
    val accountName: String = "",
    val payoutStatus: String = "pending",
    val requestDate: String = ""
)

data class RiderReview(
    val id: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerInitials: String = "",
    val riderId: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: String = ""
)

data class DeliveryRequest(
    val id: String,
    val pickup: String,
    val pickupAddress: String,
    val deliveryAddress: String,
    val earnings: Double,
    val merchantName: String,
    val merchantCity: String
)