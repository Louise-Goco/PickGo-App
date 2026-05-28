package com.example.pickgo.activities.customer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.databinding.ActivityCheckoutBinding
import com.example.pickgo.models.Address
import com.example.pickgo.models.Order
import com.example.pickgo.models.OrderItem
import com.example.pickgo.utils.CartManager
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.PriceFormatter
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class CheckoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var cartManager: CartManager
    private lateinit var sessionManager: SessionManager
    private var selectedAddress: Address? = null
    private var selectedPaymentMethod: String = "COD"
    private var savedAddresses: List<Address> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        cartManager = CartManager(this)
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Checkout"

        setupPaymentMethods()
        loadAddresses()
        loadOrderSummary()
        setupClickListeners()
    }

    private fun setupPaymentMethods() {
        binding.paymentGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedPaymentMethod = when (checkedId) {
                R.id.paymentCod -> "COD"
                R.id.paymentCard -> "Card"
                R.id.paymentGcash -> "GCash"
                else -> "COD"
            }
        }
    }

    private fun loadAddresses() {
        lifecycleScope.launch {
            val user = firebaseManager.getCurrentUser()
            user?.let {
                savedAddresses = firebaseManager.getUserAddresses(it.id)
                displayAddresses()
            }
        }
    }

    private fun displayAddresses() {
        val addressesContainer = binding.addressesContainer
        addressesContainer.removeAllViews()

        if (savedAddresses.isEmpty()) {
            binding.noAddressesText.visibility = View.VISIBLE
            return
        }

        binding.noAddressesText.visibility = View.GONE

        savedAddresses.forEach { address ->
            val addressView = createAddressView(address)
            addressesContainer.addView(addressView)
        }

        if (savedAddresses.isNotEmpty()) {
            selectedAddress = savedAddresses.first()
            updateSelectedAddress(savedAddresses.first())
        }
    }

    private fun createAddressView(address: Address): View {
        val cardView = com.google.android.material.card.MaterialCardView(this).apply {
            layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            cardElevation = 2f
            radius = 12f
            setCardBackgroundColor(getColor(R.color.glass_bg))
            isClickable = true
            isFocusable = true

            setOnClickListener {
                selectedAddress = address
                updateSelectedAddress(address)
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val titleText = android.widget.TextView(this).apply {
            text = address.label
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
        }
        layout.addView(titleText)

        val addressText = android.widget.TextView(this).apply {
            text = "${address.addressLine1}, ${address.city}"
            textSize = 14f
            setTextColor(getColor(R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 4 }
        }
        layout.addView(addressText)

        cardView.addView(layout)
        return cardView
    }

    private fun updateSelectedAddress(address: Address) {
        binding.fullAddress.setText("${address.addressLine1}, ${address.city}")
    }

    private fun loadOrderSummary() {
        val items = cartManager.getCartItems()
        val subtotal = items.sumOf { it.lineTotal }
        val deliveryFee = 49.0
        val total = subtotal + deliveryFee

        val itemsText = StringBuilder()
        items.forEach { item ->
            itemsText.append("${item.quantity}x ${item.itemName}\n")
        }

        binding.orderItemsText.text = itemsText.toString()
        binding.subtotalValue.text = PriceFormatter.format(subtotal)
        binding.deliveryValue.text = PriceFormatter.format(deliveryFee)
        binding.totalValue.text = PriceFormatter.format(total)
    }

    private fun setupClickListeners() {
        binding.addNewAddressButton.setOnClickListener {
            showAddAddressDialog()
        }

        binding.placeOrderButton.setOnClickListener {
            placeOrder()
        }
    }

    private fun showAddAddressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_address, null)
        val labelInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.labelInput)
        val addressInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.addressInput)
        val cityInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.cityInput)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Add New Address")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val label = labelInput.text.toString()
                val address = addressInput.text.toString()
                val city = cityInput.text.toString()

                if (label.isNotEmpty() && address.isNotEmpty() && city.isNotEmpty()) {
                    saveNewAddress(label, address, city)
                } else {
                    Snackbar.make(binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveNewAddress(label: String, address: String, city: String) {
        lifecycleScope.launch {
            val user = firebaseManager.getCurrentUser()
            user?.let {
                val newAddress = Address(
                    userId = it.id,
                    label = label,
                    addressLine1 = address,
                    city = city,
                    isDefault = savedAddresses.isEmpty()
                )
                firebaseManager.addAddress(newAddress)
                loadAddresses()
                Snackbar.make(binding.root, "Address added", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun placeOrder() {
        val fullAddress = binding.fullAddress.text.toString()
        val phoneNumber = binding.phoneNumber.text.toString()

        if (fullAddress.isEmpty()) {
            Snackbar.make(binding.root, "Please enter delivery address", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (phoneNumber.isEmpty()) {
            Snackbar.make(binding.root, "Please enter contact number", Snackbar.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                val items = cartManager.getCartItems()

                if (user != null && items.isNotEmpty()) {
                    // Group items by merchant
                    val groupedItems = items.groupBy { it.merchantId }

                    for ((merchantId, merchantItems) in groupedItems) {
                        val orderItems = merchantItems.map { item ->
                            OrderItem(
                                foodName = item.itemName,
                                quantity = item.quantity,
                                price = item.itemPrice
                            )
                        }

                        val subtotal = merchantItems.sumOf { it.lineTotal }
                        val deliveryFee = 49.0
                        val orderTotal = subtotal + deliveryFee

                        val order = Order(
                            orderId = "ORD-${UUID.randomUUID().toString().take(8).uppercase()}",
                            customerId = user.id,
                            merchantId = merchantId,
                            merchantName = merchantItems.first().merchantName,
                            orderTotal = orderTotal,
                            orderStatus = "pending",
                            deliveryAddress = fullAddress,
                            paymentMethod = selectedPaymentMethod,
                            orderDate = Date(),
                            items = orderItems
                        )

                        firebaseManager.createOrder(order)
                    }

                    cartManager.clearCart()

                    Snackbar.make(binding.root, "Order placed successfully!", Snackbar.LENGTH_LONG).show()

                    val intent = Intent(this@CheckoutActivity, CustomerDashboardActivity::class.java)
                    intent.putExtra("order_success", true)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error placing order: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.placeOrderButton.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}