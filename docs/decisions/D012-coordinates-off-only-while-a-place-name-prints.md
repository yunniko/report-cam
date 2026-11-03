# D012 · "Coordinates on the stamp" off drops them only while a place name prints
Date: 2026-11-03 · Goal: G-002 follow-up · Status: active (superseded by: —)
Context: The Owner asked to turn the coordinates off the stamp while the place name shows.
Decision: With the switch off and a place format chosen, `CoordinatesField` prints nothing whenever the place field prints a line; when no place is found (offline, no geocoder, nothing in that format) the coordinates print as before.
Force: judgment — the switch is an Owner instruction (2026-11-03); keeping coordinates as the fallback is so a stamp never loses its location.
Rejected: hiding coordinates unconditionally (an offline photo would carry no location on its face); a separate "place or coordinates" field (two fields already exist and the registry keeps metadata in step).
Consequence: EXIF GPS and the XMP fix stay in the file regardless; the switch only affects the stamp and caption. It is ignored while place names are off.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/settings/AppSettingsTest.kt
