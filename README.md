# report-cam

A small Android camera for photo reports. Open the app, take a photo, and it is stamped with the
date and time, your location and a text set in Settings. Edit the caption if you like, then send it with
one tap to up to three messengers of your choice, or through the system Share sheet.
Nothing runs in the background, and the camera is open only while the live preview is on screen.

- Runs on Android 8.0 (API 26) to Android 17. Native Kotlin, CameraX, framework Views.
- Settings: stamp text, date format, location on/off and format, place name (street address through Android's
  Geocoder, off by default) and whether coordinates print beside it, wait for precise location, stamp position,
  frame border width, size and colours, photo shape and size (small by default), default exposure, saving to
  the gallery when sent, the caption switch's starting state, and the quick send buttons (WhatsApp, Telegram,
  Messenger, Discord, Slack, Snapchat, Instagram; only installed ones show).
- The marker at top left shows the location state; tap it to allow location. Tap the preview to focus,
  pinch to zoom, use the slider for exposure.
- A stamped photo not yet sent comes back after the app is closed, with its caption.
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
gradlew :stub-messenger:assembleDebug  # stand-in messengers for emulator tests, see stub-messenger/README.md
```

## Style

UI: black camera screen, white controls, accent `#F2A33A`, primary `#1F5F8B`. The stamp style is
configurable in Settings.

## Third-party assets

- `ic_settings.xml`, `ic_location_on.xml`, `ic_location_off.xml`: Material Icons, Google, Apache License 2.0.
- `ic_msg_*.xml` (messenger logos): Boxicons brand set (https://icon-sets.iconify.design/bxl/), MIT License.
  The logos are trademarks of their owners and are used only to label the send buttons.
- The launcher icon is original artwork.
