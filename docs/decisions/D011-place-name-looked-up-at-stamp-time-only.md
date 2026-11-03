# D011 · The place name is looked up once, for the fix being stamped, and is off by default
Date: 2026-11-03 · Goal: G-002 M3 · Status: active (superseded by: —)
Context: The Owner chose Android's Geocoder (Google, online) for place names and ruled out any background lookup.
Decision: Just before stamping, `ReviewScreen` sends the stamped fix to `GeocoderPlaceLookup` once, waits at most 4 s, and stamps coordinates only on any failure; the setting defaults to off, and an approximate fix prints only its city, marked ≈.
Force: judgment — the single lookup per stamp and online-only source are Owner instructions (2026-11-03); the default, timeout and city-only rule are reasons, not compulsions.
Rejected: on by default (sends every fix to Google without the user choosing it); a street for a ±2 km fix (names a place the photo may not be); looking up while previewing (background work, and for fixes never stamped).
Consequence: A stamp with place names on can take up to 4 s longer offline-but-slow; offline-fast fails in about 0.3 s.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/stamp/PlaceFieldTest.kt; app/src/androidTest/kotlin/cz/julienika/reportcam/location/GeocoderPlaceLookupTest.kt
