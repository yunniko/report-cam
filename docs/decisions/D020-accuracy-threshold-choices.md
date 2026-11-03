# D020 · "Precise" means within 5, 10, 20, 30 or 50 m, default 20 m
Date: 2026-11-03 · Goal: G-003 M3 · Status: active (superseded by: —)
Context: A fixed 30 m decided whether a fix was precise: whether it is stamped without "approximate" and whether "wait for precise location" stops waiting.
Decision: The threshold is a setting with the steps 5 / 10 / 20 / 30 / 50 m and a 20 m default, passed explicitly to every rule that judges a fix; only `FixQuality.of`, `AppSettings` and the tracker hold the default.
Force: requirement — the steps are the Owner's (G-003), and the 20 m default was settled at acceptance, item 1.
Rejected: a free number field (the Owner asked for steps); keeping default arguments on each rule (a caller could silently ignore the setting).
Consequence: A new rule that judges fix quality takes the threshold as a parameter, with no default of its own.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/location/FixPolicyTest.kt; app/src/test/kotlin/cz/julienika/reportcam/settings/AppSettingsTest.kt; app/src/androidTest/kotlin/cz/julienika/reportcam/SettingsScreenTest.kt
