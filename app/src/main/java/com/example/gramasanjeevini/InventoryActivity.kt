package com.example.gramasanjeevini

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class InventoryActivity : AppCompatActivity() {

    private lateinit var medicineNameEditText: TextInputEditText
    private lateinit var stockEditText: TextInputEditText
    private lateinit var expiryEditText: TextInputEditText
    private lateinit var discountEditText: TextInputEditText
    private lateinit var lifeSavingCheckBox: CheckBox
    private lateinit var addButton: Button
    
    private lateinit var nearExpiryRecyclerView: RecyclerView
    private lateinit var inventoryRecyclerView: RecyclerView
    
    private lateinit var nearExpiryAdapter: InventoryAdapter
    private lateinit var inventoryAdapter: InventoryAdapter
    
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var snapshotListener: ListenerRegistration? = null

    private var shopName: String = ""
    private var village: String = ""
    private var shopLat: Double = 0.0
    private var shopLng: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        medicineNameEditText = findViewById(R.id.medicineNameEditText)
        stockEditText = findViewById(R.id.stockEditText)
        expiryEditText = findViewById(R.id.expiryEditText)
        discountEditText = findViewById(R.id.discountEditText)
        lifeSavingCheckBox = findViewById(R.id.lifeSavingCheckBox)
        addButton = findViewById(R.id.addButton)
        
        nearExpiryRecyclerView = findViewById(R.id.nearExpiryRecyclerView)
        inventoryRecyclerView = findViewById(R.id.inventoryRecyclerView)

        expiryEditText.setOnClickListener { showDatePicker() }

        setupRecyclerViews()
        
        addButton.isEnabled = false
        fetchPharmacistProfile()
        
        addButton.setOnClickListener { validateAndAddMedicine() }
        startListeningForChanges()
    }

    private fun showDatePicker() {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Expiry Date")
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection: Long ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            expiryEditText.setText(sdf.format(calendar.time))
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun setupRecyclerViews() {
        val clickListener = { item: InventoryItem ->
            medicineNameEditText.setText(item.name)
            stockEditText.setText(item.stock.toString())
            expiryEditText.setText(item.expiry)
            discountEditText.setText(if(item.discount > 0) item.discount.toString() else "")
            lifeSavingCheckBox.isChecked = item.isLifeSaving
            
            medicineNameEditText.isEnabled = false 
            addButton.text = "CONFIRM UPDATE"
        }

        nearExpiryAdapter = InventoryAdapter(emptyList(), clickListener)
        nearExpiryRecyclerView.layoutManager = LinearLayoutManager(this)
        nearExpiryRecyclerView.adapter = nearExpiryAdapter

        inventoryAdapter = InventoryAdapter(emptyList(), clickListener)
        inventoryRecyclerView.layoutManager = LinearLayoutManager(this)
        inventoryRecyclerView.adapter = inventoryAdapter
    }

    private fun validateAndAddMedicine() {
        val name = medicineNameEditText.text.toString().trim()
        val qtyStr = stockEditText.text.toString().trim()
        val expiry = expiryEditText.text.toString().trim()
        val discountStr = discountEditText.text.toString().trim()
        val isLifeSaving = lifeSavingCheckBox.isChecked
        val userId = auth.currentUser?.uid ?: return

        if (name.isEmpty() || qtyStr.isEmpty() || expiry.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = qtyStr.toLongOrNull() ?: -1
        val discount = if (discountStr.isEmpty()) 0L else discountStr.toLongOrNull() ?: 0L

        updateMedicineData(name, quantity, expiry, discount, isLifeSaving, userId)
    }

    private fun updateMedicineData(name: String, quantity: Long, expiry: String, discount: Long, isLifeSaving: Boolean, userId: String) {
        val medId = name.lowercase().replace(" ", "_").replace("[^a-z0-9_]".toRegex(), "")
        
        val medicineData = hashMapOf(
            "name" to name,
            "quantity" to quantity,
            "expiryDate" to expiry,
            "discount" to discount,
            "isLifeSaving" to isLifeSaving
        )

        db.collection("shops").document(userId).collection("medicines").document(medId).set(medicineData)

        val publicStockData = hashMapOf(
            "medicineName" to name,
            "searchName" to name.lowercase(),
            "quantity" to quantity,
            "expiryDate" to expiry,
            "discount" to discount,
            "isLifeSaving" to isLifeSaving,
            "pharmacistId" to userId,
            "shopName" to shopName,
            "village" to village,
            "lat" to shopLat,
            "lng" to shopLng,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("stock").document("${userId}_$medId").set(publicStockData)
            .addOnSuccessListener {
                Toast.makeText(this, "Stock Updated!", Toast.LENGTH_SHORT).show()
                resetForm()
            }
    }

    private fun startListeningForChanges() {
        val userId = auth.currentUser?.uid ?: return
        
        snapshotListener = db.collection("shops").document(userId)
            .collection("medicines")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                val allItems = mutableListOf<InventoryItem>()
                snapshots?.forEach { doc ->
                    allItems.add(InventoryItem(
                        doc.id,
                        doc.getString("name") ?: "",
                        doc.getLong("quantity") ?: 0,
                        doc.getString("expiryDate") ?: "",
                        doc.getBoolean("isLifeSaving") ?: false,
                        doc.getLong("discount") ?: 0L
                    ))
                }

                val nearExpiry = allItems.filter { 
                    val days = getDaysUntilExpiry(it.expiry)
                    days in 0..30 
                }
                
                nearExpiryAdapter.updateData(nearExpiry)
                inventoryAdapter.updateData(allItems)
            }
    }

    private fun getDaysUntilExpiry(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expiryDate = sdf.parse(dateStr)
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            
            if (expiryDate != null) {
                val diff = expiryDate.time - today.timeInMillis
                diff / (1000 * 60 * 60 * 24)
            } else 999L
        } catch (e: Exception) {
            999L
        }
    }

    private fun resetForm() {
        medicineNameEditText.text?.clear()
        stockEditText.text?.clear()
        expiryEditText.text?.clear()
        discountEditText.text?.clear()
        lifeSavingCheckBox.isChecked = false
        medicineNameEditText.isEnabled = true
        addButton.text = "UPDATE STOCK"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.inventory_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_logout) {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fetchPharmacistProfile() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("pharmacies").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    shopName = document.getString("shopName") ?: "Village Pharmacy"
                    village = document.getString("village") ?: "Rural Area"
                    shopLat = document.getDouble("lat") ?: 0.0
                    shopLng = document.getDouble("lng") ?: 0.0
                    addButton.isEnabled = true
                } else {
                    Toast.makeText(this, "Profile not found. Please re-register.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        snapshotListener?.remove()
    }
}
