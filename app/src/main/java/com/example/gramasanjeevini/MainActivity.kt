package com.example.gramasanjeevini

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var loginButton: Button
    private lateinit var emergencyButton: Button
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var searchLoader: ProgressBar
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var adapter: MedicineAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore

    private var selectedRadius: Double = Double.MAX_VALUE 
    private var allSearchResults = mutableListOf<MedicineResult>()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        loginButton = findViewById(R.id.loginButton)
        emergencyButton = findViewById(R.id.emergencyButton)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        searchLoader = findViewById(R.id.searchLoader)
        filterChipGroup = findViewById(R.id.filterChipGroup)

        setupRecyclerView()
        setupFilters()

        searchButton.setOnClickListener { searchMedicine() }
        loginButton.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
        emergencyButton.setOnClickListener { startActivity(Intent(this, EmergencyActivity::class.java)) }

        checkLocationPermission()
    }

    private fun setupRecyclerView() {
        adapter = MedicineAdapter(emptyList())
        resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        resultsRecyclerView.adapter = adapter
    }

    private fun setupFilters() {
        filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedRadius = when {
                checkedIds.contains(R.id.chip10km) -> 10.0
                checkedIds.contains(R.id.chip20km) -> 20.0
                else -> Double.MAX_VALUE
            }
            applyFilters()
        }
    }

    private fun applyFilters() {
        val filteredList = if (selectedRadius == Double.MAX_VALUE) {
            allSearchResults
        } else {
            allSearchResults.filter { it.distance != -1.0 && it.distance <= selectedRadius }
        }
        // Sorting: Nearest stores first
        adapter.updateData(filteredList.sortedBy { if (it.distance == -1.0) Double.MAX_VALUE else it.distance })
        
        if (filteredList.isEmpty() && allSearchResults.isNotEmpty()) {
            Toast.makeText(this, "No stores found within ${selectedRadius.toInt()}km", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun searchMedicine() {
        val queryText = searchEditText.text.toString().trim().lowercase()
        if (queryText.isEmpty()) {
            Toast.makeText(this, "Enter medicine name", Toast.LENGTH_SHORT).show()
            return
        }

        searchLoader.visibility = View.VISIBLE
        adapter.updateData(emptyList())
        allSearchResults.clear()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    performSearch(queryText, location)
                }.addOnFailureListener {
                    performSearch(queryText, null)
                }
        } else {
            performSearch(queryText, null)
        }
    }

    private fun performSearch(queryText: String, userLocation: Location?) {
        db.collection("stock")
            .whereEqualTo("searchName", queryText)
            .get()
            .addOnSuccessListener { documents ->
                searchLoader.visibility = View.GONE
                val today = Calendar.getInstance().time
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                for (doc in documents) {
                    val expiryDateStr = doc.getString("expiryDate") ?: ""
                    if (isExpired(expiryDateStr, today, sdf)) continue

                    val sLat = doc.getDouble("lat") ?: 0.0
                    val sLng = doc.getDouble("lng") ?: 0.0
                    
                    val distance = if (userLocation != null && sLat != 0.0) {
                        val shopLoc = Location("").apply {
                            latitude = sLat
                            longitude = sLng
                        }
                        userLocation.distanceTo(shopLoc) / 1000.0
                    } else {
                        -1.0 // Location unknown
                    }
                    
                    allSearchResults.add(MedicineResult(
                        doc.getString("medicineName") ?: "",
                        doc.getString("shopName") ?: "Pharmacy",
                        doc.getString("village") ?: "",
                        doc.getLong("quantity") ?: 0,
                        doc.getBoolean("isLifeSaving") ?: false,
                        distance,
                        sLat,
                        sLng,
                        doc.getLong("discount") ?: 0L
                    ))
                }
                
                applyFilters()
                
                if (allSearchResults.isEmpty()) {
                    Toast.makeText(this, "Medicine not found in any store", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                searchLoader.visibility = View.GONE
                Toast.makeText(this, "Search Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isExpired(dateStr: String, today: Date, sdf: SimpleDateFormat): Boolean {
        return try {
            val expiryDate = sdf.parse(dateStr)
            expiryDate != null && expiryDate.before(today)
        } catch (e: Exception) {
            false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
        }
    }
}
