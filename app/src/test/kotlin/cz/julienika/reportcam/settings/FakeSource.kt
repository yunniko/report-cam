package cz.julienika.reportcam.settings

/** Settings from a map, for unit tests: a key not given reads its default. */
class FakeSource(
    vararg values: Pair<String, Any>,
) : SettingsSource {
    private val map = values.toMap()

    override fun string(
        key: String,
        default: String,
    ) = map[key] as? String ?: default

    override fun bool(
        key: String,
        default: Boolean,
    ) = map[key] as? Boolean ?: default

    @Suppress("UNCHECKED_CAST")
    override fun stringSet(
        key: String,
        default: Set<String>,
    ) = map[key] as? Set<String> ?: default
}
