# report-cam — Handover
Last verified: 2026-11-03 at 2ebdd63

A native Android stamp camera for photo reports. Goals are in `GOALS.md`, kept local. Charter: `E:\CLAUDE\COMPANY\`.

## Current state

G-001 and G-002 are done and signed off (archived in `docs/goals-archive.md`). G-003 is open: M1 (review screen) is done,
M2–M4 remain. State on the Pixel 3a API 34 emulator at the commit above (debug build unless marked release):
- Capture → stamped JPEG (1280 px long edge by default), shown for review between the top bar and the controls.
- Review screen (G-003 M1): caption switch (starts at the Settings value) and editable caption; up to three quick
  messenger buttons, own row for two or three, beside Share (left or right, D016) for one; Share…; Retake and
  Save to gallery. Only installed messengers show. Seen on the emulator with stand-in apps (`stub-messenger/`) for
  1 left, 1 right, 2 and 3 buttons; the WhatsApp stand-in received the photo and the caption.
- Gallery (D017): at most one copy in `Pictures/Report Cam`, on the first send when "Save to gallery when sent" is on,
  or by the Save button; a retake without sending leaves none.
- A stamped, unsent photo is kept on disk with caption, switch and saved flag (D015) and came back after
  `am force-stop`; sending, Retake or a new shot drops it.
- Photo shape (D009): full frame, 1:1, 4:5, 2:3 or 9:16, short:long following how the phone is held; centre-cropped
  before stamping. The preview shows the whole sensor frame and dims what the crop will cut away.
- Stamp: settings text, date/time, coordinates with accuracy, approximate and age marks; overlay or frame. The
  frame borders all four edges: none / thin 2.5 % / medium 5 % (default) / thick 10 % of the short edge (D013),
  text in the bottom or top band.
- Place name (D011): off by default; full address, street and number, street and city, or city, above the
  coordinates and in XMP and the caption. Looked up once through Android's Geocoder just before stamping
  (650 ms on the emulator, Google answering); offline it fails in about 0.3 s and the stamp keeps coordinates only.
  Google gives Prague no locality; the city is then read from its address line, "Praha 1-Staré Město" (D014),
  and the region is the last fallback. An approximate fix prints only ≈city.
  "Coordinates on the stamp" off (D012): only the place name prints; with no place found the coordinates
  print as before. GPS metadata is kept either way. Seen on the emulator online and offline.
- Default exposure: Settings sets the EV the camera opens at (−2 to +2); tapping the EV label returns to it.
- Location marker: green precise, amber approximate/old/searching (pulses while searching), gray and crossed
  out when denied or off. Tapping it asks for permission or opens settings. Denied → no stamp line, no GPS tags.
- Metadata: EXIF (time with offset, GPS, accuracy, ASCII description) and XMP with every field; Czech text
  round-trips. The stamp text is the share caption when the setting is on (default).
- Wait for precise location (D008, D010): the photo is taken at once and waits unstamped in review with a live
  readout of the current fix and accuracy; Retake, Use current, or a precise fix stamps it. Sharing appears only
  once stamped. The waiting photo is kept on disk and comes back after `am force-stop` with its capture time;
  switching location off or denying it keeps it waiting and says so.
- Sharing: chooser verified; quick buttons hand the photo to the messenger's package with `ACTION_SEND`.
- Camera controls: tap-to-focus and exposure seen working; pinch zoom not testable through adb.
- Tests: 116 unit, 25 instrumented (shoot → review → share; deferred location through mock providers, including
  restart; square crop; the real geocoder, skipped offline; caption, one-copy rule and stamped-photo restore). The emulator camera currently runs at about
  1 frame/s, so `DeferredLocationTest` cases take 47–215 s each; they pass.
- Release build 0.2.2 (code 5): signed with the D007 key, 1.43 MB, sent to the Owner.
  `scripts/camera-release-check.sh` on 0.2.0: 10/10 with `WAIT=6` twice; at the default 3 s wait the "screen off"
  step can fail, because camera close on this emulator takes 2–5 s. 0.1.1 behaves the same here.
- Startup to a streaming preview on today's emulator: 0.2.0 2.5–3.8 s, 0.1.1 2.2–4.2 s, measured back to back,
  so no regression. Both are slower than G-001's 0.55–0.89 s because the emulator is.

Not yet verified: anything on a real phone, including the place-name format Google returns there; the direct
messengers and how each handles the caption; pinch zoom; startup times on a healthy device.

## How things fit together

- `CameraActivity` is the only screen on the hot path, with two states (live camera, review) in one layout,
  so no Activity launch sits between the shutter and sending. `review/ReviewScreen` owns the review half
  (Restoring / Waiting / Stamping / Ready, a generation counter drops stamps for abandoned photos);
  `PendingLocation` holds its pure rules, `ShareButtons` the quick-button layout rule, and `PendingStore` the on-disk
  copy in `filesDir/pending`: a waiting photo or a stamped one with its ready entry, never both;
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
- `share/Sharing` builds the send intent through the FileProvider; `SharedFiles` prunes copies after 24 h;
  `share/Messenger` lists the quick messengers and their packages, which the manifest's `<queries>` must also name.
- `stub-messenger/` is a test-only module: one APK per messenger under its real package, logging what it receives.

## Rules in force

- `GOALS.md` and `docs/goals-archive.md` stay out of the public repo, through `.git/info/exclude` (Owner, 2026-11-03).
- Never hold the camera outside a visible live preview: close it in `onStop` and after capture (Owner, D003).
- Nothing runs in the background: no services, no WorkManager, no background location (Owner, 2026-11-03).
- minSdk 26 (Owner, D002).
- Location is tracked only while visible and never fetched by a capture (Owner, D005).
- No location permission means no location line and no GPS metadata (Owner, 2026-11-03).
- Place names are looked up only for the fix being stamped, and a failed lookup never blocks the photo (Owner, G-002).
- Nothing is sent before the location is settled (Owner, 2026-11-03, D008).
- A waiting photo is lost only when the user drops it: keep it across restarts and lost location (Owner, D010).
- A stamped, unsent photo survives a kill with its caption state; Retake drops it (Owner, D015).
- At most one gallery copy per photo; a retake without sending leaves none (Owner, D017).
- XMP must stay ASCII with character references: ExifInterface corrupts non-ASCII XMP (measured, D006).
- Do not export `MSYS_NO_PATHCONV` when running `gradlew` in Git Bash: the wrapper then fails to start
  (measured); set it only for `adb` commands.

## Next steps and open questions

- G-003 M2: dark and light stamp schemes, the review switch that restamps (a restamp clears the saved flag),
  day of the week in the date. Then M3 (settings redesign) and M4 (verification, release 0.3.0).
- D009 reads the aspect ratio as following how the phone is held; the Owner may want it fixed instead.
- Back up the release key folder `.android-keys` in the Windows user profile: updates over 0.1.0 need it (D007).

## Deploy log

| Date | Commit | What changed | How verified |
|---|---|---|---|
| 2026-11-03 | 2ebdd63 | Repo pushed public to github.com/yunniko/report-cam; APK 0.2.2 sent: city from the address line (D014) | secrets scan of the history; `apksigner verify`; unit 103/103 |
| 2026-11-03 | d534cc7 | APK 0.2.1 sent to the Owner: border scale (D013), coordinates switch (D012) | `apksigner verify`; release launch on the emulator; unit 100/100, instrumented 18/18 |
| 2026-11-03 | 3e2586f | APK 0.2.0 sent to the Owner: G-002 (shape, frame border, default EV, place name, deferred location) | `apksigner verify`; release check and startup on the emulator |
| 2026-11-03 | 6eddd7f | APK 0.1.1 sent to the Owner: Settings toolbar no longer covers rows on Android 15+ | emulator screenshots, debug and release |
| 2026-11-03 | bcdd34e | APK 0.1.0 sent to the Owner on Telegram for a phone test | `apksigner verify`; release run on the emulator |

## Decisions

See `docs/decisions/README.md`.
