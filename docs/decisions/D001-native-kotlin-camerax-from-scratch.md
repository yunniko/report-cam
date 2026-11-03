# D001 · Native Kotlin + CameraX + Views, written from scratch
Date: 2026-11-03 · Goal: G-001 M1 · Status: active (superseded by: —)
Context: The Owner wants fast start, fast capture and low power from a stamp camera, built from scratch or on an open-source app.
Decision: A native Kotlin app on CameraX with framework Views, written from scratch.
Force: judgment — startup time and camera control are the Owner's stated criteria, and this option serves both best; nothing external compels it.
Rejected: forking Open Camera (GPL-3 and a large feature surface to strip out; more work than the small app needed); Flutter, already in the portfolio through plant-care (engine start-up adds to cold start, and the camera plugin gives less direct control over release); Jetpack Compose (extra runtime on the startup path with no gain for three screens).
Consequence: Kotlin is new to the portfolio (only plant-care's Flutter shell has any). Its style config is `COMPANY/configs/kotlin.editorconfig`.
Evidence: GOALS.md G-001 brief; cold-start numbers in the progress log.
