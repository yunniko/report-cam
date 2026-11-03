# D015 · A stamped, unsent photo is kept on disk with its caption state
Date: 2026-11-03 · Goal: G-003 M1 · Status: active (superseded by: —)
Context: D010 kept only the waiting photo; a stamped photo, with a caption the user had started editing, was lost when the system killed the app.
Decision: Before a stamped photo is shown, its file and a ready entry (file name, capture time, caption, caption switch, saved flag) are written to `filesDir/pending`; edits are written on `onStop`, and the entry is dropped on send, Retake or a new shot.
Force: requirement — Owner instruction in G-003: a stamped, unsent photo survives a kill with its caption, switch and saved flag, and is dropped on Retake.
Rejected: `onSaveInstanceState` (lost on force-stop and process death after swipe); keeping only the caption and restamping on restore (settings may have changed since).
Consequence: At most one entry lives at a time, waiting or ready; the ready entry names a bare file name only, so a tampered entry cannot point outside the app.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/review/PendingStoreTest.kt; app/src/androidTest/kotlin/cz/julienika/reportcam/ReviewActionsTest.kt
