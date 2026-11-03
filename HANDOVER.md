# report-cam — Handover
Last verified: 2026-11-03 at a870fd4

A native Android stamp camera for photo reports. Goal G-001 is in `GOALS.md`. Charter: `E:\CLAUDE\COMPANY\`.

## Current state

G-001 is active; M1–M5 are built and the Owner's phone test is pending. State on the Pixel 3a
API 34 emulator at the commit above (debug build unless marked release):
- Capture → stamped 960×1280 JPEG (1280 px default) in 442–482 ms, shown for review; gallery copy in
  `Pictures/Report Cam`.
- Stamp: settings text, date/time, coordinates with accuracy, approximate and age marks; overlay or frame.
- Location marker: green precise, amber approximate/old/searching (pulses while searching), gray and crossed
  out when denied or off. Tapping it asks for permission or opens settings. Denied → no stamp line, no GPS tags.
- Metadata: EXIF (time with offset, GPS, accuracy, ASCII description) and XMP with every field; Czech text
  round-trips. The stamp text is the share caption when the setting is on (default).
- Sharing: chooser verified; direct WhatsApp/Telegram buttons appear only when installed (not on the emulator).
- Camera controls: tap-to-focus and exposure seen working; pinch zoom not testable through adb.
- Tests: 53 unit, 9 instrumented (including shoot → review → share end to end);
  `scripts/camera-release-check.sh` 10/10 on debug and release builds;
  one unexplained 3 s miss on its "home" step, not reproduced (GOALS.md log).
- Release build 0.1.1: signed (D007), 1.40 MB; preview streaming 0.55–0.89 s after process start; capture 429 ms.

Not yet verified: anything on a real phone; the direct WhatsApp/Telegram buttons and how they handle the caption; pinch zoom.

## How things fit together

- `CameraActivity` is the only screen on the hot path, with two states (live camera, review) in one layout,
  so no Activity launch sits between the shutter and sending. Settings is a separate AppCompat activity.
- `camera/CameraSession` owns CameraX: open in `onStart`, close in `onStop` and after every capture (D003).
- `location/`: `LocationTracker` (platform LocationManager, started/stopped with the activity) and the pure
  `FixPolicy` and `LocationMarker` rules (D005). `LocationPermissions` decides prompt vs. settings.
- `stamp/` is the extensible stamp (D004). Only `StampRenderer.kt` touches Android graphics.
- `photo/PhotoPipeline` decodes, stamps and encodes off the main thread into `cacheDir/shared/`;
  `ExifWriter` then adds `PhotoMetadata` (D006); `GallerySaver` copies to MediaStore.
- Both activities call `enableEdgeToEdge` on every Android version (15+ enforces it) and place their bars
  from window insets; Settings has its own toolbar in `activity_settings.xml` for that reason.
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

- PENDING APPROVAL: the Owner's phone test of APK 0.1.1 and sign-off of G-001 (cold start, WhatsApp and
  Telegram caption handling, pinch zoom, outdoor GPS). Fixes it calls for go into G-001 before sign-off.
- Back up the release key folder `.android-keys` in the Windows user profile: updates over 0.1.0 need it (D007).
- Later (Owner): a street-address stamp field.

## Deploy log

| Date | Commit | What changed | How verified |
|---|---|---|---|
| 2026-11-03 | a870fd4 | APK 0.1.1 sent to the Owner: Settings toolbar no longer covers rows on Android 15+ | emulator screenshots, debug and release |
| 2026-11-03 | eb92b27 | APK 0.1.0 sent to the Owner on Telegram for a phone test | `apksigner verify`; release run on the emulator |

## Decisions

See `docs/decisions/README.md`.
