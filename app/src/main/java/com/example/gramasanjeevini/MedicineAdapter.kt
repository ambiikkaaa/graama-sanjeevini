package com.example.gramasanjeevini

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

data class MedicineResult(
    val name: String,
    val shopName: String,
    val village: String,
    val stock: Long,
    val isLifeSaving: Boolean,
    val distance: Double,
    val lat: Double,
    val lng: Double,
    val discount: Long = 0L
)

class MedicineAdapter(private var results: List<MedicineResult>) :
    RecyclerView.Adapter<MedicineAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val medicineNameText: TextView = view.findViewById(R.id.medicineNameText)
        val pharmacyNameText: TextView = view.findViewById(R.id.pharmacyNameText)
        val villageNameText: TextView = view.findViewById(R.id.villageNameText)
        val stockStatusText: TextView = view.findViewById(R.id.stockStatusText)
        val distanceText: TextView = view.findViewById(R.id.distanceText)
        val lifeSavingBadge: TextView = view.findViewById(R.id.lifeSavingBadge)
        val discountText: TextView = view.findViewById(R.id.discountText)
        val navigateButton: Button = view.findViewById(R.id.navigateButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pharmacy_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = results[position]
        val context = holder.itemView.context

        holder.medicineNameText.text = item.name
        holder.pharmacyNameText.text = item.shopName
        holder.villageNameText.text = item.village
        
        // ISSUE 1: Fixed Distance Display with Validation
        when {
            item.lat == 0.0 || item.lng == 0.0 -> {
                holder.distanceText.text = "Location not set by shop"
            }
            item.distance < 0 -> {
                holder.distanceText.text = "Distance unavailable"
            }
            item.distance < 0.1 -> {
                holder.distanceText.text = "Nearby (< 100m)"
            }
            else -> {
                holder.distanceText.text = String.format("%.2f km away", item.distance)
            }
        }

        // Visibility and High Contrast
        holder.medicineNameText.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        holder.pharmacyNameText.setTextColor(ContextCompat.getColor(context, R.color.primary))
        
        if (item.stock > 0) {
            holder.stockStatusText.text = "In Stock (${item.stock})"
            holder.stockStatusText.setTextColor(ContextCompat.getColor(context, R.color.success))
        } else {
            holder.stockStatusText.text = "Out of Stock"
            holder.stockStatusText.setTextColor(ContextCompat.getColor(context, R.color.error))
        }

        if (item.discount > 0) {
            holder.discountText.visibility = View.VISIBLE
            holder.discountText.text = "${item.discount}% OFF - Near Expiry Special"
        } else {
            holder.discountText.visibility = View.GONE
        }

        holder.lifeSavingBadge.visibility = if (item.isLifeSaving) View.VISIBLE else View.GONE
        
        // ISSUE 2: Add Google Maps Route Navigation
        holder.navigateButton.setOnClickListener {
            if (item.lat != 0.0 && item.lng != 0.0) {
                // 'q' for query/search or navigation target
                val gmmIntentUri = Uri.parse("google.navigation:q=${item.lat},${item.lng}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                
                if (mapIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(mapIntent)
                } else {
                    // Direct browser link to Google Maps Directions
                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${item.lat},${item.lng}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                }
            } else {
                android.widget.Toast.makeText(context, "Pharmacy coordinates not available", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = results.size

    fun updateData(newResults: List<MedicineResult>) {
        results = newResults
        notifyDataSetChanged()
    }
}
