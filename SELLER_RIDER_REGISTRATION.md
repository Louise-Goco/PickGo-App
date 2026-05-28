# Seller & Rider Registration Implementation

## Overview
Created comprehensive registration activities for Sellers and Riders with document upload capabilities, integrated with Firebase for application submission and approval workflow.

## Files Created

### Activities
1. **SellerRegisterActivity.kt** - Seller registration with document upload
2. **RiderRegisterActivity.kt** - Rider registration with document upload

### Layouts
1. **activity_seller_register.xml** - Seller registration UI
2. **activity_rider_register.xml** - Rider registration UI

### Resources
1. **ic_delivery.xml** - Delivery truck icon for rider registration

### Configuration
- Updated **AndroidManifest.xml** to register both activities

## Features

### Seller Registration
**Personal Information:**
- First Name & Last Name
- Email Address
- Phone Number
- Password & Confirm Password (min 6 characters)

**Store Information:**
- Store Name
- Store Type (e.g., Restaurant, Cafe, Shop)
- Store Phone
- Store Email

**Store Address:**
- Street Name
- Barangay
- City

**Required Documents:**
- Government ID (upload image/PDF)
- BIR Certificate (upload image/PDF)

**Workflow:**
1. User fills in all required fields
2. Uploads required documents
3. Submits application
4. Application is stored in Firestore with status "PENDING"
5. User account created with `userType: SELLER` and `accountStatus: PENDING`
6. Admin reviews and approves/rejects application

### Rider Registration
**Personal Information:**
- First Name & Last Name
- Email Address
- Phone Number
- Password & Confirm Password (min 6 characters)

**Vehicle Information:**
- Vehicle Type (dropdown: Motorcycle, Bicycle, E-Bike, Car, Van)
- Plate Number
- License Number

**Required Documents:**
- Driver's License Photo (upload image/PDF)
- NBI Clearance (upload image/PDF)
- Official Receipt - OR (upload image/PDF)
- Certificate of Registration - CR (upload image/PDF)

**Workflow:**
1. User fills in all required fields
2. Selects vehicle type from dropdown
3. Uploads all 4 required documents
4. Submits application
5. Application is stored in Firestore with status "PENDING"
6. User account created with `userType: RIDER` and `accountStatus: PENDING`
7. Admin reviews and approves/rejects application

## Firebase Integration

