# report-cam — Handover
Last verified: 2026-11-03 at M1 (first commit)

A native Android stamp camera for photo reports. Goal G-001 is in `GOALS.md`. Charter: `E:\CLAUDE\COMPANY\`.

## Current state

G-001 is active; M1 (camera core) is done. Verified this session on the Pixel 3a API 34 emulator, debug build:
- Live preview and capture. The photo is decoded upright at a 1600 px long edge and shown for review.
- Camera release: `scripts/camera-release-check.sh` passes launch, home, return, screen off/on, review and process kill.
- Cold start to a streaming preview: 1.2–1.6 s on the emulator (debug build, logged as `preview streaming`).

Not built yet: stamp, location, settings, sharing, exposure/focus/zoom, release signing.

## How things fit together

- `CameraActivity` is the only screen on the hot path. It has two states, live camera and review, in one layout.
  Review is a state, not a new Activity, so no Activity launch sits between the shutter and sending.
- `camera/CameraSession` owns CameraX. It opens in `onStart` and closes in `onStop` and after every capture (D003).
- `photo/` holds pure size arithmetic (`OutputSizing`, unit-tested) and the decoder that turns camera JPEG bytes
  into an upright bitmap at the output size.
- `ReportCamApp` limits CameraX to the back camera, so start-up enumerates one device.

## Rules in force

- Never hold the camera outside a visible live preview: close it in `onStop` and after capture (Owner, D003).
- Nothing runs in the background: no services, no WorkManager, no background location (Owner, 2026-11-03).
- minSdk 26 (Owner, D002).

## Next steps and open questions

- M2: the stamp engine (fields, overlay/frame layout, style) with unit tests.
- Then M3 (location, settings, EXIF, gallery), M4 (sharing, camera controls), M5 (verification, release APK).

## Deploy log

| Date | Commit | What changed | How verified |
|---|---|---|---|

## Decisions

See `docs/decisions/README.md`.
