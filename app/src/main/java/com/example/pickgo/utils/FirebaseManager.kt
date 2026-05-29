package com.example.pickgo.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.example.pickgo.models.*
import com.example.pickgo.models.admin.*
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseManager(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // First, try to find if this email belongs to a seller's store email
            val sellerQuery = db.collection("users")
                .whereEqualTo("storeEmail", email)
                .whereEqualTo("userType", "SELLER")
                .limit(1)
                .get()
                .await()
            
            val userId = if (!sellerQuery.isEmpty) {
                // Found seller by store email, use their auth email
                val sellerDoc = sellerQuery.documents[0]
                val sellerAuthEmail = sellerDoc.getString("email").orEmpty()
                if (sellerAuthEmail.isEmpty()) {
                    return Result.failure(Exception("Seller account configuration error"))
                }
                android.util.Log.d("FirebaseAuth", "Login attempt with store email, using auth email: $sellerAuthEmail")
                
                // Sign in with the auth email (personal email)
                val authResult = auth.signInWithEmailAndPassword(sellerAuthEmail, password).await()
                authResult.user?.uid ?: return Result.failure(Exception("Authentication failed"))
            } else {
                // Not a store email, try direct login with provided email
                android.util.Log.d("FirebaseAuth", "Direct login attempt with email: $email")
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                authResult.user?.uid ?: return Result.failure(Exception("Authentication failed"))
            }

            val userDoc = db.collection("users").document(userId).get().await()
            val user = userDoc.toObject(User::class.java) ?: return Result.failure(Exception("User data not found"))

            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuth", "Login failed: ${e.message}", e)
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

    suspend fun submitRiderApplication(application: RiderApplication, files: Map<String, Uri>, password: String? = null): Result<String> {
        return try {
            var userId = auth.currentUser?.uid
            android.util.Log.d("FirebaseAuth", "Current user before: $userId")
            
            // Create auth account if user doesn't exist yet
            if (userId == null && password != null) {
                android.util.Log.d("FirebaseAuth", "Creating new user account...")
                val authResult = auth.createUserWithEmailAndPassword(application.email, password).await()
                userId = authResult.user?.uid
                android.util.Log.d("FirebaseAuth", "User created successfully: $userId")
                
                if (userId == null) {
                    return Result.failure(Exception("Failed to create user account"))
                }
                
                // Wait a moment for auth state to sync
                kotlinx.coroutines.delay(500)
                
                // Verify user is now authenticated
                val currentUser = auth.currentUser
                if (currentUser == null || currentUser.uid != userId) {
                    android.util.Log.e("FirebaseAuth", "Auth state not synced after creation")
                    return Result.failure(Exception("Authentication failed. Please try again."))
                }
                android.util.Log.d("FirebaseAuth", "Auth state verified: ${currentUser.uid}")
            } else if (userId == null) {
                return Result.failure(Exception("User not logged in and no password provided"))
            }
            
            // Ensure userId is valid before proceeding
            if (userId.isEmpty()) {
                return Result.failure(Exception("Invalid user ID"))
            }
            
            android.util.Log.d("FirebaseAuth", "Proceeding with registration for userId: $userId")

            // Upload files - SKIPPED: Firebase Storage requires Blaze plan
            // Using placeholder URLs instead for development/testing
            val fileUrls = mutableMapOf<String, String>()
            for ((key, uri) in files) {
                android.util.Log.d("FirebaseStorage", "Skipping upload for: $key (Storage requires Blaze plan)")
                // Use placeholder URL instead of actual upload
                fileUrls[key] = "placeholder_url_${key}_${userId}"
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

            // Hash password if provided
            val hashedPassword = password?.let { PasswordHasher.hashPassword(it) } ?: ""

            // Check if user document exists
            val userDocRef = db.collection("users").document(userId)
            val userDoc = userDocRef.get().await()
            
            if (userDoc.exists()) {
                // Update existing user document
                userDocRef.update(
                    mapOf<String, Any>(
                        "userType" to UserType.RIDER.name,
                        "accountStatus" to AccountStatus.PENDING.name
                    )
                ).await()
            } else {
                // Create new user document
                userDocRef.set(
                    mapOf<String, Any>(
                        "firstName" to application.firstName,
                        "lastName" to application.lastName,
                        "email" to application.email,
                        "phoneNumber" to application.phoneNumber,
                        "password" to hashedPassword,
                        "userType" to UserType.RIDER.name,
                        "accountStatus" to AccountStatus.PENDING.name,
                        "isVerified" to false,
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitSellerApplication(application: SellerApplication, files: Map<String, Uri>, password: String? = null): Result<String> {
        return try {
            var userId = auth.currentUser?.uid
            android.util.Log.d("FirebaseAuth", "Current user before: $userId")
            
            // Create auth account if user doesn't exist yet
            if (userId == null && password != null) {
                android.util.Log.d("FirebaseAuth", "Creating new user account...")
                val authResult = auth.createUserWithEmailAndPassword(application.email, password).await()
                userId = authResult.user?.uid
                android.util.Log.d("FirebaseAuth", "User created successfully: $userId")
                
                if (userId == null) {
                    return Result.failure(Exception("Failed to create user account"))
                }
                
                // Wait a moment for auth state to sync
                kotlinx.coroutines.delay(500)
                
                // Verify user is now authenticated
                val currentUser = auth.currentUser
                if (currentUser == null || currentUser.uid != userId) {
                    android.util.Log.e("FirebaseAuth", "Auth state not synced after creation")
                    return Result.failure(Exception("Authentication failed. Please try again."))
                }
                android.util.Log.d("FirebaseAuth", "Auth state verified: ${currentUser.uid}")
            } else if (userId == null) {
                return Result.failure(Exception("User not logged in and no password provided"))
            }
            
            // Ensure userId is valid before proceeding
            if (userId.isEmpty()) {
                return Result.failure(Exception("Invalid user ID"))
            }
            
            android.util.Log.d("FirebaseAuth", "Proceeding with registration for userId: $userId")

            // Upload files - SKIPPED: Firebase Storage requires Blaze plan
            // Using placeholder URLs instead for development/testing
            val fileUrls = mutableMapOf<String, String>()
            for ((key, uri) in files) {
                android.util.Log.d("FirebaseStorage", "Skipping upload for: $key (Storage requires Blaze plan)")
                // Use placeholder URL instead of actual upload
                fileUrls[key] = "placeholder_url_${key}_${userId}"
            }

            val completeApplication = application.copy(
                userId = userId,
                govIdUrl = fileUrls["gov_id"],
                birCertUrl = fileUrls["bir_cert"],
                submittedAt = java.util.Date()
            )

            val docRef = db.collection("seller_applications").document(userId)
            docRef.set(completeApplication).await()

            // Hash password if provided
            val hashedPassword = password?.let { PasswordHasher.hashPassword(it) } ?: ""

            // Check if user document exists
            val userDocRef = db.collection("users").document(userId)
            val userDoc = userDocRef.get().await()
            
            if (userDoc.exists()) {
                // Update existing user document
                userDocRef.update(
                    mapOf<String, Any>(
                        "userType" to UserType.SELLER.name,
                        "accountStatus" to AccountStatus.PENDING.name,
                        "storeEmail" to application.storeEmail
                    )
                ).await()
            } else {
                // Create new user document
                userDocRef.set(
                    mapOf<String, Any>(
                        "firstName" to application.firstName,
                        "lastName" to application.lastName,
                        "email" to application.email,
                        "storeEmail" to application.storeEmail,
                        "phoneNumber" to application.phoneNumber,
                        "password" to hashedPassword,
                        "userType" to UserType.SELLER.name,
                        "accountStatus" to AccountStatus.PENDING.name,
                        "isVerified" to false,
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()
            }

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
                .whereEqualTo("userType", "CUSTOMER")
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
            val snapshot = db.collection("users")
                .whereEqualTo("userType", "SELLER")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.let { user ->
                    AdminSeller(
                        id = user.id,
                        sellerId = user.id,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        email = user.email,
                        phoneNumber = user.phoneNumber,
                        merchantName = "",  // Will be populated from merchants collection
                        sellerStatus = (user.accountStatus ?: AccountStatus.ACTIVE).name.lowercase(),
                        sellerRating = 0.0,
                        createdAt = user.createdAt?.toString() ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllRiders(): List<AdminRider> {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("userType", "RIDER")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.let { user ->
                    AdminRider(
                        id = user.id,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        email = user.email,
                        phoneNumber = user.phoneNumber,
                        riderStatus = (user.accountStatus ?: AccountStatus.ACTIVE).name.lowercase(),
                        riderRating = 0.0,
                        createdAt = user.createdAt?.toString() ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllRidersWithDocuments(): List<AdminRider> {
        return try {
            // Get rider applications
            val applicationsSnapshot = db.collection("rider_applications")
                .get()
                .await()
            
            applicationsSnapshot.documents.mapNotNull { doc ->
                try {
                    val application = doc.toObject(RiderApplication::class.java)
                    application?.let { app ->
                        AdminRider(
                            id = app.userId,
                            firstName = app.firstName,
                            lastName = app.lastName,
                            email = app.email,
                            phoneNumber = app.phoneNumber,
                            vehicleType = app.vehicleType,
                            plateNumber = app.plateNumber,
                            licenseNumber = app.licenseNumber,
                            riderStatus = app.status.name.lowercase(),
                            riderRating = 0.0,
                            isVerified = false,
                            licensePhotoUrl = app.licensePhotoUrl,
                            nbiUrl = app.nbiUrl,
                            orUrl = app.orUrl,
                            crUrl = app.crUrl,
                            createdAt = app.submittedAt?.toString() ?: ""
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseManager", "Error parsing rider application: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    
    suspend fun updateRider(riderId: String, updates: Map<String, Any>) {
        updateRecord("users", riderId, updates)
    }

    suspend fun deleteRider(riderId: String) {
        deleteRecord("users", riderId)
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

    suspend fun updateSellerStatus(sellerId: String, status: String): Result<Unit> {
        return try {
            // Map status to correct enum values
            val applicationStatus = when (status.uppercase()) {
                "APPROVED", "ACTIVE" -> ApplicationStatus.APPROVED
                "REJECTED" -> ApplicationStatus.REJECTED
                else -> ApplicationStatus.PENDING
            }
            
            val accountStatus = when (status.uppercase()) {
                "APPROVED", "ACTIVE" -> AccountStatus.ACTIVE
                "REJECTED" -> AccountStatus.PENDING  // Keep as PENDING, not SUSPENDED
                "SUSPENDED" -> AccountStatus.SUSPENDED
                else -> AccountStatus.PENDING
            }
            
            // Update application status
            db.collection("seller_applications").document(sellerId)
                .update("status", applicationStatus)
                .await()
            
            // Update user account status
            db.collection("users").document(sellerId)
                .update("accountStatus", accountStatus)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error updating seller status: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateRiderStatus(riderId: String, status: String): Result<Unit> {
        return try {
            // Map status to correct enum values
            val applicationStatus = when (status.uppercase()) {
                "APPROVED", "ACTIVE" -> ApplicationStatus.APPROVED
                "REJECTED" -> ApplicationStatus.REJECTED
                else -> ApplicationStatus.PENDING
            }
            
            val accountStatus = when (status.uppercase()) {
                "APPROVED", "ACTIVE" -> AccountStatus.ACTIVE
                "REJECTED" -> AccountStatus.PENDING  // Keep as PENDING, not SUSPENDED
                "SUSPENDED" -> AccountStatus.SUSPENDED
                else -> AccountStatus.PENDING
            }
            
            // Update application status
            db.collection("rider_applications").document(riderId)
                .update("status", applicationStatus)
                .await()
            
            // Update user account status
            db.collection("users").document(riderId)
                .update("accountStatus", accountStatus)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error updating rider status: ${e.message}", e)
            Result.failure(e)
        }
    }

    
    suspend fun updateSeller(sellerId: String, updates: Map<String, Any>) {
        updateRecord("users", sellerId, updates)
    }

    suspend fun deleteSeller(sellerId: String) {
        deleteRecord("users", sellerId)
    }

    suspend fun getAllSellersWithMerchants(): List<AdminSeller> {
        return try {
            // Get seller applications
            val applicationsSnapshot = db.collection("seller_applications")
                .get()
                .await()
            
            applicationsSnapshot.documents.mapNotNull { doc ->
                try {
                    val application = doc.toObject(SellerApplication::class.java)
                    application?.let { app ->
                        AdminSeller(
                            id = app.userId,
                            sellerId = app.userId,
                            firstName = app.firstName,
                            lastName = app.lastName,
                            email = app.email,
                            phoneNumber = app.phoneNumber,
                            merchantName = app.storeName,
                            merchantType = app.storeType,
                            sellerStatus = app.status.name.lowercase(),
                            sellerRating = 0.0,
                            govIdUrl = app.govIdUrl,
                            birCertUrl = app.birCertUrl,
                            createdAt = app.submittedAt?.toString() ?: ""
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseManager", "Error parsing seller application: ${e.message}")
                    null
                }
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

    suspend fun uploadItemImage(itemId: String, category: String, imageUri: android.net.Uri): String {
        return try {
            android.util.Log.d("FirebaseStorage", "Uploading item image for: $itemId in category: $category")
            
            // Sanitize category name for folder path (replace spaces and special characters)
            val sanitizedCategory = category
                .lowercase()
                .replace(Regex("[^a-z0-9_]"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')
            
            // Read image from URI
            val inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                android.util.Log.e("FirebaseStorage", "Cannot open image file")
                throw Exception("Cannot read image file")
            }
            
            val imageBytes = inputStream.readBytes()
            inputStream.close()
            
            android.util.Log.d("FirebaseStorage", "Image read: ${imageBytes.size} bytes")
            
            // Upload to category-specific folder: item_images/{category}/{itemId}.jpg
            val imageRef = storage.child("item_images/$sanitizedCategory/$itemId.jpg")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()
            
            // Upload as bytes
            imageRef.putBytes(imageBytes, metadata).await()
            
            android.util.Log.d("FirebaseStorage", "Item image uploaded to: item_images/$sanitizedCategory/$itemId.jpg")
            
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorage", "Failed to upload item image: ${e.message}", e)
            throw Exception("Failed to upload image: ${e.message}")
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

    private suspend fun uploadRegistrationDocument(basePath: String, key: String, uri: Uri): String {
        return try {
            android.util.Log.d("FirebaseStorage", "Starting upload: $basePath/$key from URI: $uri")
            
            val resolver = context.contentResolver
            val mimeType = resolveMimeType(uri)
            val extension = extensionForMimeType(mimeType, uri)

            // Verify file is accessible and not empty
            val inputStream = resolver.openInputStream(uri)
            if (inputStream == null) {
                throw Exception("Cannot open file. Please try selecting the file again.")
            }
            
            val fileBytes = inputStream.readBytes()
            inputStream.close()
            
            if (fileBytes.isEmpty()) {
                throw Exception("Selected file is empty")
            }
            
            android.util.Log.d("FirebaseStorage", "File read successfully: ${fileBytes.size} bytes")

            val fileRef = storage.child("$basePath/${key}$extension")
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .build()

            android.util.Log.d("FirebaseStorage", "Uploading to: ${fileRef.path}")
            
            // Use putBytes instead of putFile for better reliability
            // Workaround for ControllableTask .await() bug in older coroutines-play-services
            kotlin.coroutines.suspendCoroutine<com.google.firebase.storage.UploadTask.TaskSnapshot> { cont ->
                fileRef.putBytes(fileBytes, metadata)
                    .addOnSuccessListener { cont.resumeWith(Result.success(it)) }
                    .addOnFailureListener { cont.resumeWith(Result.failure(it)) }
            }
            
            android.util.Log.d("FirebaseStorage", "Upload successful")
            
            val downloadUrl = fileRef.downloadUrl.await().toString()
            android.util.Log.d("FirebaseStorage", "Download URL obtained: $downloadUrl")
            
            downloadUrl
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorage", "Upload failed: ${e.message}", e)
            throw Exception("Failed to upload $key: ${e.message ?: "Unknown error"}. Please check your internet connection and try again.")
        }
    }

    private fun resolveMimeType(uri: Uri): String {
        context.contentResolver.getType(uri)?.let { return it }
        return when (extensionFromUri(uri)) {
            ".pdf" -> "application/pdf"
            ".png" -> "image/png"
            ".webp" -> "image/webp"
            ".jpg", ".jpeg" -> "image/jpeg"
            else -> "image/jpeg"
        }
    }

    private fun extensionFromUri(uri: Uri): String {
        val name = queryDisplayName(uri)?.lowercase() ?: return ""
        return when {
            name.endsWith(".pdf") -> ".pdf"
            name.endsWith(".png") -> ".png"
            name.endsWith(".webp") -> ".webp"
            name.endsWith(".jpeg") -> ".jpeg"
            name.endsWith(".jpg") -> ".jpg"
            else -> ""
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    return cursor.getString(index)
                }
            }
        }
        return uri.lastPathSegment
    }

    private fun extensionForMimeType(mimeType: String, uri: Uri): String {
        extensionFromUri(uri).takeIf { it.isNotEmpty() }?.let { return it }
        return when {
            mimeType == "application/pdf" -> ".pdf"
            mimeType == "image/png" -> ".png"
            mimeType == "image/webp" -> ".webp"
            mimeType == "image/jpeg" || mimeType == "image/jpg" -> ".jpg"
            mimeType.startsWith("image/") -> ".jpg"
            else -> ".jpg"
        }
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