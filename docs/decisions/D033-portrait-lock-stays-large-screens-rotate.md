# D033 · The portrait lock stays; large screens may rotate the camera
Date: 2026-11-06 · Goal: G-006 M1 · Status: active (superseded by: —)
Context: `CameraActivity` is locked to portrait. Android 16 ignores that lock on screens 600 dp and wider for apps targeting SDK 36, with an opt-out until SDK 37 removes it, and Play Console notes the lock under large-screen compatibility.
Decision: keep the lock, do not opt out, and let the activity rotate and recreate on large screens.
Force: judgment — a report camera is held upright on a phone, and the opt-out buys a year; no landscape layout until a large-screen user asks.
Rejected: opting out with `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` (gone at SDK 37, which Play requires by 2027); dropping the lock on phones (the preview and crop guide are laid out for portrait, D009).
Consequence: a rotation on a large screen recreates the activity; a photo in review survives through the pending store (D015), as after a process kill. The landscape layout is unverified here (no large-screen emulator); Play's pre-launch report in M5 shows it. The Play note is accepted.
Evidence: `app/src/main/AndroidManifest.xml`; Android Developers Blog, "Orientation and resizability changes in Android 16" (2025-01, read 2026-11-06).
