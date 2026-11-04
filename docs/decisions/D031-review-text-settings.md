# D031 · Two Settings switches shape the review's Note and Message rows
Date: 2026-11-04 · Goal: G-005 M3 · Status: active (superseded by: —)
Context: the Owner asked to choose separately whether the review offers quick editing of the Note and Message, and their on/off switches.
Decision: `text_editing` off shows the texts read only; `text_switches` off hides both switches and a row that started off; both off hide both rows (`ReviewTexts.rowShown`).
Force: requirement — the Owner's spec (2026-11-04); hiding a row that started off without a switch is judgment, as it could show nothing useful.
Rejected: a pair of settings per row (the Owner named two, for both rows); keeping an off row visible as an empty label.
Consequence: "Compact text fields" depends on editing and is disabled while it is off; both switches default on, so 0.4 behaviour is unchanged.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/review/ReviewTextsRowsTest.kt; app/src/androidTest/kotlin/cz/julienika/reportcam/ReviewTextSettingsTest.kt
