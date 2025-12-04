package com.example.kaimera
import com.example.kaimera.mms.ui.MmsActivity
import com.example.kaimera.chronometer.ui.ChronometerActivity
import com.example.kaimera.colorlight.ui.ColorLightActivity
import com.example.kaimera.browser.ui.BrowserActivity
import com.example.kaimera.files.ui.FileExplorerActivity
import com.example.kaimera.camera.ui.MainActivity
import com.example.kaimera.camera.ui.SettingsActivity
import com.example.kaimera.gallery.ui.GalleryActivity
import com.example.kaimera.notes.ui.NoteActivity
import com.example.kaimera.sphereqix.SphereQixActivity
import com.example.kaimera.R

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents an app that can be displayed in the launcher
 */
@Parcelize
data class LauncherApp(
    val id: String,
    val name: String,
    val iconRes: Int,
    val activityClass: Class<*>,
    var isVisible: Boolean = true,
    var order: Int = 0
) : Parcelable {
    companion object {
        /**
         * Returns the default list of all available launcher apps
         */
        fun getDefaultApps(): List<LauncherApp> {
            return listOf(
                LauncherApp(
                    id = "camera",
                    name = "Camera",
                    iconRes = R.drawable.ic_camera,
                    activityClass = MainActivity::class.java,
                    order = 0
                ),
                LauncherApp(
                    id = "browser",
                    name = "Browser",
                    iconRes = R.drawable.ic_search,
                    activityClass = BrowserActivity::class.java,
                    order = 1
                ),
                LauncherApp(
                    id = "files",
                    name = "Files",
                    iconRes = R.drawable.ic_folder,
                    activityClass = FileExplorerActivity::class.java,
                    order = 2
                ),
                LauncherApp(
                    id = "light",
                    name = "Light",
                    iconRes = R.drawable.ic_color_light,
                    activityClass = ColorLightActivity::class.java,
                    order = 3
                ),
                LauncherApp(
                    id = "timer",
                    name = "Timer",
                    iconRes = android.R.drawable.ic_menu_recent_history,
                    activityClass = ChronometerActivity::class.java,
                    order = 4
                ),
                LauncherApp(
                    id = "mms",
                    name = "MMS",
                    iconRes = android.R.drawable.ic_menu_send,
                    activityClass = MmsActivity::class.java,
                    order = 5
                ),
                LauncherApp(
                    id = "settings",
                    name = "Settings",
                    iconRes = R.drawable.ic_settings,
                    activityClass = SettingsActivity::class.java,
                    order = 6
                ),
                LauncherApp(
                    id = "notes",
                    name = "Notes",
                    iconRes = android.R.drawable.ic_menu_edit, // Placeholder icon
                    activityClass = com.example.kaimera.notes.ui.NoteActivity::class.java,
                    order = 7
                ),
                LauncherApp(
                    id = "sphereqix",
                    name = "SphereQix",
                    iconRes = R.mipmap.ic_launcher, // Placeholder icon, assuming this is desired from the snippet
                    activityClass = SphereQixActivity::class.java,
                    order = 8
                )
            )
        }
    }
}
