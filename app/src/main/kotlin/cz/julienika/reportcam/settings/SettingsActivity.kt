package cz.julienika.reportcam.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cz.julienika.reportcam.R
import cz.julienika.reportcam.location.LocationAccess
import cz.julienika.reportcam.location.LocationPermissions
import cz.julienika.reportcam.location.LocationTracker

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
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
            setPreferencesFromResource(R.xml.preferences, rootKey)
            findPreference<Preference>(KEY_PERMISSION)?.setOnPreferenceClickListener {
                permissions.resolve(LocationTracker(requireContext()).access())
                true
            }
        }

        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)
            // Edge-to-edge (target 35+): keep the last row above the navigation bar.
            ViewCompat.setOnApplyWindowInsetsListener(listView) { v, insets ->
                v.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
                insets
            }
        }

        override fun onResume() {
            super.onResume()
            refreshLocationState()
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
            val LOCATION_KEYS = listOf(Keys.SHOW_LOCATION, Keys.COORD_FORMAT, Keys.WAIT_PRECISE)
        }
    }
}
