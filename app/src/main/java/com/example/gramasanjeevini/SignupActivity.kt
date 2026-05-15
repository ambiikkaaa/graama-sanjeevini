package com.example.gramasanjeevini

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var storeNameEditText: TextInputEditText
    private lateinit var locationEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var signupButton: Button
    private lateinit var locationStatusText: TextView
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private var storeLat: Double = 0.0
    private var storeLng: Double = 0.0

    private val selectLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            storeLat = data?.getDoubleExtra("lat", 0.0) ?: 0.0
            storeLng = data?.getDoubleExtra("lng", 0.0) ?: 0.0
            locationStatusText.text = String.format("Location: Selected (%.4f, %.4f)", storeLat, storeLng)
            locationStatusText.setTextColor(ContextCompat.getColor(this, R.color.primary))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        nameEditText = findViewById(R.id.nameEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        emailEditText = findViewById(R.id.emailEditText)
        storeNameEditText = findViewById(R.id.storeNameEditText)
        locationEditText = findViewById(R.id.locationEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        signupButton = findViewById(R.id.signupButton)
        locationStatusText = findViewById(R.id.locationStatusText)
        
        findViewById<MaterialButton>(R.id.pickLocationButton).setOnClickListener {
            val intent = Intent(this, SelectLocationActivity::class.java)
            selectLocationLauncher.launch(intent)
        }
        
        findViewById<MaterialButton>(R.id.backToLoginButton).setOnClickListener { finish() }
        signupButton.setOnClickListener { register() }
    }

    private fun register() {
        val email = emailEditText.text.toString().trim()
        val pass = passwordEditText.text.toString().trim()
        val sName = storeNameEditText.text.toString().trim()
        val name = nameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val location = locationEditText.text.toString().trim()

        if (email.isEmpty() || pass.isEmpty() || sName.isEmpty() || name.isEmpty() || phone.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (storeLat == 0.0 || storeLng == 0.0) {
            Toast.makeText(this, "Please pick your store location on the map", Toast.LENGTH_LONG).show()
            return
        }

        signupButton.isEnabled = false
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid ?: ""
                val profile = hashMapOf(
                    "pharmacistId" to uid,
                    "shopName" to sName,
                    "village" to location,
                    "lat" to storeLat,
                    "lng" to storeLng,
                    "name" to name,
                    "phone" to phone,
                    "email" to email
                )

                db.collection("pharmacies").document(uid).set(profile).addOnSuccessListener {
                    Toast.makeText(this, "Pharmacist Registered Successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, InventoryActivity::class.java))
                    finish()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Firestore Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    signupButton.isEnabled = true
                }
            } else {
                Toast.makeText(this, "Auth Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                signupButton.isEnabled = true
            }
        }
    }
}