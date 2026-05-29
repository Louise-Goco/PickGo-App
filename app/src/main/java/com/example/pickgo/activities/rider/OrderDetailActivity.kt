package com.example.pickgo.activities.rider

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.databinding.ActivityOrderDetailsBinding
import com.example.pickgo.models.Order
import com.example.pickgo.models.rider.DeliveryOrder
import com.example.pickgo.models.rider.OrderItem
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.PriceFormatter
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class OrderDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderDetailsBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private var orderId: String = ""
    private var riderId: String = ""
    private var currentOrder: DeliveryOrder? = null
    private var proofImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            proofImageUri = result.data?.data
            completeDelivery()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        sessionManager = SessionManager(this)

        orderId = intent.getStringExtra("order_id") ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Order Details"

        loadOrderDetails()
    }

    private fun loadOrderDetails() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                user?.let {
                    val rider = firebaseManager.getRiderByEmail(it.email)
                    riderId = rider?.id ?: ""
                    currentOrder = firebaseManager.getOrderById(orderId)?.let { order ->
                        // Convert Order to DeliveryOrder
                        DeliveryOrder(
                            id = order.id,
                            orderId = order.orderId,
                            customerId = order.customerId,
                            customerName = "",
                            customerPhone = "",
                            merchantName = order.merchantName,
                            merchantId = order.merchantId,
                            merchantAddress = "",
                            merchantPhone = "",
                            deliveryAddress = order.deliveryAddress,
                            orderTotal = order.orderTotal,
                            riderEarnings = order.riderEarnings,
                            orderStatus = order.orderStatus,
                            paymentMethod = order.paymentMethod,
                            orderDate = order.orderDate.toString(),
                            items = emptyList()
                        )
                    }
                    currentOrder?.let { order ->
                        renderOrderDetails(order)
                    } ?: run {
                        Snackbar.make(binding.root, "Order not found", Snackbar.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun renderOrderDetails(order: DeliveryOrder) {
        binding.orderContent.removeAllViews()

        // Order Info Card
        val orderCard = createOrderInfoCard(order)
        binding.orderContent.addView(orderCard)

        // Pickup Details Card
        val pickupCard = createPickupDetailsCard(order)
        binding.orderContent.addView(pickupCard)

        // Delivery Details Card
        val deliveryCard = createDeliveryDetailsCard(order)
        binding.orderContent.addView(deliveryCard)

        // Items Card
        val itemsCard = createItemsCard(order)
        binding.orderContent.addView(itemsCard)

        // Action Buttons Card
        val actionCard = createActionButtonsCard(order)
        binding.orderContent.addView(actionCard)
    }

    private fun createOrderInfoCard(order: DeliveryOrder): View {
        val card = createCard()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }

        val orderIdText = TextView(this).apply {
            text = "Order #${order.orderId}"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
        }
        headerRow.addView(orderIdText)

        val statusText = TextView(this).apply {
            text = order.orderStatus.replace("_", " ").uppercase()
            textSize = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(10, 4, 10, 4)
            setBackgroundColor(getColor(R.color.primary_green_light))
            setTextColor(getColor(R.color.primary_green))
        }
        headerRow.addView(statusText)
        content.addView(headerRow)

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { topMargin = 16; bottomMargin = 16 }
            setBackgroundColor(getColor(R.color.input_border))
        }
        content.addView(divider)

        val totalRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }

        val totalLabel = TextView(this).apply {
            text = "Order Total"
            textSize = 14f
            setTextColor(getColor(R.color.text_secondary))
        }
        totalRow.addView(totalLabel)

        val totalValue = TextView(this).apply {
            text = PriceFormatter.format(order.orderTotal)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.primary_green))
        }
        totalRow.addView(totalValue)
        content.addView(totalRow)

        val earningsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8 }
        }

        val earningsLabel = TextView(this).apply {
            text = "Your Earnings"
            textSize = 14f
            setTextColor(getColor(R.color.text_secondary))
        }
        earningsRow.addView(earningsLabel)

        val earningsValue = TextView(this).apply {
            text = PriceFormatter.format(order.riderEarnings)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.primary_green))
        }
        earningsRow.addView(earningsValue)
        content.addView(earningsRow)

        card.addView(content)
        return card
    }

    private fun createPickupDetailsCard(order: DeliveryOrder): View {
        val card = createCard()
        val title = TextView(this).apply {
            text = "Pickup Details"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }
        card.addView(title)

        addDetailRow(card, "Merchant", order.merchantName)
        addDetailRow(card, "Address", order.merchantAddress)
        addDetailRow(card, "Contact", order.merchantPhone)

        return card
    }

    private fun createDeliveryDetailsCard(order: DeliveryOrder): View {
        val card = createCard()
        val title = TextView(this).apply {
            text = "Delivery Details"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }
        card.addView(title)

        addDetailRow(card, "Customer", order.customerName)
        addDetailRow(card, "Address", order.deliveryAddress)
        addDetailRow(card, "Phone", order.customerPhone)
        addDetailRow(card, "Payment Method", order.paymentMethod)

        return card
    }

    private fun createItemsCard(order: DeliveryOrder): View {
        val card = createCard()
        val title = TextView(this).apply {
            text = "Order Items"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }
        card.addView(title)

        order.items.forEach { item ->
            val itemRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }

            val itemName = TextView(this).apply {
                text = "${item.quantity}x ${item.foodName}"
                textSize = 14f
                setTextColor(getColor(R.color.text_primary))
            }
            itemRow.addView(itemName)

            val itemPrice = TextView(this).apply {
                text = PriceFormatter.format(item.subtotal)
                textSize = 14f
                setTextColor(getColor(R.color.text_primary))
            }
            itemRow.addView(itemPrice)

            card.addView(itemRow)
        }

        return card
    }

    private fun createActionButtonsCard(order: DeliveryOrder): View {
        val card = createCard()
        val status = order.orderStatus

        when (status) {
            "ready_for_pickup" -> {
                val pickupBtn = Button(this).apply {
                    text = "Confirm Pickup"
                    backgroundTintList = ContextCompat.getColorStateList(this@OrderDetailsActivity, R.color.primary_green)
                    setOnClickListener {
                        confirmPickup()
                    }
                }
                card.addView(pickupBtn)
            }
            "on_the_way" -> {
                val deliverBtn = Button(this).apply {
                    text = "Complete Delivery"
                    backgroundTintList = ContextCompat.getColorStateList(this@OrderDetailsActivity, R.color.primary_green)
                    setOnClickListener {
                        showProofDialog()
                    }
                }
                card.addView(deliverBtn)

                val navigateBtn = Button(this).apply {
                    text = "Navigate"
                    backgroundTintList = ContextCompat.getColorStateList(this@OrderDetailsActivity, R.color.primary_blue)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 12 }
                    setOnClickListener {
                        startNavigation()
                    }
                }
                card.addView(navigateBtn)
            }
            "pending", "preparing" -> {
                val waitingText = TextView(this).apply {
                    text = "Waiting for store to prepare order..."
                    textSize = 14f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setTextColor(getColor(R.color.text_secondary))
                }
                card.addView(waitingText)
            }
        }

        if (status in listOf("ready_for_pickup", "on_the_way")) {
            val cancelBtn = Button(this).apply {
                text = "Cancel Delivery"
                backgroundTintList = ContextCompat.getColorStateList(this@OrderDetailsActivity, R.color.error_text)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 12 }
                setOnClickListener {
                    showCancelDialog()
                }
            }
            card.addView(cancelBtn)
        }

        return card
    }

    private fun createCard(): LinearLayout {
        return com.google.android.material.card.MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
            cardElevation = 2f
            radius = 16f
            setCardBackgroundColor(getColor(R.color.glass_bg))

            val content = LinearLayout(this@OrderDetailsActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
            }
            addView(content)
        }.findViewById(android.R.id.content) as? LinearLayout ?: LinearLayout(this)
    }

    private fun addDetailRow(parent: LinearLayout, label: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }

        val labelText = TextView(this).apply {
            text = label
            textSize = 12f
            setTextColor(getColor(R.color.text_secondary))
        }
        row.addView(labelText)

        val valueText = TextView(this).apply {
            text = value
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            gravity = android.view.Gravity.END
        }
        row.addView(valueText)

        parent.addView(row)
    }

    private fun confirmPickup() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.updateOrderStatus(orderId, "on_the_way")
                Snackbar.make(binding.root, "Pickup confirmed! Order is on the way.", Snackbar.LENGTH_SHORT).show()
                loadOrderDetails()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showProofDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delivery Proof")
            .setMessage("Take a photo of the delivered items as proof")
            .setPositiveButton("Take Photo") { _, _ ->
                requestCameraPermission()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
            } else {
                openCamera()
            }
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Snackbar.make(binding.root, "Camera permission required", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        pickImageLauncher.launch(intent)
    }

    private fun completeDelivery() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                proofImageUri?.let { uri ->
                    firebaseManager.completeDelivery(orderId, uri)
                    Snackbar.make(binding.root, "Delivery completed! Thank you!", Snackbar.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun startNavigation() {
        currentOrder?.let { order ->
            val intent = Intent(this, NavigationActivity::class.java).apply {
                putExtra("order_id", order.id)
                putExtra("pickup_address", order.merchantAddress)
                putExtra("pickup_name", order.merchantName)
                putExtra("delivery_address", order.deliveryAddress)
            }
            startActivity(intent)
        }
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Delivery")
            .setMessage("Are you sure you want to cancel this delivery? It will be reassigned to another rider.")
            .setPositiveButton("Cancel Delivery") { _, _ ->
                cancelDelivery()
            }
            .setNegativeButton("Continue", null)
            .show()
    }

    private fun cancelDelivery() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.cancelDelivery(orderId)
                Snackbar.make(binding.root, "Delivery cancelled", Snackbar.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
