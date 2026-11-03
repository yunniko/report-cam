# D005 · Location is tracked in a foreground duty cycle, never on capture
Date: 2026-11-03 · Goal: G-001 M3 · Status: active (superseded by: —)
Context: The Owner wants location refreshed on app start and wake and tracked in reasonable cycles while the app runs, never fetched by the shutter, with the stamp marking old or approximate fixes.
Decision: `LocationTracker` runs only between onStart and onStop: it searches at 1 s until a precise fresh fix (or 45 s), then rests 30 s with the receiver off; the shutter reads the best fix it already has.
Force: requirement — Owner instructions, 2026-11-03; the 1 s / 45 s / 30 s / 30 m / 2 min constants are judgment.
Rejected: Fused Location Provider (adds Play services to a framework-only app for little gain at these intervals); a fix per capture (delays the shutter, Owner ruled it out); continuous updates (receiver on for the whole session).
Consequence: The stamp may carry a fix up to the rest interval old; it says so when over 2 min. "Wait for precise" may restart a search early (`ensureSearching`).
Evidence: `app/src/test/kotlin/cz/julienika/reportcam/location/FixPolicyTest.kt`; `app/src/test/kotlin/cz/julienika/reportcam/location/LocationMarkerTest.kt`; emulator checks in the G-001 progress log.
