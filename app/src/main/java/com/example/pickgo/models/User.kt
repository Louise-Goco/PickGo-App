package com.example.pickgo.models

import java.util.Date

data class User(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val storeEmail: String? = null,  // For sellers - store email used for login
    val phoneNumber: String = "",
    val password: String = "",
    val userType: UserType = UserType.CUSTOMER,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    val isVerified: Boolean = false,
    val profilePhoto: String? = null,
    val displayName: String? = null,
    val createdAt: java.util.Date = java.util.Date()
)

enum class UserType {
    CUSTOMER, RIDER, SELLER, ADMIN
}

enum class AccountStatus {
    ACTIVE, PENDING, SUSPENDED
}

data class RiderApplication(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val password: String = "",
    val vehicleType: String = "",
    val plateNumber: String = "",
    val licenseNumber: String = "",
    val licensePhotoUrl: String? = null,
    val nbiUrl: String? = null,
    val orUrl: String? = null,
    val crUrl: String? = null,
    val userType: UserType = UserType.RIDER,
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val submittedAt: Date = Date()
)

data class SellerApplication(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val password: String = "",
    val storeName: String = "",
    val storeType: String = "",
    val storePhone: String = "",
    val storeEmail: String = "",
    val building: String = "",
    val unitFloor: String = "",
    val streetNo: String = "",
    val streetName: String = "",
    val city: String = "",
    val barangay: String = "",
    val province: String = "",
    val zip: String = "",
    val landmark: String = "",
    val govIdUrl: String? = null,
    val birCertUrl: String? = null,
    val userType: UserType = UserType.SELLER,
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val submittedAt: Date = Date()
)

enum class ApplicationStatus {
    PENDING, APPROVED, REJECTED
}
