package com.example.kaimera

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kaimera.managers.LauncherPreferencesManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class LauncherSettingsActivity : AppCompatActivity() {

    private lateinit var appsRecyclerView: RecyclerView
    private lateinit var saveButton: MaterialButton
    private lateinit var toolbar: MaterialToolbar
    
    private lateinit var preferencesManager: LauncherPreferencesManager
    private lateinit var adapter: LauncherAppsAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher_settings)

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        appsRecyclerView = findViewById(R.id.appsRecyclerView)
        saveButton = findViewById(R.id.saveButton)

        // Set up toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Home button
        findViewById<android.view.View>(R.id.btn_home)?.setOnClickListener {
            finish()
        }

        // Initialize preferences manager
        preferencesManager = LauncherPreferencesManager(this)

        // Load apps
        val apps = preferencesManager.getLauncherApps().toMutableList()

        // Set up RecyclerView
        adapter = LauncherAppsAdapter(apps) { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }
        
        appsRecyclerView.layoutManager = LinearLayoutManager(this)
        appsRecyclerView.adapter = adapter

        // Set up drag-and-drop
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                adapter.onItemMove(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }

            override fun isLongPressDragEnabled(): Boolean {
                // Disable long press drag, we use the drag handle instead
                return false
            }
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(appsRecyclerView)

        // Set up save button
        saveButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun saveChanges() {
        val apps = adapter.getApps()
        preferencesManager.saveLauncherApps(apps)
        
        Toast.makeText(this, "Launcher customization saved", Toast.LENGTH_SHORT).show()
        
        // Set result to indicate changes were saved
        setResult(RESULT_OK)
        finish()
    }
}
