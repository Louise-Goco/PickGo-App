package com.example.pickgo.activities.rider

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.example.pickgo.R
import com.example.pickgo.databinding.ActivityNavigationBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class NavigationActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityNavigationBinding
    private lateinit var googleMap: GoogleMap
    private var pickupAddress: String = ""
    private var deliveryAddress: String = ""
    private var pickupName: String = ""
    private var orderId: String = ""
    private var pickupCoords: LatLng? = null
    private var deliveryCoords: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        orderId = intent.getStringExtra("order_id") ?: ""
        pickupAddress = intent.getStringExtra("pickup_address") ?: ""
        pickupName = intent.getStringExtra("pickup_name") ?: "Store"
        deliveryAddress = intent.getStringExtra("delivery_address") ?: ""

        binding.pickupTitle.text = "Pickup: $pickupName"
        binding.pickupAddress.text = pickupAddress
        binding.deliveryAddress.text = deliveryAddress

        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.orderControlsButton.setOnClickListener {
            val intent = Intent(this, OrderDetailsActivity::class.java)
            intent.putExtra("order_id", orderId)
            startActivity(intent)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true

        geocodeAddresses()
    }

    private fun geocodeAddresses() {
        lifecycleScope.launch {
            pickupCoords = geocodeAddress(pickupAddress)
            deliveryCoords = geocodeAddress(deliveryAddress)

            if (pickupCoords != null && deliveryCoords != null) {
                addMarkers()
                drawRoute()
                fitBounds()
            } else if (pickupCoords != null) {
                addPickupMarker()
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickupCoords!!, 15f))
            } else if (deliveryCoords != null) {
                addDeliveryMarker()
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(deliveryCoords!!, 15f))
            }
        }
    }

    private suspend fun geocodeAddress(address: String): LatLng? = withContext(Dispatchers.IO) {
        return@withContext try {
            val encodedAddress = Uri.encode(address)
            val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedAddress&limit=1"
            val response = URL(url).readText()
            val jsonArray = org.json.JSONArray(response)
            if (jsonArray.length() > 0) {
                val jsonObject = jsonArray.getJSONObject(0)
                val lat = jsonObject.getString("lat").toDouble()
                val lon = jsonObject.getString("lon").toDouble()
                LatLng(lat, lon)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun addMarkers() {
        pickupCoords?.let {
            googleMap.addMarker(MarkerOptions()
                .position(it)
                .title("Pickup: $pickupName")
                .snippet(pickupAddress))
        }

        deliveryCoords?.let {
            googleMap.addMarker(MarkerOptions()
                .position(it)
                .title("Delivery")
                .snippet(deliveryAddress))
        }
    }

    private fun addPickupMarker() {
        pickupCoords?.let {
            googleMap.addMarker(MarkerOptions()
                .position(it)
                .title("Pickup: $pickupName")
                .snippet(pickupAddress))
        }
    }

    private fun addDeliveryMarker() {
        deliveryCoords?.let {
            googleMap.addMarker(MarkerOptions()
                .position(it)
                .title("Delivery")
                .snippet(deliveryAddress))
        }
    }

    private fun drawRoute() {
        if (pickupCoords != null && deliveryCoords != null) {
            // Get route from OSRM API
            lifecycleScope.launch {
                val routePoints = getRoute(pickupCoords!!, deliveryCoords!!)
                if (routePoints.isNotEmpty()) {
                    val polylineOptions = PolylineOptions()
                        .addAll(routePoints)
                        .color(getColor(R.color.primary_blue))
                        .width(8f)
                    googleMap.addPolyline(polylineOptions)
                }
            }
        }
    }

    private suspend fun getRoute(origin: LatLng, destination: LatLng): List<LatLng> = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = "https://router.project-osrm.org/route/v1/driving/${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}?overview=full&geometries=geojson"
            val response = URL(url).readText()
            val json = JSONObject(response)
            val routes = json.getJSONArray("routes")
            if (routes.length() > 0) {
                val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")
                val points = mutableListOf<LatLng>()
                for (i in 0 until coordinates.length()) {
                    val coord = coordinates.getJSONArray(i)
                    points.add(LatLng(coord.getDouble(1), coord.getDouble(0)))
                }
                points
            } else emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun fitBounds() {
        val pickup = pickupCoords
        val delivery = deliveryCoords
        if (pickup != null && delivery != null) {
            val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
            builder.include(pickup)
            builder.include(delivery)
            val bounds = builder.build()
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }
    }
}