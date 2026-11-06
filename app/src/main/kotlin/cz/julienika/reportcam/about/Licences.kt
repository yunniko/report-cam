package cz.julienika.reportcam.about

/**
 * The third-party work the app ships and the licence each is under: the same list as README → Third-party
 * assets, kept here so Settings → About can show it with the licence texts (G-006, D034).
 */
object Licences {
    enum class Licence(
        val title: String,
        val asset: String,
    ) {
        APACHE_2("Apache License 2.0", "licenses/apache-2.0.txt"),
        MIT("MIT License", "licenses/mit-boxicons.txt"),
    }

    data class Component(
        val name: String,
        val holder: String,
        val licence: Licence,
        val url: String,
    )

    val components =
        listOf(
            Component(
                "AndroidX libraries (AppCompat, Preference, CameraX, ExifInterface, Lifecycle, Core)",
                "The Android Open Source Project",
                Licence.APACHE_2,
                "https://developer.android.com/jetpack/androidx",
            ),
            Component("kotlinx.coroutines", "JetBrains s.r.o.", Licence.APACHE_2, "https://github.com/Kotlin/kotlinx.coroutines"),
            Component(
                "Material Icons and Material Symbols (settings, location, dark and light mode)",
                "Google LLC",
                Licence.APACHE_2,
                "https://fonts.google.com/icons",
            ),
            Component(
                "OpenSearch UI icons (save, retake, share, stamp corners)",
                "OpenSearch Contributors",
                Licence.APACHE_2,
                "https://github.com/opensearch-project/oui",
            ),
            Component("Boxicons brand logos (messenger buttons)", "Aniket Suvarna", Licence.MIT, "https://github.com/atisawd/boxicons"),
        )

    const val TRADEMARKS = "The messenger logos are trademarks of their owners and label the send buttons only."

    /** The notice shown in Settings: every component, the trademark line, then each licence text once. */
    fun notice(read: (asset: String) -> String): String =
        buildString {
            for (c in components) append("${c.name}\n${c.holder} — ${c.licence.title}\n${c.url}\n\n")
            append(TRADEMARKS).append("\n\n")
            for (l in Licence.entries) append("—— ${l.title} ——\n\n").append(read(l.asset).trim()).append("\n\n")
        }.trimEnd()
}
