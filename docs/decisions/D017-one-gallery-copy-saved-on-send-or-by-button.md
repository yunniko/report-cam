# D017 · The gallery gets at most one copy, saved on send or by the Save button
Date: 2026-11-03 · Goal: G-003 M1 · Status: active (superseded in part by: D025, saved status per version)
Context: Every stamped photo used to be copied to the gallery at once, so retakes left copies and sending twice could not be told apart.
Decision: The "Save to gallery when sent" setting copies on the first send; the review screen's Save button copies without sending; a saved flag, kept with the photo (D015), stops a second copy.
Force: requirement — Owner instruction in G-003: one gallery copy per photo at most, and a retake without sending leaves none.
Rejected: saving on stamping and deleting on Retake (needs delete rights on older Android and briefly shows the copy elsewhere).
Consequence: The saved flag is set before the gallery write and reset if it fails; a restamp (G-003 M2) must clear it.
Evidence: app/src/androidTest/kotlin/cz/julienika/reportcam/ReviewActionsTest.kt
