package com.example.kaimera

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

class LauncherAppsAdapter(
    private val apps: MutableList<LauncherApp>,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<LauncherAppsAdapter.AppViewHolder>() {

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dragHandle: ImageView = view.findViewById(R.id.dragHandle)
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val appName: TextView = view.findViewById(R.id.appName)
        val visibilityCheckbox: CheckBox = view.findViewById(R.id.visibilityCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_launcher_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        
        holder.appIcon.setImageResource(app.iconRes)
        holder.appName.text = app.name
        holder.visibilityCheckbox.isChecked = app.isVisible
        
        // Handle checkbox changes
        holder.visibilityCheckbox.setOnCheckedChangeListener { _, isChecked ->
            app.isVisible = isChecked
        }
        
        // Handle drag start
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                onStartDrag(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int = apps.size

    /**
     * Move item from one position to another
     */
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(apps, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(apps, i, i - 1)
            }
        }
        
        // Update order values
        apps.forEachIndexed { index, app ->
            app.order = index
        }
        
        notifyItemMoved(fromPosition, toPosition)
    }

    /**
     * Get the current list of apps
     */
    fun getApps(): List<LauncherApp> = apps
}
