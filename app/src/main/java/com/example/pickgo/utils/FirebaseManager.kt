package com.example.pickgo.utils

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.example.pickgo.models.*
import com.example.pickgo.models.admin.*
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: return Result.failure(Exception("Authentication failed"))

            val userDoc = db.collection("users").document(userId).get().await()
            val user = userDoc.toObject(User::class.java) ?: return Result.failure(Exception("User data not found"))

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerCustomer(userData: User): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(userData.email, userData.password).await()
            val userId = authResult.user?.uid ?: return Result.failure(Exception("Authentication failed"))

            val hashedPassword = PasswordHasher.hashPassword(userData.password)
            val userWithId = userData.copy(
                id = userId,
                password = hashedPassword,
                createdAt = java.util.Date()
            )

            db.collection("users").document(userId).set(userWithId).await()
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitRiderApplication(application: RiderApplication, files: Map<String, Uri>): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            // Upload files
            val fileUrls = mutableMapOf<String, String>()
            for ((key, uri) in files) {
                val fileRef = storage.child("rider_applications/$userId/$key")
                fileRef.putFile(uri).await()
                val url = fileRef.downloadUrl.await().toString()
                fileUrls[key] = url
            }

            val completeApplication = application.copy(
                userId = userId,
                licensePhotoUrl = fileUrls["license_photo"],
                nbiUrl = fileUrls["nbi"],
                orUrl = fileUrls["or"],
                crUrl = fileUrls["cr"],
                submittedAt = java.util.Date()
            )

            val docRef = db.collection("rider_applications").document(userId)
            docRef.set(completeApplication).await()

            // Update user type to rider pending
            db.collection("users").document(userId).update(
                mapOf(
                    "userType" to UserType.RIDER.name,
                    "accountStatus" to AccountStatus.PENDING.name
                )
            ).await()

            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitSellerApplication(application: SellerApplication, files: Map<String, Uri>): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            // Upload files
            val fileUrls = mutableMapOf<String, String>()
            for ((key, uri) in files) {
                val fileRef = storage.child("seller_applications/$userId/$key")
                fileRef.putFile(uri).await()
                val url = fileRef.downloadUrl.await().toString()
                fileUrls[key] = url
            }

            val completeApplication = application.copy(
                userId = userId,
                govIdUrl = fileUrls["gov_id"],
                birCertUrl = fileUrls["bir_cert"],
                submittedAt = java.util.Date()
            )

            val docRef = db.collection("seller_applications").document(userId)
            docRef.set(completeApplication).await()

            // Update user type to seller pending
            db.collection("users").document(userId).update(
                mapOf(
                    "userType" to UserType.SELLER.name,
                    "accountStatus" to AccountStatus.PENDING.name
                )
            ).await()

            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): User? {
        val userId = auth.currentUser?.uid ?: return null
        val userDoc = db.collection("users").document(userId).get().await()
        return userDoc.toObject(User::class.java)
    }

    suspend fun logout() {
        auth.signOut()
    }

    suspend fun createAdminUser(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: return Result.failure(Exception("Authentication failed"))

            val hashedPassword = PasswordHasher.hashPassword(password)
            val adminUser = User(
                id = userId,
                firstName = "Admin",
                lastName = "User",
                email = email,
                phoneNumber = "09000000000",
                password = hashedPassword,
                userType = UserType.ADMIN,
                accountStatus = AccountStatus.ACTIVE,
                isVerified = true,
                createdAt = java.util.Date()
            )

            db.collection("users").document(userId).set(adminUser).await()
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableItems(): List<com.example.pickgo.models.Item> {
        return try {
            val snapshot = db.collection("items")
                .whereEqualTo("itemStatus", "available")
                .get()
                .await()
            snapshot.toObjects(com.example.pickgo.models.Item::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCategories(): List<com.example.pickgo.models.Category> {
        return try {
            val snapshot = db.collection("categories")
                .get()
                .await()
            snapshot.toObjects(com.example.pickgo.models.Category::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFeaturedItems(): List<com.example.pickgo.models.Item> {
        return try {
            val snapshot = db.collection("items")
                .whereEqualTo("itemStatus", "available")
                .get()
                .await()
            snapshot.toObjects(com.example.pickgo.models.Item::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getActiveMerchants(): List<com.example.pickgo.models.Merchant> {
        return try {
            val snapshot = db.collection("merchants")
                .get()
                .await()
            snapshot.toObjects(com.example.pickgo.models.Merchant::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCustomerOrders(userId: String): List<com.example.pickgo.models.Order> {
        return try {
            val snapshot = db.collection("orders")
                .whereEqualTo("customerId", userId)
                .get()
                .await()
            snapshot.toObjects(com.example.pickgo.models.Order::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Admin-specific methods
    suspend fun getAllCustomers(): List<AdminCustomer> {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("userType", "customer")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.let { user ->
                    AdminCustomer(
                        id = user.id,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        email = user.email,
                        phoneNumber = user.phoneNumber,
                        accountStatus = (user.accountStatus ?: AccountStatus.ACTIVE).name,
                        isVerified = user.isVerified,
                        createdAt = user.createdAt?.toString() ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createCustomer(customer: AdminCustomer, hashedPassword: String): String {
        val userData = mapOf(
            "firstName" to customer.firstName,
            "lastName" to customer.lastName,
            "email" to customer.email,
            "phoneNumber" to customer.phoneNumber,
            "password" to hashedPassword,
            "userType" to "customer",
            "accountStatus" to "active",
            "isVerified" to false,
            "createdAt" to customer.createdAt
        )
        return addRecord("users", userData)
    }

    suspend fun updateCustomer(customerId: String, updates: Map<String, Any>) {
        updateRecord("users", customerId, updates)
    }

    suspend fun deleteCustomer(customerId: String) {
        deleteRecord("users", customerId)
    }

    suspend fun getAllCategories(): List<AdminCategory> {
        return try {
            val snapshot = db.collection("categories")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.let { cat ->
                    AdminCategory(
                        id = cat.id,
                        categoryName = cat.name,
                        categoryDescription = cat.description,
                        createdAt = ""
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createCategory(category: AdminCategory): String {
        val catData = mapOf(
            "Categ_Name" to category.categoryName,
            "Categ_Description" to category.categoryDescription,
            "created_at" to category.createdAt
        )
        return addRecord("categories", catData)
    }

    suspend fun updateCategory(category: AdminCategory) {
        updateRecord("categories", category.id, mapOf(
            "Categ_Name" to category.categoryName,
            "Categ_Description" to category.categoryDescription
        ))
    }

    suspend fun deleteCategory(categoryId: String) {
        deleteRecord("categories", categoryId)
    }

    suspend fun getSystemSettings(): SystemSettings {
        val settingsMap = getSettingsMap()
        return SystemSettings(
            deliveryFee = (settingsMap["delivery_fee"] as? String)?.toDoubleOrNull() ?: 49.0,
            serviceFee = (settingsMap["service_fee"] as? String)?.toDoubleOrNull() ?: 0.0,
            taxRate = (settingsMap["tax_rate"] as? String)?.toDoubleOrNull() ?: 0.0,
            paymentCodEnabled = (settingsMap["payment_cod_enabled"] as? String)?.toIntOrNull()?.let { it == 1 } ?: true,
            paymentGcashEnabled = (settingsMap["payment_gcash_enabled"] as? String)?.toIntOrNull()?.let { it == 1 } ?: true,
            paymentCardEnabled = (settingsMap["payment_card_enabled"] as? String)?.toIntOrNull()?.let { it == 1 } ?: true
        )
    }

    suspend fun updateSystemSettings(settings: SystemSettings) {
        upsertSetting("delivery_fee", settings.deliveryFee.toString())
        upsertSetting("service_fee", settings.serviceFee.toString())
        upsertSetting("tax_rate", settings.taxRate.toString())
        upsertSetting("payment_cod_enabled", if (settings.paymentCodEnabled) "1" else "0")
        upsertSetting("payment_gcash_enabled", if (settings.paymentGcashEnabled) "1" else "0")
        upsertSetting("payment_card_enabled", if (settings.paymentCardEnabled) "1" else "0")
    }

    private suspend fun upsertSetting(key: String, value: String) {
        val existing = queryOne("settings", "Setting_Key", key)
        if (existing != null) {
            updateRecord("settings", existing.id, mapOf("Setting_Value" to value))
        } else {
            addRecord("settings", mapOf("Setting_Key" to key, "Setting_Value" to value))
        }
    }

    suspend fun getAllPromoCodes(): List<PromoCode> {
        return try {
            val snapshot = db.collection("promo_codes")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(PromoCode::class.java)?.let { promo ->
                    PromoCode(
                        id = promo.id,
                        code = promo.code,
                        discountType = promo.discountType,
                        discountValue = promo.discountValue,
                        expiryDate = promo.expiryDate,
                        usageLimit = promo.usageLimit,
                        currentUsage = promo.currentUsage,
                        createdAt = promo.createdAt
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createPromoCode(promo: PromoCode): String {
        val promoData = mapOf(
            "Code" to promo.code,
            "Discount_Type" to promo.discountType,
            "Discount_Value" to promo.discountValue,
            "Expiry_Date" to promo.expiryDate,
            "Usage_Limit" to promo.usageLimit,
            "Current_Usage" to promo.currentUsage,
            "created_at" to promo.createdAt
        )
        return addRecord("promo_codes", promoData)
    }

    suspend fun deletePromoCode(promoId: String) {
        deleteRecord("promo_codes", promoId)
    }

    suspend fun getAllProducts(): List<AdminProduct> {
        val items = getItemsWithMerchants()
        val categories = getAllCategories()
        val catMap = categories.associateBy { it.id }

        return items.map { item ->
            AdminProduct(
                id = item.id,
                itemId = item.itemId,
                itemName = item.itemName,
                itemDescription = item.itemDescription,
                itemPrice = item.itemPrice,
                itemCategory = item.itemCategory,
                categoryName = catMap[item.itemCategory]?.categoryName ?: "General",
                itemImage = item.itemImage,
                itemStatus = item.itemStatus,
                merchantId = item.merchantId,
                merchantName = item.merchantName,
                sellerId = item.sellerId,
                createdAt = ""
            )
        }
    }

    suspend fun updateProductStatus(productId: String, status: String) {
        updateRecord("items", productId, mapOf("itemStatus" to status))
    }

    suspend fun getAllOrders(): List<com.example.pickgo.models.Order> {
        return try {
            val snapshot = db.collection("orders")
                .get()
                .await()
            snapshot.toObjects(com.example.pickgo.models.Order::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllOrdersWithDetails(): List<AdminOrder> {
        return try {
            val snapshot = db.collection("orders")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(AdminOrder::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        updateRecord("orders", orderId, mapOf("orderStatus" to status))
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = db.collection("users")
                .get()
                .await()
            snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllSellers(): List<AdminSeller> {
        return try {
            val snapshot = db.collection("sellers")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(AdminSeller::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllRiders(): List<AdminRider> {
        return try {
            val snapshot = db.collection("riders")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(AdminRider::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllRidersWithDocuments(): List<AdminRider> {
        return getAllRiders()
    }

    suspend fun updateRider(riderId: String, updates: Map<String, Any>) {
        updateRecord("riders", riderId, updates)
    }

    suspend fun deleteRider(riderId: String) {
        deleteRecord("riders", riderId)
    }

    suspend fun getActiveMerchantCities(): List<String> {
        return try {
            val snapshot = db.collection("merchants")
                .whereEqualTo("merchantStatus", "active")
                .get()
                .await()
            snapshot.documents.mapNotNull { 
                it.getString("merchantCity") 
            }.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRiderTodayStats(riderId: String): Map<String, Any> {
        return try {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val snapshot = db.collection("orders")
                .whereEqualTo("riderId", riderId)
                .get()
                .await()
            
            val todayOrders = snapshot.documents.filter { 
                val date = it.getTimestamp("orderDate")
                date?.let { ts ->
                    val orderDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(ts.toDate())
                    orderDate == today
                } == true
            }
            
            val deliveries = todayOrders.count { it.getString("orderStatus") == "delivered" }
            val earnings = todayOrders.filter { it.getString("orderStatus") == "delivered" }
                .sumOf { it.getDouble("riderEarnings") ?: 0.0 }
            
            mapOf(
                "deliveries" to deliveries,
                "earnings" to earnings,
                "totalOrders" to todayOrders.size
            )
        } catch (e: Exception) {
            mapOf("deliveries" to 0, "earnings" to 0.0, "totalOrders" to 0)
        }
    }

    suspend fun getRiderActiveDeliveries(riderId: String): List<com.example.pickgo.models.rider.DeliveryOrder> {
        return try {
            val snapshot = db.collection("orders")
                .whereEqualTo("riderId", riderId)
                .whereIn("orderStatus", listOf("picked_up", "delivering"))
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                convertOrderToDeliveryOrder(doc)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAvailableDeliveryTrips(riderId: String, city: String?): List<com.example.pickgo.models.rider.DeliveryOrder> {
        return try {
            var query = db.collection("orders")
                .whereEqualTo("orderStatus", "ready_for_pickup")
            
            if (!city.isNullOrEmpty()) {
                query = query.whereEqualTo("merchantCity", city)
            }
            
            val snapshot = query.get().await()
            snapshot.documents.mapNotNull { doc ->
                convertOrderToDeliveryOrder(doc)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun convertOrderToDeliveryOrder(doc: com.google.firebase.firestore.DocumentSnapshot): com.example.pickgo.models.rider.DeliveryOrder? {
        return try {
            com.example.pickgo.models.rider.DeliveryOrder(
                id = doc.id,
                orderId = doc.getString("orderId") ?: "",
                customerId = doc.getString("customerId") ?: "",
                customerName = doc.getString("customerName") ?: "",
                customerPhone = doc.getString("customerPhone") ?: "",
                merchantId = doc.getString("merchantId") ?: "",
                merchantName = doc.getString("merchantName") ?: "",
                merchantAddress = doc.getString("merchantAddress") ?: "",
                merchantPhone = doc.getString("merchantPhone") ?: "",
                deliveryAddress = doc.getString("deliveryAddress") ?: "",
                orderTotal = doc.getDouble("orderTotal") ?: 0.0,
                riderEarnings = doc.getDouble("riderEarnings") ?: 0.0,
                orderStatus = doc.getString("orderStatus") ?: "pending",
                paymentMethod = doc.getString("paymentMethod") ?: "COD",
                orderDate = doc.getString("orderDate") ?: "",
                items = emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun acceptDeliveryOrder(riderId: String, orderId: String): Boolean {
        return try {
            updateRecord("orders", orderId, mapOf(
                "riderId" to riderId,
                "orderStatus" to "picked_up"
            ))
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchNextDeliveryRequest(riderId: String, city: String?): com.example.pickgo.models.rider.DeliveryRequest? {
        return try {
            var query = db.collection("orders")
                .whereEqualTo("orderStatus", "ready_for_pickup")
            
            if (!city.isNullOrEmpty()) {
                query = query.whereEqualTo("merchantCity", city)
            }
            
            val snapshot = query.limit(1).get().await()
            snapshot.documents.firstOrNull()?.let { doc ->
                com.example.pickgo.models.rider.DeliveryRequest(
                    id = doc.id,
                    pickup = doc.getString("merchantName") ?: "",
                    pickupAddress = doc.getString("merchantAddress") ?: "",
                    deliveryAddress = doc.getString("deliveryAddress") ?: "",
                    earnings = doc.getDouble("riderEarnings") ?: 0.0,
                    merchantName = doc.getString("merchantName") ?: "",
                    merchantCity = doc.getString("merchantCity") ?: ""
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateSeller(sellerId: String, updates: Map<String, Any>) {
        updateRecord("sellers", sellerId, updates)
    }

    suspend fun deleteSeller(sellerId: String) {
        deleteRecord("sellers", sellerId)
    }

    suspend fun getAllSellersWithMerchants(): List<AdminSeller> {
        return try {
            val snapshot = db.collection("sellers")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(AdminSeller::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateMerchant(merchantId: String, updates: Map<String, Any>) {
        updateRecord("merchants", merchantId, updates)
    }

    suspend fun deleteMerchant(merchantId: String) {
        deleteRecord("merchants", merchantId)
    }

    suspend fun getMerchantById(merchantId: String): Merchant? {
        return try {
            val doc = db.collection("merchants").document(merchantId).get().await()
            doc.toObject(Merchant::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getItemsByMerchant(merchantId: String): List<com.example.pickgo.models.Item> {
        return try {
            val snapshot = db.collection("items")
                .whereEqualTo("merchantId", merchantId)
                .whereEqualTo("itemStatus", "available")
                .get()
                .await()
            snapshot.toObjects(com.example.pickgo.models.Item::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getOrderById(orderId: String): com.example.pickgo.models.Order? {
        return try {
            val doc = db.collection("orders").document(orderId).get().await()
            doc.toObject(com.example.pickgo.models.Order::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRiderById(riderId: String): AdminRider? {
        return try {
            val doc = db.collection("riders").document(riderId).get().await()
            doc.toObject(AdminRider::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addReview(review: Review) {
        val reviewData = mapOf(
            "orderId" to review.orderId,
            "customerId" to review.customerId,
            "sellerId" to (review.sellerId ?: ""),
            "riderId" to (review.riderId ?: ""),
            "rating" to review.rating,
            "comment" to (review.comment ?: ""),
            "createdAt" to review.createdAt
        )
        addRecord("reviews", reviewData)
    }

    suspend fun addRiderTip(riderId: String, amount: Int, orderId: String) {
        val tipData = mapOf(
            "riderId" to riderId,
            "amount" to amount,
            "orderId" to orderId,
            "createdAt" to Date()
        )
        addRecord("tips", tipData)
    }

    suspend fun getUserAddresses(userId: String): List<Address> {
        return try {
            val snapshot = db.collection("addresses")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.toObjects(Address::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addAddress(address: Address) {
        val addressData = mapOf(
            "userId" to address.userId,
            "label" to address.label,
            "addressLine1" to address.addressLine1,
            "city" to address.city,
            "isDefault" to address.isDefault
        )
        addRecord("addresses", addressData)
    }

    suspend fun createOrder(order: com.example.pickgo.models.Order): String {
        val orderData = mapOf(
            "orderId" to order.orderId,
            "customerId" to order.customerId,
            "sellerId" to order.sellerId,
            "merchantName" to order.merchantName,
            "merchantId" to order.merchantId,
            "orderTotal" to order.orderTotal,
            "orderStatus" to order.orderStatus,
            "deliveryAddress" to order.deliveryAddress,
            "paymentMethod" to order.paymentMethod,
            "riderId" to (order.riderId ?: ""),
            "riderEarnings" to order.riderEarnings,
            "orderDate" to order.orderDate
        )
        return addRecord("orders", orderData)
    }

    suspend fun deleteAddress(addressId: String) {
        deleteRecord("addresses", addressId)
    }

    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        updateRecord("users", userId, updates)
    }

    suspend fun uploadProfileImage(userId: String, imageUri: android.net.Uri): String {
        return try {
            val imageRef = storage.child("profile_images/$userId.jpg")
            imageRef.putFile(imageUri).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun getRiderByEmail(email: String): com.example.pickgo.models.rider.Rider? {
        return try {
            val snapshot = db.collection("riders")
                .whereEqualTo("email", email)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toObject(com.example.pickgo.models.rider.Rider::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSellerByEmail(email: String): com.example.pickgo.models.seller.Seller? {
        return try {
            val snapshot = db.collection("sellers")
                .whereEqualTo("email", email)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toObject(com.example.pickgo.models.seller.Seller::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRiderReviews(riderId: String): List<com.example.pickgo.models.rider.RiderReview> {
        return try {
            val snapshot = db.collection("reviews")
                .whereEqualTo("riderId", riderId)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                com.example.pickgo.models.rider.RiderReview(
                    id = doc.id,
                    orderId = doc.getString("orderId") ?: "",
                    customerId = doc.getString("customerId") ?: "",
                    customerName = doc.getString("customerName") ?: "Customer",
                    customerInitials = "C",
                    riderId = doc.getString("riderId") ?: "",
                    rating = (doc.getLong("rating") ?: 5).toInt(),
                    comment = doc.getString("comment") ?: "",
                    createdAt = doc.getString("createdAt") ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun completeDelivery(orderId: String, proofImageUri: android.net.Uri) {
        val imageRef = storage.child("delivery_proof/$orderId.jpg")
        imageRef.putFile(proofImageUri).await()
        val imageUrl = imageRef.downloadUrl.await().toString()
        updateRecord("orders", orderId, mapOf(
            "orderStatus" to "delivered",
            "deliveryProofUrl" to imageUrl,
            "deliveredAt" to Date()
        ))
    }

    suspend fun cancelDelivery(orderId: String) {
        updateRecord("orders", orderId, mapOf(
            "orderStatus" to "cancelled"
        ))
    }

    suspend fun uploadRiderPhoto(riderId: String, imageUri: android.net.Uri): String {
        return try {
            val imageRef = storage.child("rider_photos/$riderId.jpg")
            imageRef.putFile(imageUri).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun getAllPayouts(): List<AdminPayout> {
        return try {
            val snapshot = db.collection("payouts")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(AdminPayout::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updatePayout(payoutId: String, updates: Map<String, Any>) {
        updateRecord("payouts", payoutId, updates)
    }

    suspend fun getAllItems(): List<com.example.pickgo.models.Item> {
        return try {
            val snapshot = db.collection("items")
                .get()
                .await()
            snapshot.toObjects(com.example.pickgo.models.Item::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSellerReviews(sellerId: String): List<com.example.pickgo.models.seller.SellerReview> {
        return try {
            val snapshot = db.collection("reviews")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                com.example.pickgo.models.seller.SellerReview(
                    id = doc.id,
                    orderId = doc.getString("orderId") ?: "",
                    customerId = doc.getString("customerId") ?: "",
                    customerName = doc.getString("customerName") ?: "Customer",
                    customerInitials = "C",
                    sellerId = doc.getString("sellerId") ?: "",
                    itemId = doc.getString("itemId"),
                    rating = (doc.getLong("rating") ?: 5).toInt(),
                    comment = doc.getString("comment") ?: "",
                    createdAt = doc.getString("createdAt") ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getItemById(itemId: String): com.example.pickgo.models.Item? {
        return try {
            val doc = db.collection("items").document(itemId).get().await()
            doc.toObject(com.example.pickgo.models.Item::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadMerchantLogo(merchantId: String, imageUri: android.net.Uri): String {
        return try {
            val imageRef = storage.child("merchant_logos/$merchantId.jpg")
            imageRef.putFile(imageUri).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun uploadMerchantBanner(merchantId: String, imageUri: android.net.Uri): String {
        return try {
            val imageRef = storage.child("merchant_banners/$merchantId.jpg")
            imageRef.putFile(imageUri).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun updateMerchantProfile(merchantId: String, updates: Map<String, Any>) {
        updateRecord("merchants", merchantId, updates)
    }

    suspend fun getSellerOrders(sellerId: String): List<com.example.pickgo.models.Order> {
        return try {
            val snapshot = db.collection("orders")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .await()
            snapshot.toObjects(com.example.pickgo.models.Order::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateOrderStatusSeller(orderId: String, status: String) {
        updateRecord("orders", orderId, mapOf("orderStatus" to status))
    }

    suspend fun getSellerItems(sellerId: String): List<com.example.pickgo.models.Item> {
        return try {
            val snapshot = db.collection("items")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .await()
            snapshot.toObjects(com.example.pickgo.models.Item::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addItem(item: com.example.pickgo.models.Item): String {
        val itemData = mapOf<String, Any>(
            "itemId" to item.itemId,
            "itemName" to item.itemName,
            "itemDescription" to item.itemDescription,
            "itemPrice" to item.itemPrice,
            "itemCategory" to item.itemCategory,
            "itemImage" to (item.itemImage ?: ""),
            "itemStatus" to item.itemStatus,
            "merchantId" to item.merchantId,
            "merchantName" to item.merchantName,
            "sellerId" to item.sellerId,
            "createdAt" to ""
        )
        return addRecord("items", itemData)
    }

    suspend fun updateItem(itemId: String, updates: Map<String, Any>) {
        updateRecord("items", itemId, updates)
    }

    suspend fun deleteItem(itemId: String) {
        deleteRecord("items", itemId)
    }

    suspend fun uploadItemImage(itemId: String, imageUri: android.net.Uri): String {
        return try {
            val imageRef = storage.child("item_images/$itemId.jpg")
            imageRef.putFile(imageUri).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun getSellerEarnings(sellerId: String): Map<String, Any> {
        return try {
            val snapshot = db.collection("orders")
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("orderStatus", "delivered")
                .get()
                .await()
            
            val totalEarnings = snapshot.documents.sumOf { 
                it.getDouble("orderTotal") ?: 0.0 
            }
            val totalOrders = snapshot.size()
            
            mapOf(
                "totalEarnings" to totalEarnings,
                "totalOrders" to totalOrders
            )
        } catch (e: Exception) {
            mapOf("totalEarnings" to 0.0, "totalOrders" to 0)
        }
    }

    suspend fun requestPayout(sellerId: String, amount: Double, bankDetails: Map<String, String>): String {
        val payoutData = mapOf(
            "sellerId" to sellerId,
            "amount" to amount,
            "bankName" to (bankDetails["bankName"] ?: ""),
            "accountNumber" to (bankDetails["accountNumber"] ?: ""),
            "accountName" to (bankDetails["accountName"] ?: ""),
            "payoutStatus" to "pending",
            "requestDate" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        )
        return addRecord("payouts", payoutData)
    }

    suspend fun getSellerPayouts(sellerId: String): List<com.example.pickgo.models.seller.SellerPayout> {
        return try {
            val snapshot = db.collection("payouts")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                com.example.pickgo.models.seller.SellerPayout(
                    id = doc.id,
                    sellerId = doc.getString("sellerId") ?: "",
                    amount = doc.getDouble("amount") ?: 0.0,
                    bankName = doc.getString("bankName") ?: "",
                    accountNumber = doc.getString("accountNumber") ?: "",
                    accountName = doc.getString("accountName") ?: "",
                    payoutStatus = doc.getString("payoutStatus") ?: "pending",
                    requestDate = doc.getString("requestDate") ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Rider earnings and payout methods
    suspend fun getRiderEarningsData(riderId: String): com.example.pickgo.models.rider.RiderEarnings {
        return try {
            // Get completed deliveries
            val ordersSnapshot = db.collection("orders")
                .whereEqualTo("riderId", riderId)
                .whereEqualTo("orderStatus", "delivered")
                .get()
                .await()
            
            val trips = ordersSnapshot.documents.mapNotNull { doc ->
                try {
                    com.example.pickgo.models.rider.DeliveryOrder(
                        id = doc.id,
                        orderId = doc.getString("orderId") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        customerName = doc.getString("customerName") ?: "Customer",
                        customerPhone = doc.getString("customerPhone") ?: "",
                        merchantId = doc.getString("merchantId") ?: "",
                        merchantName = doc.getString("merchantName") ?: "",
                        merchantAddress = doc.getString("merchantAddress") ?: "",
                        merchantPhone = doc.getString("merchantPhone") ?: "",
                        deliveryAddress = doc.getString("deliveryAddress") ?: "",
                        orderTotal = doc.getDouble("orderTotal") ?: 0.0,
                        riderEarnings = doc.getDouble("riderEarnings") ?: 0.0,
                        orderStatus = "delivered",
                        paymentMethod = doc.getString("paymentMethod") ?: "COD",
                        orderDate = doc.getString("orderDate") ?: "",
                        items = emptyList()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            // Get payout history
            val payoutsSnapshot = db.collection("payouts")
                .whereEqualTo("userId", riderId)
                .whereEqualTo("userType", "rider")
                .get()
                .await()
            
            val payouts = payoutsSnapshot.documents.map { doc ->
                com.example.pickgo.models.rider.PayoutRequest(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    userType = "rider",
                    amount = doc.getDouble("amount") ?: 0.0,
                    bankName = doc.getString("bankName") ?: "",
                    accountNumber = doc.getString("accountNumber") ?: "",
                    accountName = doc.getString("accountName") ?: "",
                    payoutStatus = doc.getString("payoutStatus") ?: "pending",
                    requestDate = doc.getString("requestDate") ?: ""
                )
            }
            
            // Calculate withdrawable balance
            val totalEarnings = trips.sumOf { it.riderEarnings }
            val totalPayouts = payouts.filter { it.payoutStatus == "completed" }.sumOf { it.amount }
            val withdrawableBalance = totalEarnings - totalPayouts
            
            com.example.pickgo.models.rider.RiderEarnings(
                todayTrips = 0,
                todayEarnings = 0.0,
                totalEarnings = totalEarnings,
                withdrawableBalance = withdrawableBalance,
                trips = trips,
                payouts = payouts
            )
        } catch (e: Exception) {
            com.example.pickgo.models.rider.RiderEarnings(
                withdrawableBalance = 0.0
            )
        }
    }

    suspend fun createPayoutRequest(payoutRequest: com.example.pickgo.models.rider.PayoutRequest): String {
        val payoutData = mapOf(
            "userId" to payoutRequest.userId,
            "userType" to payoutRequest.userType,
            "amount" to payoutRequest.amount,
            "bankName" to payoutRequest.bankName,
            "accountNumber" to payoutRequest.accountNumber,
            "accountName" to payoutRequest.accountName,
            "payoutStatus" to payoutRequest.payoutStatus,
            "requestDate" to payoutRequest.requestDate
        )
        return addRecord("payouts", payoutData)
    }

    suspend fun createPayoutRequest(payoutRequest: com.example.pickgo.models.seller.PayoutRequest): String {
        val payoutData = mapOf(
            "userId" to payoutRequest.userId,
            "userType" to payoutRequest.userType,
            "amount" to payoutRequest.amount,
            "bankName" to payoutRequest.bankName,
            "accountNumber" to payoutRequest.accountNumber,
            "accountName" to payoutRequest.accountName,
            "payoutStatus" to payoutRequest.payoutStatus,
            "requestDate" to payoutRequest.requestDate
        )
        return addRecord("payouts", payoutData)
    }

    // Seller category methods
    suspend fun getCategoryNames(): List<String> {
        return try {
            val categories = getCategories()
            categories.map { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Seller-specific item methods with SellerItem type
    suspend fun getSellerItemsAsSellerItems(sellerId: String): List<com.example.pickgo.models.seller.SellerItem> {
        return try {
            val items = getSellerItems(sellerId)
            items.map { item ->
                com.example.pickgo.models.seller.SellerItem(
                    id = item.id,
                    itemId = item.itemId,
                    sellerId = item.sellerId,
                    itemName = item.itemName,
                    itemDescription = item.itemDescription,
                    itemPrice = item.itemPrice,
                    itemCategory = item.itemCategory,
                    itemImage = item.itemImage,
                    itemStatus = item.itemStatus
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addItemAsSellerItem(item: com.example.pickgo.models.seller.SellerItem): String {
        val itemData = mapOf(
            "itemId" to item.itemId,
            "sellerId" to item.sellerId,
            "itemName" to item.itemName,
            "itemDescription" to item.itemDescription,
            "itemPrice" to item.itemPrice,
            "itemCategory" to item.itemCategory,
            "itemImage" to (item.itemImage ?: ""),
            "itemStatus" to item.itemStatus
        )
        return addRecord("items", itemData)
    }

    // Seller order methods
    suspend fun getOrderItems(sellerId: String): List<com.example.pickgo.models.seller.SellerOrder> {
        return try {
            val orders = getSellerOrders(sellerId)
            orders.map { order ->
                com.example.pickgo.models.seller.SellerOrder(
                    id = order.id,
                    orderId = order.orderId,
                    customerId = order.customerId,
                    sellerId = order.sellerId,
                    merchantName = order.merchantName,
                    customerName = "Customer",
                    customerPhone = "",
                    orderTotal = order.orderTotal,
                    orderStatus = order.orderStatus,
                    deliveryAddress = order.deliveryAddress,
                    paymentMethod = order.paymentMethod,
                    orderDate = order.orderDate.toString(),
                    items = emptyList() // Order items would need to be fetched from order_items collection
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllOrderItems(sellerId: String): List<com.example.pickgo.models.seller.SellerOrder> {
        return getOrderItems(sellerId)
    }

    // Helper methods for database operations
    private suspend fun getAll(collection: String): QuerySnapshot {
        return db.collection(collection)
            .get()
            .await()
    }

    private suspend fun addRecord(collection: String, data: Map<String, Any>): String {
        val docRef = db.collection(collection).add(data).await()
        return docRef.id
    }

    private suspend fun updateRecord(collection: String, docId: String, data: Map<String, Any>) {
        db.collection(collection).document(docId)
            .update(data)
            .await()
    }

    private suspend fun deleteRecord(collection: String, docId: String) {
        db.collection(collection).document(docId)
            .delete()
            .await()
    }

    private suspend fun queryOne(collection: String, field: String, value: String): com.google.firebase.firestore.DocumentSnapshot? {
        val snapshot = db.collection(collection)
            .whereEqualTo(field, value)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()
    }

    private suspend fun getSettingsMap(): Map<String, Any> {
        return try {
            val snapshot = db.collection("settings")
                .get()
                .await()
            snapshot.documents.associate { doc ->
                val key = doc.getString("Setting_Key") ?: ""
                val value = doc.get("Setting_Value") ?: ""
                key to value
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun getItemsWithMerchants(): List<com.example.pickgo.models.Item> {
        return getAllItems()
    }
}