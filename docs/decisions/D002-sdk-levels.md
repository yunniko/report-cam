# D002 · minSdk 26, compile/target SDK 36; runs on Android 17
Date: 2026-11-03 · Goal: G-001 M1 · Status: active (superseded by: —)
Context: The Owner wants the app to run on Android 17, built for a lower SDK if Android 17 offers nothing useful, and accepted Android 8.0 as the minimum.
Decision: minSdk 26; compileSdk and targetSdk 36 (Android 16), the newest platform installed.
Force: requirement for minSdk 26 (Owner, 2026-11-03); judgment for target 36 — the app uses no Android 17 API, and installing the API 37 platform would mean accepting a new SDK licence.
Rejected: target 37 (no feature needed, and an extra licence step); minSdk 21 (Owner chose 8.0; it would add old storage and location code paths).
Consequence: Raise the target when an Android 17 API or Play policy needs it. Gallery saving needs WRITE_EXTERNAL_STORAGE on API 26–28 only.
Evidence: `app/build.gradle.kts`; Owner's reply logged in GOALS.md 2026-11-03.
