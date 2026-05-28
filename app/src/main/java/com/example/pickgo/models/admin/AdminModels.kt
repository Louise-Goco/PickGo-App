package com.example.pickgo.models.admin

data class AdminUser(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val userType: String = "admin",
    val createdAt: String = ""
)

data class AdminCustomer(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val accountStatus: String = "active",
    val isVerified: Boolean = false,
    val createdAt: String = ""
)

data class AdminSeller(
    val id: String = "",
    val sellerId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val merchantId: String = "",
    val merchantName: String = "",
    val merchantType: String = "",
    val sellerStatus: String = "pending",
    val sellerRating: Double = 0.0,
    val govIdUrl: String? = null,
    val birCertUrl: String? = null,
    val createdAt: String = ""
)

data class AdminRider(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val vehicleType: String = "",
    val plateNumber: String = "",
    val licenseNumber: String = "",
    val riderStatus: String = "pending",
    val riderRating: Double = 0.0,
    val totalDeliveries: Int = 0,
    val isVerified: Boolean = false,
    val licensePhotoUrl: String? = null,
    val nbiUrl: String? = null,
    val orUrl: String? = null,
    val crUrl: String? = null,
    val createdAt: String = ""
)

data class AdminProduct(
    val id: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val itemDescription: String = "",
    val itemPrice: Double = 0.0,
    val itemCategory: String = "",
    val categoryName: String = "",
    val itemImage: String? = null,
    val itemStatus: String = "pending",
    val merchantId: String = "",
    val merchantName: String = "",
    val sellerId: String = "",
    val createdAt: String = ""
)

data class AdminOrder(
    val id: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val sellerId: String = "",
    val merchantName: String = "",
    val sellerName: String = "",
    val riderId: String? = null,
    val riderName: String? = null,
    val orderTotal: Double = 0.0,
    val orderStatus: String = "pending",
    val paymentMethod: String = "COD",
    val deliveryAddress: String = "",
    val orderDate: String = ""
)

data class AdminCategory(
    val id: String = "",
    val categoryName: String = "",
    val categoryDescription: String = "",
    val createdAt: String = ""
)

data class AdminPayout(
    val id: String = "",
    val userId: String = "",
    val userType: String = "",
    val userName: String = "",
    val amount: Double = 0.0,
    val bankName: String = "",
    val accountNumber: String = "",
    val accountName: String = "",
    val payoutStatus: String = "pending",
    val requestDate: String = "",
    val processedDate: String? = null
)

data class PromoCode(
    val id: String = "",
    val code: String = "",
    val discountType: String = "percentage",
    val discountValue: Double = 0.0,
    val expiryDate: String = "",
    val usageLimit: Int = 100,
    val currentUsage: Int = 0,
    val createdAt: String = ""
)

data class SystemSettings(
    val deliveryFee: Double = 49.0,
    val serviceFee: Double = 0.0,
    val taxRate: Double = 0.0,
    val paymentCodEnabled: Boolean = true,
    val paymentGcashEnabled: Boolean = true,
    val paymentCardEnabled: Boolean = true
)

data class DashboardStats(
    val totalUsers: Int = 0,
    val pendingApprovals: Int = 0,
    val activeSellers: Int = 0,
    val activeRiders: Int = 0,
    val todayRevenue: Double = 0.0
)

data class AdminItem(
    val id: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val itemDescription: String = "",
    val itemPrice: Double = 0.0,
    val itemCategory: String = "",
    val categoryName: String = "",
    val itemImage: String? = null,
    val itemStatus: String = "pending", // pending, available, rejected, out_of_stock
    val merchantId: String = "",
    val merchantName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val rejectionReason: String? = null,
    val approvalDate: String? = null,
    val viewsCount: Int = 0,
    val ordersCount: Int = 0
)