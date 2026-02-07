package com.deepeye.otg

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class PartitionInfo(
    val name: String,
    val size: String,
    val type: String = "Unknown"
)

class PartitionAdapter(
    private var partitions: List<PartitionInfo> = emptyList(),
    private val onItemClick: (PartitionInfo) -> Unit = {}
) : RecyclerView.Adapter<PartitionAdapter.PartitionViewHolder>() {

    inner class PartitionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.partitionName)
        val sizeText: TextView = itemView.findViewById(R.id.partitionSize)
        val typeText: TextView = itemView.findViewById(R.id.partitionType)
        
        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(partitions[adapterPosition])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartitionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_partition, parent, false)
        return PartitionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartitionViewHolder, position: Int) {
        val partition = partitions[position]
        holder.nameText.text = partition.name
        holder.sizeText.text = partition.size
        holder.typeText.text = partition.type
    }

    override fun getItemCount(): Int = partitions.size

    fun updatePartitions(newPartitions: List<PartitionInfo>) {
        partitions = newPartitions
        notifyDataSetChanged()
    }
    
    fun clear() {
        partitions = emptyList()
        notifyDataSetChanged()
    }
}
