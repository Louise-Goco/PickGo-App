package com.example.pickgo.activities.customer

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.pickgo.R
import com.example.pickgo.adapters.ItemAdapter
import com.example.pickgo.databinding.ActivityViewStoreBinding
import com.example.pickgo.models.CartItem
import com.example.pickgo.models.Item
import com.example.pickgo.models.Merchant
import com.example.pickgo.utils.CartManager
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class ViewStoreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewStoreBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var cartManager: CartManager
    private lateinit var sessionManager: SessionManager
    private var merchantId: String = ""
    private var merchant: Merchant? = null
    private var groupedItems: Map<String, List<Item>> = emptyMap()
    private val categorySections = mutableMapOf<String, ItemAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        cartManager = CartManager(this)
        sessionManager = SessionManager(this)

        merchantId = intent.getStringExtra("merchant_id") ?: ""
        val merchantName = intent.getStringExtra("merchant_name") ?: "Store"

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = merchantName

        setupTabLayout()
        loadStoreData()
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: 0
                binding.viewPager.currentItem = position
                scrollToCategory(position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun scrollToCategory(position: Int) {
        val categoryName = groupedItems.keys.elementAtOrNull(position) ?: return
        val sectionView = categorySections[categoryName]?.let { adapter ->
            // Find the section header view
            binding.nestedScrollView.post {
                val sectionHeader = findViewById<View>(getSectionHeaderId(categoryName))
                sectionHeader?.let {
                    binding.nestedScrollView.smoothScrollTo(0, it.top)
                }
            }
        }
    }

    private fun getSectionHeaderId(categoryName: String): Int {
        return categoryName.hashCode()
    }

    private fun loadStoreData() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                merchant = firebaseManager.getMerchantById(merchantId)
                merchant?.let { m ->
                    displayStoreInfo(m)
                    loadMenuItems()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading store: ${e.message}", Snackbar.LENGTH_SHORT).show()
                finish()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun displayStoreInfo(merchant: Merchant) {
        binding.storeName.text = merchant.merchName
        binding.storeType.text = merchant.merchType
        binding.storeRating.text = String.format("%.1f ★", merchant.rating)
        binding.storeHours.text = "${merchant.merchOpeningTime} - ${merchant.merchClosingTime}"

        if (!merchant.merchDescription.isNullOrEmpty()) {
            binding.storeDescription.text = merchant.merchDescription
            binding.storeDescription.visibility = View.VISIBLE
        } else {
            binding.storeDescription.visibility = View.GONE
        }

        Glide.with(this)
            .load(merchant.merchBanner)
            .placeholder(R.drawable.placeholder_store)
            .error(R.drawable.placeholder_store)
            .into(binding.storeBanner)

        Glide.with(this)
            .load(merchant.merchLogo)
            .placeholder(R.drawable.placeholder_store)
            .error(R.drawable.placeholder_store)
            .circleCrop()
            .into(binding.storeLogo)
    }

    private fun loadMenuItems() {
        lifecycleScope.launch {
            val items = firebaseManager.getItemsByMerchant(merchantId)
            groupedItems = items.groupBy { it.itemCategory }

            buildMenuUI()
        }
    }

    private fun buildMenuUI() {
        val categoriesContainer = binding.categoriesContainer
        categoriesContainer.removeAllViews()
        categorySections.clear()

        if (groupedItems.isEmpty()) {
            binding.emptyMenu.visibility = View.VISIBLE
            return
        }

        binding.emptyMenu.visibility = View.GONE

        // Add tabs
        groupedItems.keys.forEach { categoryName ->
            val tab = binding.tabLayout.newTab()
            tab.text = categoryName
            binding.tabLayout.addTab(tab)
        }

        // Add sections
        groupedItems.forEach { (categoryName, items) ->
            val sectionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 24, 0, 24)
            }

            val titleView = android.widget.TextView(this).apply {
                text = categoryName
                textSize = 20f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(getColor(R.color.text_primary))
                setPadding(16, 0, 16, 16)
                id = getSectionHeaderId(categoryName)
            }
            sectionLayout.addView(titleView)

            val recyclerView = androidx.recyclerview.widget.RecyclerView(this).apply {
                layoutManager = GridLayoutManager(this@ViewStoreActivity, 2)
                adapter = ItemAdapter { item ->
                    addToCart(item)
                }.also { adapter ->
                    adapter.submitList(items)
                    categorySections[categoryName] = adapter
                }
            }
            sectionLayout.addView(recyclerView)

            categoriesContainer.addView(sectionLayout)
        }

        // Setup ViewPager for tabs
        setupViewPager()
    }

    private fun setupViewPager() {
        val adapter = MenuPagerAdapter(this, groupedItems.keys.toList())
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = groupedItems.keys.elementAt(position)
        }.attach()
    }

    private fun addToCart(item: Item) {
        if (sessionManager.getSession() == null) {
            Snackbar.make(binding.root, "Please login to add items to cart", Snackbar.LENGTH_LONG)
                .setAction("Login") {}.show()
            return
        }

        val cartItem = CartItem(
            itemId = item.itemId,
            itemName = item.itemName,
            itemPrice = item.itemPrice,
            itemImage = item.itemImage,
            merchantName = item.merchantName,
            merchantId = item.merchantId,
            quantity = 1
        )
        cartManager.addItem(cartItem)
        Snackbar.make(binding.root, "${item.itemName} added to cart", Snackbar.LENGTH_SHORT).show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    inner class MenuPagerAdapter(
        activity: AppCompatActivity,
        private val categories: List<String>
    ) : androidx.viewpager2.adapter.FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = categories.size

        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            return MenuCategoryFragment.newInstance(
                groupedItems[categories[position]] ?: emptyList()
            )
        }
    }
}

class MenuCategoryFragment : androidx.fragment.app.Fragment() {
    private lateinit var adapter: ItemAdapter
    private var items: List<Item> = emptyList()

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): android.view.View? {
        val rootView = inflater.inflate(R.layout.fragment_menu_category, container, false)
        val recyclerView = rootView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 2)
        adapter = ItemAdapter { item ->
            val cartManager = CartManager(requireContext())
            val cartItem = CartItem(
                itemId = item.itemId,
                itemName = item.itemName,
                itemPrice = item.itemPrice,
                itemImage = item.itemImage,
                merchantName = item.merchantName,
                merchantId = item.merchantId,
                quantity = 1
            )
            cartManager.addItem(cartItem)
            android.widget.Toast.makeText(context, "${item.itemName} added to cart", android.widget.Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter

        adapter.submitList(items)
        return rootView
    }

    companion object {
        fun newInstance(items: List<Item>): MenuCategoryFragment {
            val fragment = MenuCategoryFragment()
            fragment.items = items
            return fragment
        }
    }
}