# D008 · With "wait for precise location", the photo is taken at once and stamped when the fix settles
Date: 2026-11-03 · Goal: G-002 M1 · Status: active (superseded by: —)
Context: G-001 held the shutter until a precise fix arrived, so the moment worth photographing could pass while waiting.
Decision: The shutter always captures at once. If the fix is not precise and fresh, review shows the unstamped photo with a live readout of the current fix and accuracy, plus Retake and Use current. A fix that becomes precise stamps the photo by itself, and nothing can be sent before then.
Force: requirement — Owner instruction, 2026-11-03 (G-002), including "no sending until location is settled".
Rejected: holding the shutter (G-001 behaviour; the Owner replaced it); allowing sending with a provisional stamp (Owner ruled it out).
Consequence: The camera stays closed during the wait (D003). Tracking keeps searching without its rest pause while a photo waits, still only while the app is visible (D005). The capture time stays the shutter time; a fix newer than the shot counts as age 0.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/review/PendingLocationTest.kt; app/src/androidTest/kotlin/cz/julienika/reportcam/CaptureFlowTest.kt
