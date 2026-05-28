package com.example.pickgo.activities.customer

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.adapters.CartAdapter
import com.example.pickgo.databinding.ActivityCartBinding
import com.example.pickgo.utils.CartManager
import com.example.pickgo.utils.PriceFormatter
import kotlinx.coroutines.launch

class CartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCartBinding
    private lateinit var cartManager: CartManager
    private lateinit var cartAdapter: CartAdapter
    private var appliedPromoCode: String? = null
    private var promoDiscount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cartManager = CartManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Cart"

        setupRecyclerView()
        setupClickListeners()
        loadCart()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChange = { itemId, change ->
                updateQuantity(itemId, change)
            },
            onRemove = { itemId ->
                removeItem(itemId)
            }
        )

        binding.cartItemsRecycler.layoutManager = LinearLayoutManager(this)
        binding.cartItemsRecycler.adapter = cartAdapter
    }

    private fun setupClickListeners() {
        binding.applyPromoButton.setOnClickListener {
            val code = binding.promoInput.text.toString().trim()
            if (code.isNotEmpty()) {
                applyPromoCode(code)
            }
        }

        binding.removePromoButton.setOnClickListener {
            removePromoCode()
        }

        binding.checkoutButton.setOnClickListener {
            val items = cartManager.getCartItems()
            if (items.isNotEmpty()) {
                startActivity(Intent(this, CheckoutActivity::class.java))
            }
        }

        binding.browseMenuButton.setOnClickListener {
            startActivity(Intent(this, BrowseItemsActivity::class.java))
            finish()
        }
    }

    private fun loadCart() {
        val items = cartManager.getCartItems()

        if (items.isEmpty()) {
            binding.emptyCart.visibility = View.VISIBLE
            binding.summaryCard.visibility = View.GONE
            return
        }

        binding.emptyCart.visibility = View.GONE
        binding.summaryCard.visibility = View.VISIBLE

        cartAdapter.submitList(items)
        updateSummary()
    }

    private fun updateQuantity(itemId: String, change: Int) {
        val newItems = cartManager.updateQuantity(itemId, change)
        cartAdapter.submitList(newItems)

        if (newItems.isEmpty()) {
            loadCart()
        } else {
            updateSummary()
        }
    }

    private fun removeItem(itemId: String) {
        val newItems = cartManager.removeItem(itemId)
        cartAdapter.submitList(newItems)

        if (newItems.isEmpty()) {
            loadCart()
        } else {
            updateSummary()
        }

        Snackbar.make(binding.root, "Item removed from cart", Snackbar.LENGTH_SHORT).show()
    }

    private fun updateSummary() {
        val items = cartManager.getCartItems()
        val subtotal = items.sumOf { it.lineTotal }
        val deliveryFee = if (items.isNotEmpty()) 49.0 else 0.0
        val total = subtotal + deliveryFee - promoDiscount

        binding.subtotalText.text = PriceFormatter.format(subtotal)
        binding.deliveryFeeText.text = PriceFormatter.format(deliveryFee)
        binding.totalText.text = PriceFormatter.format(total)

        if (promoDiscount > 0) {
            binding.discountLayout.visibility = View.VISIBLE
            binding.discountText.text = "-${PriceFormatter.format(promoDiscount)}"
        } else {
            binding.discountLayout.visibility = View.GONE
        }

        // Update cart badge in toolbar
        invalidateOptionsMenu()
    }

    private fun applyPromoCode(code: String) {
        lifecycleScope.launch {
            // Simulate promo validation
            if (code.uppercase() == "SAVE20") {
                promoDiscount = cartManager.getSubtotal() * 0.2
                appliedPromoCode = code
                binding.appliedPromoCode.text = code.uppercase()
                binding.promoSection.visibility = View.GONE
                binding.appliedPromoLayout.visibility = View.VISIBLE
                updateSummary()
                Snackbar.make(binding.root, "Promo code applied! 20% discount", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, "Invalid promo code", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun removePromoCode() {
        promoDiscount = 0.0
        appliedPromoCode = null
        binding.promoSection.visibility = View.VISIBLE
        binding.appliedPromoLayout.visibility = View.GONE
        binding.promoInput.text?.clear()
        updateSummary()
        Snackbar.make(binding.root, "Promo code removed", Snackbar.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        loadCart()
    }
}