# report-cam

A small Android camera for photo reports. Open the app, take a photo, and it is stamped with the
date and time, your location and a text set in Settings. Send it straight to WhatsApp or Telegram.
Nothing runs in the background, and the camera is open only while the live preview is on screen.

- Runs on Android 8.0 (API 26) to Android 17. Native Kotlin, CameraX, framework Views.
- Settings: stamp text, date format, location on/off and format, place name (street address through Android's
  Geocoder, off by default) and whether coordinates print beside it, wait for precise location, stamp position,
  frame border width, size and colours, photo shape and size (small by default), default exposure, gallery
  copy, and sending the stamp text as the caption.
- The marker at top left shows the location state; tap it to allow location. Tap the preview to focus,
  pinch to zoom, use the slider for exposure.
- Stamp data is also stored in the file's EXIF and XMP metadata.
- Project state and how it fits together: `HANDOVER.md`.

## Build and run

Needs JDK 17+ and the Android SDK (platform 36). Set `sdk.dir` in `local.properties`.

```
gradlew assembleDebug            # app/build/outputs/apk/debug/app-debug.apk
gradlew testDebugUnitTest        # unit tests
gradlew connectedDebugAndroidTest  # instrumented tests (device or emulator)
gradlew ktlintCheck              # formatting (ktlintFormat to fix)
scripts/camera-release-check.sh  # camera held only while previewing (adb, Git Bash)
```

## Style

UI: black camera screen, white controls, accent `#F2A33A`, primary `#1F5F8B`. The stamp style is
configurable in Settings.

## Third-party assets

- `ic_settings.xml`, `ic_location_on.xml`, `ic_location_off.xml`: Material Icons, Google, Apache License 2.0.
- The launcher icon is original artwork.
