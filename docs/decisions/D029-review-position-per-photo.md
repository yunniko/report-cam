# D029 · A position picked on the review changes that photo only
Date: 2026-11-04 · Goal: G-005 M1 · Status: active (superseded by: —)
Context: G-005 puts the stamp positions on the review screen; a tap could change the one photo or the Settings default.
Decision: A tap redraws the photo under review at that position, like the colour buttons (D025 naming, so a position tried before is reused); Settings keeps the default. A frame picked there gets the border from Settings.
Force: requirement — the Owner accepted the plan stating "per-photo position" (G-005, 2026-11-04).
Rejected: changing the Settings default from the review (one photo's need would move every later photo).
Consequence: The kept review state stores the position (`placement`); an entry without one (0.4) uses the Settings position.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/settings/AppSettingsTest.kt; app/src/test/kotlin/cz/julienika/reportcam/review/PendingStoreTest.kt
