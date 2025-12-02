package com.example.kaimera

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
                )
            )
        }
    }
}
