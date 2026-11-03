# report-cam — Handover
Last verified: 2026-11-03 at 5869fd6

A native Android stamp camera for photo reports. Open goals are in `GOALS.md`. Charter: `E:\CLAUDE\COMPANY\`.

## Current state

G-001 is done (archived in `docs/goals-archive.md`); G-002 is active, all 4 milestones done, waiting for the Owner's phone test and sign-off. State on the Pixel 3a
API 34 emulator at the commit above (debug build unless marked release):
- Capture → stamped JPEG (1280 px long edge by default), shown for review; gallery copy in `Pictures/Report Cam`.
- Photo shape (D009): full frame, 1:1, 4:5, 2:3 or 9:16, short:long following how the phone is held; centre-cropped
  before stamping. The preview shows the whole sensor frame and dims what the crop will cut away.
- Stamp: settings text, date/time, coordinates with accuracy, approximate and age marks; overlay or frame. The
  frame now borders all four edges (none / thin / medium / thick), text in the bottom or top band.
- Place name (D011): off by default; full address, street and number, street and city, or city, above the
  coordinates and in XMP and the caption. Looked up once through Android's Geocoder just before stamping
  (650 ms on the emulator, Google answering); offline it fails in about 0.3 s and the stamp keeps coordinates only.
  Google gives Prague no locality, so the city reads "Hlavní město Praha". An approximate fix prints only ≈city.
- Default exposure: Settings sets the EV the camera opens at (−2 to +2); tapping the EV label returns to it.
- Location marker: green precise, amber approximate/old/searching (pulses while searching), gray and crossed
  out when denied or off. Tapping it asks for permission or opens settings. Denied → no stamp line, no GPS tags.
- Metadata: EXIF (time with offset, GPS, accuracy, ASCII description) and XMP with every field; Czech text
  round-trips. The stamp text is the share caption when the setting is on (default).
- Wait for precise location (D008, D010): the photo is taken at once and waits unstamped in review with a live
  readout of the current fix and accuracy; Retake, Use current, or a precise fix stamps it. Sharing appears only
  once stamped. The waiting photo is kept on disk and comes back after `am force-stop` with its capture time;
  switching location off or denying it keeps it waiting and says so.
- Sharing: chooser verified; direct WhatsApp/Telegram buttons appear only when installed (not on the emulator).
- Camera controls: tap-to-focus and exposure seen working; pinch zoom not testable through adb.
- Tests: 97 unit, 18 instrumented (shoot → review → share; deferred location through mock providers, including
  restart; square crop; the real geocoder, skipped offline). The emulator camera currently runs at about
  1 frame/s, so `DeferredLocationTest` cases take 47–215 s each; they pass.
- Release build 0.2.0 (code 3): signed with the D007 key, 1.43 MB, sent to the Owner.
  `scripts/camera-release-check.sh` on it: 10/10 with `WAIT=6` twice; at the default 3 s wait the "screen off"
  step can fail, because camera close on this emulator takes 2–5 s. 0.1.1 behaves the same here.
- Startup to a streaming preview on today's emulator: 0.2.0 2.5–3.8 s, 0.1.1 2.2–4.2 s, measured back to back,
  so no regression. Both are slower than G-001's 0.55–0.89 s because the emulator is.

Not yet verified: anything on a real phone, including the place-name format Google returns there; the direct
WhatsApp/Telegram buttons and how they handle the caption; pinch zoom; startup times on a healthy device.

## How things fit together

- `CameraActivity` is the only screen on the hot path, with two states (live camera, review) in one layout,
  so no Activity launch sits between the shutter and sending. `review/ReviewScreen` owns the review half
  (Restoring / Waiting / Stamping / Ready, a generation counter drops stamps for abandoned photos);
  `PendingLocation` holds its pure rules, `PendingStore` the on-disk copy of a waiting photo in `filesDir/pending`;
  `location/PlaceLookup` the geocoder call made inside the stamp step.
  Settings is a separate AppCompat activity.
- `camera/CameraSession` owns CameraX: open in `onStart`, close in `onStop` and after every capture (D003).
  `camera/CropGuideView` draws the crop over the preview with the same `photo/AspectCrop` math the pipeline uses.
- `location/`: `LocationTracker` (platform LocationManager, started/stopped with the activity) and the pure
  `FixPolicy` and `LocationMarker` rules (D005). `LocationPermissions` decides prompt vs. settings.
- `stamp/` is the extensible stamp (D004). Only `StampRenderer.kt` touches Android graphics; `StampLayout`
  places the photo, border and band.
- `photo/PhotoPipeline` works in two steps off the main thread: `decode` (rotate, crop) after capture, `finish`
  (stamp, encode into `cacheDir/shared/`) once the stamp data is final;
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
- Place names are looked up only for the fix being stamped, and a failed lookup never blocks the photo (Owner, G-002).
- Nothing is sent before the location is settled (Owner, 2026-11-03, D008).
- A waiting photo is lost only when the user drops it: keep it across restarts and lost location (Owner, D010).
- XMP must stay ASCII with character references: ExifInterface corrupts non-ASCII XMP (measured, D006).
- Do not export `MSYS_NO_PATHCONV` when running `gradlew` in Git Bash: the wrapper then fails to start
  (measured); set it only for `adb` commands.

## Next steps and open questions

- G-002 (`GOALS.md`): waiting for the Owner's phone test of 0.2.0 and sign-off. Day of week in the date is
  postponed to a later goal.
- D009 reads the aspect ratio as following how the phone is held; the Owner may want it fixed instead.
- Back up the release key folder `.android-keys` in the Windows user profile: updates over 0.1.0 need it (D007).

## Deploy log

| Date | Commit | What changed | How verified |
|---|---|---|---|
| 2026-11-03 | 5869fd6 | APK 0.2.0 sent to the Owner: G-002 (shape, frame border, default EV, place name, deferred location) | `apksigner verify`; release check and startup on the emulator |
| 2026-11-03 | a870fd4 | APK 0.1.1 sent to the Owner: Settings toolbar no longer covers rows on Android 15+ | emulator screenshots, debug and release |
| 2026-11-03 | eb92b27 | APK 0.1.0 sent to the Owner on Telegram for a phone test | `apksigner verify`; release run on the emulator |

## Decisions

See `docs/decisions/README.md`.
