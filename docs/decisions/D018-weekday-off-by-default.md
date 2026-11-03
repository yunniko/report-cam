# D018 · The day of the week is off by default and prints before the date
Date: 2026-11-03 · Goal: G-003 M2 · Status: active (superseded by: —)
Context: G-003 asks for a switchable day of the week in the stamp's date.
Decision: A "Day of the week" switch, off by default, prefixes the short day name in the phone's language ("út 2026-11-03"), unless the pattern already has one.
Force: judgment — off keeps existing stamps and their width unchanged after the update; the Owner asked for a switch, not a default.
Rejected: on by default (changes every user's stamp silently); a separate stamp line (adds a line for three letters).
Consequence: A pattern containing `E` already prints a weekday, so the switch adds nothing there.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/stamp/StampFieldTest.kt; app/src/test/kotlin/cz/julienika/reportcam/settings/AppSettingsTest.kt
