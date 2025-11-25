package com.example.kaimera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kaimera.MainActivity.CameraFilter

class FilterAdapter(
    private val filters: List<CameraFilter>,
    private val onFilterSelected: (CameraFilter) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    inner class FilterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.filterName)
        val preview: ImageView = view.findViewById(R.id.filterPreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter, parent, false)
        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = filters[position]
        holder.nameText.text = filter.name
        // Simple preview: use a placeholder color or icon
        holder.preview.setImageResource(android.R.drawable.ic_menu_gallery)
        holder.itemView.setOnClickListener { onFilterSelected(filter) }
    }

    override fun getItemCount(): Int = filters.size
}
