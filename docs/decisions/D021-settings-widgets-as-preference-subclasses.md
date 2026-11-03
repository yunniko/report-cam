# D021 · Settings controls are custom Preference rows, drawn as cards by one decoration
Date: 2026-11-03 · Goal: G-003 M3 · Status: active (superseded by: —)
Context: The redesign needs grouped cards, segmented buttons, a corner grid, sliders, a colour dialog and a messenger picker, and every existing setting must keep its stored key.
Decision: Each control is a `Preference` subclass in `settings/ui/` on one shared row layout, and `CardDecoration` draws each category's rows as one rounded card on the stock `PreferenceFragmentCompat`.
Force: judgment — the preference library keeps storage, dependencies and keys unchanged, and the controls need nothing Material Components would add.
Rejected: Material Components (a new dependency for buttons and a slider the framework has); a hand-built screen without preferences (every key, default and dependency rewritten by hand).
Consequence: A new setting is a row in `preferences.xml`, using an existing control where one fits; the card look comes from the decoration, not from row backgrounds.
Evidence: app/src/main/kotlin/cz/julienika/reportcam/settings/ui/CardDecoration.kt; app/src/androidTest/kotlin/cz/julienika/reportcam/SettingsScreenTest.kt
