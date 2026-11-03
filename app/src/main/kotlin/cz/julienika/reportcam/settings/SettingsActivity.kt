package cz.julienika.reportcam.settings

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import cz.julienika.reportcam.R
import cz.julienika.reportcam.location.LocationAccess
import cz.julienika.reportcam.location.LocationPermissions
import cz.julienika.reportcam.location.LocationTracker
import cz.julienika.reportcam.settings.ui.CardDecoration

class SettingsActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        // This screen only: the camera and review screens stay dark. Set before AppCompat builds this screen's
        // configuration; set later, the window keeps the old theme's background.
        delegate.localNightMode = AppTheme.of(PrefsSource.of(newBase).string(Keys.APP_THEME, "")).nightMode
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge to edge on every version, not only where Android 15+ enforces it, so all devices lay out alike.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            toolbar.updatePadding(left = bars.left, top = bars.top, right = bars.right)
            findViewById<View>(R.id.settings_container).updatePadding(left = bars.left, right = bars.right)
            insets
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.settings_container, SettingsFragment()).commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var permissions: LocationPermissions

        override fun onCreate(savedInstanceState: Bundle?) {
            permissions = LocationPermissions(this) { refreshLocationState() }
            super.onCreate(savedInstanceState)
        }

        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?,
        ) {
            val prefs = preferenceManager.sharedPreferences!!
            AppSettings.legacySeed(PrefsSource(prefs)).takeIf { it.isNotEmpty() }?.let { seed ->
                prefs.edit().apply { seed.forEach { (k, v) -> putString(k, v) } }.apply()
            }
            setPreferencesFromResource(R.xml.preferences, rootKey)
            findPreference<Preference>(KEY_PERMISSION)?.setOnPreferenceClickListener {
                permissions.resolve(LocationTracker(requireContext()).access())
                true
            }
            // Applying the mode recreates this screen in the new theme.
            findPreference<Preference>(Keys.APP_THEME)?.setOnPreferenceChangeListener { _, value ->
                (requireActivity() as AppCompatActivity).delegate.localNightMode = AppTheme.of(value as? String).nightMode
                true
            }
        }

        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)
            setDivider(null)
            listView.addItemDecoration(CardDecoration(requireContext()) { rows().map { it is PreferenceCategory }.toBooleanArray() })
            // Edge to edge: keep the last row above the navigation bar.
            ViewCompat.setOnApplyWindowInsetsListener(listView) { v, insets ->
                v.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
                insets
            }
        }

        override fun onResume() {
            super.onResume()
            refreshLocationState()
        }

        /** The rows as the list shows them: visible preferences in order, categories included. */
        private fun rows(): List<Preference> =
            buildList {
                fun walk(group: PreferenceGroup) {
                    for (i in 0 until group.preferenceCount) {
                        val p = group.getPreference(i)
                        if (!p.isVisible) continue
                        add(p)
                        if (p is PreferenceGroup) walk(p)
                    }
                }
                walk(preferenceScreen)
            }

        /** No permission: location settings greyed out, with a row that leads to the fix (Owner, 2026-11-03). */
        private fun refreshLocationState() {
            val access = LocationTracker(requireContext()).access()
            val row = findPreference<Preference>(KEY_PERMISSION) ?: return
            row.isVisible = access != LocationAccess.PRECISE
            when (access) {
                LocationAccess.DENIED -> {
                    row.setTitle(R.string.pref_location_denied)
                    row.setSummary(R.string.pref_location_denied_summary)
                }
                LocationAccess.SERVICES_OFF -> {
                    row.setTitle(R.string.pref_location_off)
                    row.setSummary(R.string.pref_location_off_summary)
                }
                LocationAccess.APPROXIMATE_ONLY -> {
                    row.setTitle(R.string.pref_location_approx)
                    row.setSummary(R.string.pref_location_approx_summary)
                }
                LocationAccess.PRECISE -> Unit
            }
            val enabled = access != LocationAccess.DENIED
            for (key in LOCATION_KEYS) findPreference<Preference>(key)?.isEnabled = enabled
        }

        private companion object {
            const val KEY_PERMISSION = "location_permission"
            val LOCATION_KEYS = listOf(Keys.SHOW_LOCATION, Keys.COORD_FORMAT, Keys.PRECISE_ACCURACY, Keys.WAIT_PRECISE)
        }
    }
}
