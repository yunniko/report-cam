# D016 · A single quick messenger sits left of Share by default
Date: 2026-11-03 · Goal: G-003 M1 · Status: active (superseded by: —)
Context: With one quick messenger chosen it shares Share's row, on the side set in Settings; the setting needed a default.
Decision: The default is left ("start", so right in right-to-left languages).
Force: tie-break — either side works and the Owner can change it in Settings.
Rejected: right by default (no reason to prefer it).
Consequence: None beyond the default value in `Keys.DEFAULT_QUICK_SIDE`.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/review/ShareButtonsTest.kt
