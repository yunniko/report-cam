package cz.julienika.reportcam.share

/**
 * The messengers a quick-send button can target: those the Owner supplied icons for (Boxicons brands, MIT).
 * Each package must also be listed in the manifest's `<queries>`, or Android 11+ hides it from us.
 * [packages] are in order of preference: the first one installed is used.
 */
enum class Messenger(
    val id: String,
    val label: String,
    val packages: List<String>,
) {
    WHATSAPP("whatsapp", "WhatsApp", listOf("com.whatsapp", "com.whatsapp.w4b")),
    TELEGRAM("telegram", "Telegram", listOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram")),
    MESSENGER("messenger", "Messenger", listOf("com.facebook.orca")),
    DISCORD("discord", "Discord", listOf("com.discord")),
    SLACK("slack", "Slack", listOf("com.Slack")),
    SNAPCHAT("snapchat", "Snapchat", listOf("com.snapchat.android")),
    INSTAGRAM("instagram", "Instagram", listOf("com.instagram.android")),
    ;

    fun firstInstalled(isInstalled: (String) -> Boolean): String? = packages.firstOrNull(isInstalled)

    companion object {
        /** At most three quick buttons on the review screen (Owner, 2026-11-03). */
        const val QUICK_MAX = 3

        fun of(id: String): Messenger? = entries.firstOrNull { it.id == id }
    }
}
