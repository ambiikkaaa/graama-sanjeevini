package com.example.gramasanjeevini

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class InventoryItem(
    val id: String,
    val name: String,
    val stock: Long,
    val expiry: String,
    val isLifeSaving: Boolean,
    val discount: Long = 0L
)

class InventoryAdapter(
    private var items: List<InventoryItem>,
    private val onItemClick: (InventoryItem) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.invMedicineName)
        val expiryText: TextView = view.findViewById(R.id.invExpiryDate)
        val stockText: TextView = view.findViewById(R.id.invStockCount)
        val lifeSavingText: TextView = view.findViewById(R.id.invLifeSavingStatus)
        val discountBadge: TextView = view.findViewById(R.id.invDiscountBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        
        holder.nameText.text = item.name
        holder.stockText.text = "Stock: ${item.stock}"
        
        val expiryDateStr = item.expiry
        val daysUntilExpiry = getDaysUntilExpiry(expiryDateStr)
        
        // Feature 1: Visual indicators for Expiry
        when {
            daysUntilExpiry < 0 -> {
                holder.expiryText.text = "EXPIRED ($expiryDateStr)"
                holder.expiryText.setTextColor(ContextCompat.getColor(context, R.color.error))
            }
            daysUntilExpiry <= 30 -> {
                holder.expiryText.text = "⚠ Near Expiry: $expiryDateStr\nConsider selling at a discount."
                holder.expiryText.setTextColor(ContextCompat.getColor(context, R.color.warning))
            }
            else -> {
                holder.expiryText.text = "Expires: $expiryDateStr"
                holder.expiryText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }

        // Feature 1: Discount badge
        if (item.discount > 0) {
            holder.discountBadge.visibility = View.VISIBLE
            holder.discountBadge.text = "${item.discount}% OFF"
        } else {
            holder.discountBadge.visibility = View.GONE
        }

        holder.lifeSavingText.visibility = if (item.isLifeSaving) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            onItemClick(item)
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

    override fun getItemCount() = items.size

    fun updateData(newItems: List<InventoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}