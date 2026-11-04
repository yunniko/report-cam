# report-cam — Handover
Last verified: 2026-11-04 at cc06b20

A native Android stamp camera for photo reports. Goals are in `GOALS.md`, kept local. Charter: `E:\CLAUDE\COMPANY\`.

## Current state

G-001, G-002 and G-003 are done and signed off (archived in `docs/goals-archive.md`). G-004 is active: M1 (Note row,
hash-named versions) and M2 (compact text fields) are done, M3 and M4 are open.
State on the Pixel 3a API 34 emulator at the commit above (debug build unless marked release):
- Capture → stamped JPEG (1280 px long edge by default), shown for review between the top bar and the controls.
- Review screen (G-003 M1, G-004 M1): a Note row (the stamp text, editable for this photo) and a Message row (the text
  sent, starts at the Settings switch), each a switch that hides its field when off. A finished note edit (Done,
  leaving the field, the switch, or tapping Send/Save) redraws the photo; the message follows the note until edited by
  hand. With Settings' "Compact text fields" on (off by default, D026) both fields read as plain text, without
  underline or spelling marks, until tapped; Done, Back or a tap beside them makes them plain again. Seen in light
  and dark. Up to three quick
  messenger buttons, own row for two or three, beside Share (left or right, D016) for one; Share…; Retake and
  Save to gallery. Only installed messengers show. Seen on the emulator with stand-in apps (`stub-messenger/`) for
  1 left, 1 right, 2 and 3 buttons; the WhatsApp stand-in received the photo and the message. Quick buttons wear
  their messenger's brand colour with a white or black foreground, whichever reads at 4.5:1; a single one beside
  Share is its icon alone (D023). Changing the quick messengers in Settings while a photo is in review shows on
  coming back, without a restart. Seen on the emulator.
- Stamp schemes (G-003 M2): dark (default: white on 60 % black) and light (black on 70 % white), each with its
  own palettes from the Owner's colour chart plus black and white (six dark backgrounds with light texts, six light
  with dark; every pairing at least 4.5:1, D024),
  background, text colour and overlay opacity (20–100 %, a frame is always opaque); the old three-way colour
  setting carries over once. The review screen's Dark/Light buttons, each drawn in its scheme's colours, redraw
  from the kept camera JPEG (D019). Each version is `report_<time>_<8-hex hash of its look>.jpg` (D025): going back
  to a look reuses its file (seen: note off and on again left two files), and saved status is per version, so Save
  adds one copy of a new version and the earlier copy stays. Seen on the emulator.
- Settings (G-003 M3): grouped cards; segmented buttons for small choices; a 2×2 corner grid; sliders for
  default EV and the precise-location radius (5 / 10 / 20 / 30 / 50 m, default 20 m, D020); a scheme dialog per
  stamp scheme with background and font palettes, overlay transparency and two previews (overlay on the Owner's
  photo, frame on solid background) drawn by the stamp renderer; a quick-messenger picker of up to three.
- App theme (D022): system, light or dark, applied to Settings live and after a cold start and to the controls
  under a photo (disabled ones dimmed); the live camera view stays black. Seen on the emulator in both themes.
- Day of the week (D018): off by default; when on, the short day name in the phone's language precedes the date.
- Gallery (D017): at most one copy in `Pictures/Report Cam`, on the first send when "Save to gallery when sent" is on,
  or by the Save button; a retake without sending leaves none.
- A stamped, unsent photo is kept on disk with its camera JPEG, stamp facts, scheme, note, message, both switches and saved versions (D015) and came back after
  `am force-stop`; sending, Retake or a new shot drops it.
- Photo shape (D009): full frame, 1:1, 4:5, 2:3 or 9:16, short:long following how the phone is held; centre-cropped
  before stamping. The preview shows the whole sensor frame and dims what the crop will cut away.
- Stamp: settings text, date/time, coordinates with accuracy, approximate and age marks; overlay or frame. The
  frame borders all four edges: none / thin 2.5 % / medium 5 % (default) / thick 10 % of the short edge (D013),
  text in the bottom or top band.
- Place name (D011): off by default; full address, street and number, street and city, or city, above the
  coordinates and in XMP and the message. Looked up once through Android's Geocoder just before stamping
  (650 ms on the emulator, Google answering); offline it fails in about 0.3 s and the stamp keeps coordinates only.
  Google gives Prague no locality; the city is then read from its address line, "Praha 1-Staré Město" (D014),
  and the region is the last fallback. An approximate fix prints only ≈city.
  "Coordinates on the stamp" off (D012): only the place name prints; with no place found the coordinates
  print as before. GPS metadata is kept either way. Seen on the emulator online and offline.
- Default exposure: Settings sets the EV the camera opens at (−2 to +2); tapping the EV label returns to it.
- Location marker: green precise, amber approximate/old/searching (pulses while searching), gray and crossed
  out when denied or off. Tapping it asks for permission or opens settings. Denied → no stamp line, no GPS tags.
- Metadata: EXIF (time with offset, GPS, accuracy, ASCII description) and XMP with every field; Czech text
  round-trips. The stamp text is the message when the setting is on (default).
- Wait for precise location (D008, D010): the photo is taken at once and waits unstamped in review with a live
  readout of the current fix and accuracy; Retake, Use current, or a precise fix stamps it. Sharing appears only
  once stamped. The waiting photo is kept on disk and comes back after `am force-stop` with its capture time;
  switching location off or denying it keeps it waiting and says so.
- Sharing: chooser verified; quick buttons hand the photo to the messenger's package with `ACTION_SEND`.
- Camera controls: tap-to-focus and exposure seen working; pinch zoom not testable through adb.
- Tests: 146/146 unit, 39/39 instrumented (shoot → review → share; the settings slider and segments; deferred location through mock providers, including
  restart; square crop; the real geocoder, skipped offline; message, one-copy rule, stamped-photo restore, redraw, version reuse and its restore; the note row: redraw on Done,
  switch, message following, send while typing, restart; compact fields: tap, Done, focus leaving, setting change). The emulator camera currently runs at about
  1 frame/s, so `DeferredLocationTest` cases take 47–215 s each; they pass.
- Release build 0.3.1 (code 7): signed with the D007 key, 1.60 MB, sent to the Owner. Settings, the colour dialog
  and the review screen with brand-coloured quick buttons open in it (custom Preference classes survive R8).
  `scripts/camera-release-check.sh` on 0.3.0: 10/10 with `WAIT=6` twice; at the default 3 s wait the "screen off"
  step can fail, because camera close on this emulator takes 2–5 s.
- Startup to a streaming preview (last measured on 0.3.0, alternated with 0.2.2, 10 cold starts each): 0.3.0
  1.9–2.9 s, 0.2.2 2.2–3.3 s, so no regression. Both are slower than G-001's 0.55–0.89 s because the emulator is.

On a real phone the Owner tested 0.3.1 and approved it (2026-11-04). Not measured by us there: the place-name
format Google returns, each messenger's message handling, pinch zoom, startup times.

## How things fit together

- `CameraActivity` is the only screen on the hot path, with two states (live camera, review) in one layout,
  so no Activity launch sits between the shutter and sending. `review/ReviewScreen` owns the review half
  (Restoring / Waiting / Stamping / Ready, a generation counter drops stamps for abandoned photos);
  `PendingLocation` holds its pure rules, `ShareButtons` the quick-button layout rule, and `PendingStore` the on-disk
  copy in `filesDir/pending`: a waiting photo, or a stamped one with its source JPEG and ready entry (`review/ReadyMeta`), never both;
  `location/PlaceLookup` the geocoder call made inside the stamp step.
  Settings is a separate AppCompat activity: `settings/ui/` holds one `Preference` subclass per control on the
  shared `pref_widget_row` layout, and `CardDecoration` draws each category as a card (D021). `review/ReviewPalette`
  colours the review controls in the app theme each time a photo enters review (D022).
- `camera/CameraSession` owns CameraX: open in `onStart`, close in `onStop` and after every capture (D003).
  `camera/CropGuideView` draws the crop over the preview with the same `photo/AspectCrop` math the pipeline uses.
- `location/`: `LocationTracker` (platform LocationManager, started/stopped with the activity) and the pure
  `FixPolicy` and `LocationMarker` rules (D005). `LocationPermissions` decides prompt vs. settings.
- `stamp/` is the extensible stamp (D004). Only `StampRenderer.kt` touches Android graphics; `StampLayout`
  places the photo, border and band.
- `photo/PhotoPipeline` works in two steps off the main thread: `decode` (rotate, crop) after capture, `finish`
  (stamp, encode into `cacheDir/shared/`) once the stamp data is final;
  `ExifWriter` then adds `PhotoMetadata` (D006); `GallerySaver` copies to MediaStore. `review/PhotoVersions` draws
  or reuses a version by its `PhotoPipeline.fileName` (D025); `ReviewTexts`, `SchemeButtons` and `QuickButtons` own
  their rows of the review controls, and `ReviewState` the states.
- Both activities call `enableEdgeToEdge` on every Android version (15+ enforces it) and place their bars
  from window insets; Settings has its own toolbar in `activity_settings.xml` for that reason.
- `stamp/StampScheme` holds the scheme colours, palettes and defaults; `AppSettings.withScheme` swaps one in.
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
- A stamped, unsent photo survives a kill with its texts and switches; Retake drops it (Owner, D015).
- Going back to an earlier look must not make a new version: everything drawn goes into the file-name hash (Owner, D025).
- At most one gallery copy per photo; a retake without sending leaves none (Owner, D017).
- XMP must stay ASCII with character references: ExifInterface corrupts non-ASCII XMP (measured, D006).
- Do not export `MSYS_NO_PATHCONV` when running `gradlew` in Git Bash: the wrapper then fails to start
  (measured); set it only for `adb` commands.

## Next steps and open questions

- G-004 M3: the frame stamp in two columns, place name and coordinates on the right. M4: release 0.4.0 (code 8).
- D009 reads the aspect ratio as following how the phone is held; the Owner may want it fixed instead.
- Back up the release key folder `.android-keys` in the Windows user profile: updates over 0.1.0 need it (D007).

## Deploy log

| Date | Commit | What changed | How verified |
|---|---|---|---|
| 2026-11-04 | 0731ad0 | APK 0.3.1 sent to the Owner: G-003 M5 phone-test fixes (brand-coloured quick buttons, icon-only single button, Owner palettes) | `apksigner verify`; release Settings, dialog and review on the emulator; unit 141/141, instrumented 31/31 |
| 2026-11-04 | a089e6e | APK 0.3.0 sent to the Owner: G-003 (review screen, stamp schemes, settings redesign, app theme) | `apksigner verify`; release check 10/10 twice; startup vs 0.2.2; unit 135/135, instrumented 30/30 |
| 2026-11-03 | 2ebdd63 | Repo pushed public to github.com/yunniko/report-cam; APK 0.2.2 sent: city from the address line (D014) | secrets scan of the history; `apksigner verify`; unit 103/103 |
| 2026-11-03 | d534cc7 | APK 0.2.1 sent to the Owner: border scale (D013), coordinates switch (D012) | `apksigner verify`; release launch on the emulator; unit 100/100, instrumented 18/18 |
| 2026-11-03 | 3e2586f | APK 0.2.0 sent to the Owner: G-002 (shape, frame border, default EV, place name, deferred location) | `apksigner verify`; release check and startup on the emulator |
| 2026-11-03 | 6eddd7f | APK 0.1.1 sent to the Owner: Settings toolbar no longer covers rows on Android 15+ | emulator screenshots, debug and release |
| 2026-11-03 | bcdd34e | APK 0.1.0 sent to the Owner on Telegram for a phone test | `apksigner verify`; release run on the emulator |

## Decisions

See `docs/decisions/README.md`.