### Firestore Collections Used
- **users** - User account data
- **seller_applications** - Seller application records
- **rider_applications** - Rider application records
- **seller_applications/{userId}/** - Document storage path
- **rider_applications/{userId}/** - Document storage path

### Firebase Storage
Documents are uploaded to:
- `seller_applications/{userId}/gov_id` - Government ID
- `seller_applications/{userId}/bir_cert` - BIR Certificate
- `rider_applications/{userId}/license_photo` - License Photo
- `rider_applications/{userId}/nbi` - NBI Clearance
- `rider_applications/{userId}/or` - Official Receipt
- `rider_applications/{userId}/cr` - Certificate of Registration

### Existing FirebaseManager Methods Used
- `submitSellerApplication(application: SellerApplication, files: Map<String, Uri>)` - Submits seller application
- `submitRiderApplication(application: RiderApplication, files: Map<String, Uri>)` - Submits rider application

## Validation

### Seller Registration Validation
✅ All personal info fields required
✅ All store info fields required
✅ All address fields required
✅ Password match verification
✅ Password minimum 6 characters
✅ Government ID must be uploaded
✅ BIR Certificate must be uploaded
✅ Terms & conditions must be accepted

### Rider Registration Validation
✅ All personal info fields required
✅ Vehicle type must be selected
✅ Plate number required
✅ License number required
✅ All 4 documents must be uploaded (License, NBI, OR, CR)
✅ Password match verification
✅ Password minimum 6 characters
✅ Terms & conditions must be accepted

## UI Features

### Common Features
- **Back button** - Navigate back to previous screen
- **Error messages** - Red banner with error icon
- **Success messages** - Green banner with success icon
- **Progress indicator** - Shows during submission
- **Document status** - Shows selected file name after upload
- **Terms checkbox** - Required before submission
- **Sign in link** - Navigate to login if already has account

### Material Design Components
- TextInputLayout with outlined boxes
- Password visibility toggle
- Material buttons with proper styling
- CardView for form container
- ScrollView for long forms
- AutoCompleteTextView for vehicle type dropdown

## How to Access Registration

### From LoginActivity
Currently, the LoginActivity has a "Create one" button that opens the customer registration (RegisterActivity).

To add seller/rider registration buttons to the login screen, you can:

**Option 1: Add buttons below the existing "Create one" link**
```xml
<!-- Add to activity_login.xml -->
<Button
    android:id="@+id/sellerRegisterBtn"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Register as Seller"
    style="@style/Widget.MaterialComponents.Button.OutlinedButton" />

<Button
    android:id="@+id/riderRegisterBtn"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Register as Rider"
    style="@style/Widget.MaterialComponents.Button.OutlinedButton" />
```

**Option 2: Create a registration type selection screen**
Create a new activity that lets users choose their registration type (Customer, Seller, or Rider) before proceeding to the appropriate registration form.

### Navigation Code Example
```kotlin
// In LoginActivity.kt
binding.sellerRegisterBtn.setOnClickListener {
    startActivity(Intent(this, SellerRegisterActivity::class.java))
}

binding.riderRegisterBtn.setOnClickListener {
    startActivity(Intent(this, RiderRegisterActivity::class.java))
}
```

## Approval Workflow

### After Submission
1. Application is saved to Firestore with `status: PENDING`
2. Documents are uploaded to Firebase Storage
3. User account is created with `accountStatus: PENDING`
4. User sees success message

### Admin Review
Admin can review applications in:
- **ManageSellersActivity** - Review seller applications
- **ManageRidersActivity** - Review rider applications

Admin can:
- **Approve** - Changes status to `APPROVED` and `accountStatus: ACTIVE`
- **Reject** - Changes status to `REJECTED` and `accountStatus: SUSPENDED`

### User Login After Approval
- User tries to login with email/password
- System checks `accountStatus`
- If `ACTIVE` → Login successful
- If `PENDING` → Show message: "Your application is pending approval"
- If `SUSPENDED` → Show message: "Your application was rejected"

## Testing Checklist

### Seller Registration
- [ ] Fill all fields correctly
- [ ] Upload Government ID
- [ ] Upload BIR Certificate
- [ ] Submit successfully
- [ ] Verify error messages for missing fields
- [ ] Verify password validation
- [ ] Verify document upload works
- [ ] Check Firestore for application record
- [ ] Check Firebase Storage for uploaded documents

### Rider Registration
- [ ] Fill all fields correctly
- [ ] Select vehicle type
- [ ] Upload all 4 documents
- [ ] Submit successfully
- [ ] Verify error messages for missing fields
- [ ] Verify password validation
- [ ] Verify dropdown works
- [ ] Check Firestore for application record
- [ ] Check Firebase Storage for uploaded documents

## Future Enhancements

1. **Email Notifications** - Send email when application is approved/rejected
2. **Application Status Tracking** - Let users check their application status
3. **Document Preview** - Preview uploaded documents before submission
4. **Camera Integration** - Take photos directly instead of picking from gallery
5. **Image Compression** - Compress images before upload to save storage
6. **Progress Tracking** - Show upload progress for large files
7. **Edit Application** - Allow users to edit pending applications
8. **Resubmission** - Allow rejected users to resubmit with updated documents

## Notes

- Both registration forms use the existing FirebaseManager methods
- Document uploads are handled automatically by FirebaseManager
- Passwords are hashed using PasswordHasher before storage
- All operations are performed in coroutines (lifecycleScope)
- Forms clear after successful submission
- Error handling with user-friendly messages
- Progress indicators during submission
- All required icons must exist in drawable folder

## Dependencies Required

All dependencies are already in the project:
- Firebase Auth
- Firebase Firestore
- Firebase Storage
- Material Components
- Activity Result API (for document picker)
- Coroutines

No additional dependencies needed!
