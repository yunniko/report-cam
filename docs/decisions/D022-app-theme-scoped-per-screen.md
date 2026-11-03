# D022 · The app theme is applied per screen; the live camera view stays black
Date: 2026-11-03 · Goal: G-003 M3 · Status: active (superseded by: —)
Context: The app theme (system, light, dark) must change Settings and the review controls, while the camera view stays black in both.
Decision: Settings sets its own `localNightMode` before it builds its configuration; the review controls are coloured by `ReviewPalette` from the theme each time a photo enters review, and the camera activity's theme never changes.
Force: requirement — the G-003 criterion that the live camera view stays black, settled at acceptance, item 3.
Rejected: process-wide `setDefaultNightMode` (recreates the camera activity and turns its window light); a night-qualified review layout (the camera activity would have to follow the night mode, and the camera view with it).
Consequence: A new control on the review panel is coloured by `ReviewPalette` unless it is added to the skip set.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/settings/AppSettingsTest.kt; app/src/main/kotlin/cz/julienika/reportcam/review/ReviewPalette.kt
