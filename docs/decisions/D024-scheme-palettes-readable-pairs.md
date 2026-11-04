# D024 · Each stamp scheme has its own palettes, and every pairing in it reads at 4.5:1
Date: 2026-11-04 · Goal: G-003 M5 · Status: active (superseded by: —)
Context: Both schemes offered one shared palette, so a dark scheme could get a light background or an unreadable pair; the Owner found this after the phone test.
Decision: The dark scheme offers the Owner's five dark backgrounds and five light text colours plus black and white, the light scheme the Owner's light backgrounds and dark texts plus white and black; every background and text pairing within a scheme reaches WCAG AA 4.5:1, and a stored colour outside its scheme's palette falls back to that scheme's default.
Force: requirement — Owner, 2026-11-04: dark backgrounds with light text, light with dark, every combination readable, and the colours from the Owner's chart.
Rejected: our earlier saturated palette (replaced by the Owner's chart); a shared palette with warnings (still stamps poor pairs); carrying old out-of-palette choices over (they may be unreadable).
Consequence: A colour added to a palette must keep `StampSchemeTest` green: it checks every pairing and that the backgrounds stay dark or light.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/stamp/StampSchemeTest.kt; app/src/test/kotlin/cz/julienika/reportcam/settings/AppSettingsTest.kt
