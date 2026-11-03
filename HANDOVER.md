# report-cam — Handover
Last verified: 2026-11-03 at 56df0e7

A native Android stamp camera for photo reports. Goal G-001 is in `GOALS.md`. Charter: `E:\CLAUDE\COMPANY\`.

## Current state

G-001 is active; M1–M4 are done, M5 (release and phone test) remains. Verified this session on the Pixel 3a
API 34 emulator, debug build:
- Capture → stamped 960×1280 JPEG (1280 px default) in 442–482 ms, shown for review; gallery copy in
  `Pictures/Report Cam`.
- Stamp: settings text, date/time, coordinates with accuracy, approximate and age marks; overlay or frame.
- Location marker: green precise, amber approximate/old/searching (pulses while searching), gray and crossed
  out when denied or off. Tapping it asks for permission or opens settings. Denied → no stamp line, no GPS tags.
- Metadata: EXIF (time with offset, GPS, accuracy, ASCII description) and XMP with every field; Czech text
  round-trips. The stamp text is the share caption when the setting is on (default).
- Sharing: chooser verified; direct WhatsApp/Telegram buttons appear only when installed (not on the emulator).
- Camera controls: tap-to-focus and exposure seen working; pinch zoom not testable through adb.
- Tests: 53 unit, 7 instrumented; `scripts/camera-release-check.sh` 10/10 including the share steps.
- Cold start to streaming preview: 0.8–2.4 s on the emulator, debug build (`am start -W` / `preview streaming` log).

Not built yet: release signing and a release build; nothing tested on a real phone.

## How things fit together

- `CameraActivity` is the only screen on the hot path, with two states (live camera, review) in one layout,
  so no Activity launch sits between the shutter and sending. Settings is a separate AppCompat activity.
- `camera/CameraSession` owns CameraX: open in `onStart`, close in `onStop` and after every capture (D003).
- `location/`: `LocationTracker` (platform LocationManager, started/stopped with the activity) and the pure
  `FixPolicy` and `LocationMarker` rules (D005). `LocationPermissions` decides prompt vs. settings.
- `stamp/` is the extensible stamp (D004). Only `StampRenderer.kt` touches Android graphics.
- `photo/PhotoPipeline` decodes, stamps and encodes off the main thread into `cacheDir/shared/`;
  `ExifWriter` then adds `PhotoMetadata` (D006); `GallerySaver` copies to MediaStore.
- `settings/AppSettings` maps stored preferences to what the app uses; keys and defaults live in `Keys`.
- `share/Sharing` builds the send intent through the FileProvider; `SharedFiles` prunes copies after 24 h.

## Rules in force

- Never hold the camera outside a visible live preview: close it in `onStop` and after capture (Owner, D003).
- Nothing runs in the background: no services, no WorkManager, no background location (Owner, 2026-11-03).
- minSdk 26 (Owner, D002).
- Location is tracked only while visible and never fetched by a capture (Owner, D005).
- No location permission means no location line and no GPS metadata (Owner, 2026-11-03).
- XMP must stay ASCII with character references: ExifInterface corrupts non-ASCII XMP (measured, D006).
- Do not export `MSYS_NO_PATHCONV` when running `gradlew` in Git Bash: the wrapper then fails to start
  (measured); set it only for `adb` commands.

## Next steps and open questions

- M5: release signing (keystore outside the repo, `keystore.properties` git-ignored), release build,
  startup measured on it, release APK to the Owner by Telegram for a phone test: cold start, WhatsApp and
  Telegram caption handling, pinch zoom, a real GPS fix outdoors.
- Later (Owner): a street-address stamp field.

## Deploy log

| Date | Commit | What changed | How verified |
|---|---|---|---|

## Decisions

See `docs/decisions/README.md`.
