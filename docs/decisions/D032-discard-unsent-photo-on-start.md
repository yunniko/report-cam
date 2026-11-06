# D032 · An unsent photo past the Settings limit is dropped at the next start, never by a timer
Date: 2026-11-06 · Goal: G-007 M1 · Status: active (superseded by: —)
Context: the Owner wants a photo forgotten in review to expire after a chosen time (1 minute to 12 hours, or never), optionally saved to the gallery first.
Decision: `Expiry.discardIfExpired` runs from `ReviewScreen.restore` at a cold start and from `CameraActivity.onStart` when the review comes back past its limit; it reads the capture time from the kept entry's state file, so no pixels load for a photo about to go.
Force: requirement — the Owner's spec (2026-11-06: "at next start it will check") and the no-background-work rule (G-001).
Rejected: an AlarmManager wake (background work for nothing); checking only at cold start (a phone left on all day never restarts the process).
Consequence: a stamped photo is saved under its shared name, skipped when already saved (D017); a waiting one is stamped with "Location unavailable". The default is never: D010 and D015 behaviour is unchanged. On Android 9 and older no storage permission is asked here; a refused write is logged.
Evidence: app/src/test/kotlin/cz/julienika/reportcam/review/ExpiryTest.kt; app/src/androidTest/kotlin/cz/julienika/reportcam/ReviewDiscardTest.kt
