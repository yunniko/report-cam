package cz.julienika.reportcam.settings

import androidx.appcompat.app.AppCompatDelegate

/** The app theme (Owner, G-003): Settings and the review controls follow it; the live camera view stays black. */
enum class AppTheme(
    val id: String,
    val nightMode: Int,
) {
    SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("dark", AppCompatDelegate.MODE_NIGHT_YES),
    ;

    /** Whether this theme is dark on a phone whose system setting is [systemDark]. */
    fun isDark(systemDark: Boolean): Boolean =
        when (this) {
            SYSTEM -> systemDark
            LIGHT -> false
            DARK -> true
        }

    companion object {
        /** An unknown or missing value follows the system. */
        fun of(id: String?): AppTheme = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}
