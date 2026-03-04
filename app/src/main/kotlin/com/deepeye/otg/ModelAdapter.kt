package com.deepeye.otg

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class DeviceModel(
    val name: String,
    val chipset: String,
    val brand: String = "Xiaomi"
)

class ModelAdapter(
    private var models: List<DeviceModel> = emptyList(),
    private val onItemClick: (DeviceModel) -> Unit = {}
) : RecyclerView.Adapter<ModelAdapter.ModelViewHolder>() {

    private var selectedPosition = -1

    inner class ModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.modelName)
        val chipText: TextView = itemView.findViewById(R.id.modelChip)
        
        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val prevSelected = selectedPosition
                    selectedPosition = adapterPosition
                    notifyItemChanged(prevSelected)
                    notifyItemChanged(selectedPosition)
                    onItemClick(models[adapterPosition])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model, parent, false)
        return ModelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        val model = models[position]
        holder.nameText.text = model.name
        holder.chipText.text = model.chipset
        
        // Highlight selected
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(0x3000F2FF.toInt())
        } else {
            holder.itemView.setBackgroundColor(0x00000000)
        }
    }

    override fun getItemCount(): Int = models.size

    fun updateModels(newModels: List<DeviceModel>) {
        models = newModels
        selectedPosition = -1
        notifyDataSetChanged()
    }
    
    fun filter(query: String): List<DeviceModel> {
        return if (query.isEmpty()) {
            models
        } else {
            models.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.chipset.contains(query, ignoreCase = true) 
            }
        }
    }
}
