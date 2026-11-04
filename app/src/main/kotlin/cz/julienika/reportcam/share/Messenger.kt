package cz.julienika.reportcam.share

import cz.julienika.reportcam.ui.Contrast

/**
 * The messengers a quick-send button can target: those the Owner supplied icons for (Boxicons brands, MIT).
 * Each package must also be listed in the manifest's `<queries>`, or Android 11+ hides it from us.
 * [packages] are in order of preference: the first one installed is used. [brandColor] fills its button (Owner,
 * G-003 phone test); the icon and label take [foreground], white or black, whichever is readable (D023).
 */
enum class Messenger(
    val id: String,
    val label: String,
    val packages: List<String>,
    val brandColor: Int,
) {
    WHATSAPP("whatsapp", "WhatsApp", listOf("com.whatsapp", "com.whatsapp.w4b"), 0xFF25D366.toInt()),
    TELEGRAM(
        "telegram",
        "Telegram",
        listOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram"),
        0xFF229ED9.toInt(),
    ),
    MESSENGER("messenger", "Messenger", listOf("com.facebook.orca"), 0xFF0084FF.toInt()),
    DISCORD("discord", "Discord", listOf("com.discord"), 0xFF5865F2.toInt()),
    SLACK("slack", "Slack", listOf("com.Slack"), 0xFF4A154B.toInt()),
    SNAPCHAT("snapchat", "Snapchat", listOf("com.snapchat.android"), 0xFFFFFC00.toInt()),
    INSTAGRAM("instagram", "Instagram", listOf("com.instagram.android"), 0xFFE1306C.toInt()),
    ;

    val foreground: Int get() = Contrast.foregroundOn(brandColor)

    fun firstInstalled(isInstalled: (String) -> Boolean): String? = packages.firstOrNull(isInstalled)

    companion object {
        /** At most three quick buttons on the review screen (Owner, 2026-11-03). */
        const val QUICK_MAX = 3

        fun of(id: String): Messenger? = entries.firstOrNull { it.id == id }
    }
}
