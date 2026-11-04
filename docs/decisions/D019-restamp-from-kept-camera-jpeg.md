# D019 · A restamp redraws from the kept camera JPEG into a new, suffixed file
Date: 2026-11-03 · Goal: G-003 M2 · Status: active, file naming superseded by D025
Context: The review switch between dark and light schemes must redraw the stamp, also after the app was killed (D015).
Decision: The camera JPEG and the stamp's facts (time, text, fix, place) are kept beside the stamped photo; a restamp decodes them again with the shutter-time shape and writes `report_<time>_<scheme>.jpg`.
Force: judgment — redrawing over the stamped image would stamp twice, and reusing the file name would change a file a messenger already received.
Rejected: keeping a decoded bitmap (tens of MB, lost on kill); re-reading location at restamp time (the stamp must describe the moment of capture).
Consequence: The restamped version is unsaved (D017), so Save adds one copy of it and the earlier copy stays.
Evidence: app/src/androidTest/kotlin/cz/julienika/reportcam/ReviewActionsTest.kt; app/src/test/kotlin/cz/julienika/reportcam/review/PendingStoreTest.kt
