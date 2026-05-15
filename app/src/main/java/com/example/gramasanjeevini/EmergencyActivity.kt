package com.example.gramasanjeevini

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EmergencyActivity : AppCompatActivity() {

    private lateinit var emergencyRecyclerView: RecyclerView
    private lateinit var emergencyLoader: ProgressBar
    private lateinit var adapter: MedicineAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        emergencyRecyclerView = findViewById(R.id.emergencyRecyclerView)
        emergencyLoader = findViewById(R.id.emergencyLoader)
        
        adapter = MedicineAdapter(emptyList())
        emergencyRecyclerView.layoutManager = LinearLayoutManager(this)
        emergencyRecyclerView.adapter = adapter

        fetchEmergencyMedicines()
    }

    private fun fetchEmergencyMedicines() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        emergencyLoader.visibility = View.VISIBLE
        adapter.updateData(emptyList())

        // ISSUE 1: Using High Accuracy Fresh Location
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                queryEmergencyStock(location)
            }.addOnFailureListener {
                queryEmergencyStock(null)
            }
    }

    private fun queryEmergencyStock(userLocation: Location?) {
        val today = Calendar.getInstance().time
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        db.collection("stock")
            .whereEqualTo("isLifeSaving", true)
            .get()
            .addOnSuccessListener { documents ->
                emergencyLoader.visibility = View.GONE
                val results = mutableListOf<MedicineResult>()
                for (doc in documents) {
                    val qty = doc.getLong("quantity") ?: 0
                    if (qty <= 0) continue

                    val expiryDateStr = doc.getString("expiryDate") ?: ""
                    if (isExpired(expiryDateStr, today, sdf)) continue

                    val sLat = doc.getDouble("lat") ?: 0.0
                    val sLng = doc.getDouble("lng") ?: 0.0
                    
                    // ISSUE 1: Accurate Distance Calculation
                    val distance = if (userLocation != null && sLat != 0.0) {
                        val shopLoc = Location("").apply {
                            latitude = sLat
                            longitude = sLng
                        }
                        userLocation.distanceTo(shopLoc) / 1000.0 // KM
                    } else {
                        -1.0
                    }

                    results.add(MedicineResult(
                        doc.getString("medicineName") ?: "",
                        doc.getString("shopName") ?: "Pharmacy",
                        doc.getString("village") ?: "",
                        qty,
                        true,
                        distance,
                        sLat,
                        sLng,
                        doc.getLong("discount") ?: 0L
                    ))
                }
                
                // Sorting: Nearest first
                adapter.updateData(results.sortedBy { if (it.distance == -1.0) Double.MAX_VALUE else it.distance })
                
                if (results.isEmpty()) {
                    Toast.makeText(this, "No emergency drugs found nearby.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                emergencyLoader.visibility = View.GONE
                Log.e("Emergency", "Error fetching data", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
            fetchEmergencyMedicines()
        }
    }
}
