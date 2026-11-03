# report-cam

A small Android camera for photo reports. Open the app, take a photo, and it is stamped with the
date and time, your location and a text set in Settings. Send it straight to WhatsApp or Telegram.
Nothing runs in the background, and the camera is open only while the live preview is on screen.

- Runs on Android 8.0 (API 26) to Android 17. Native Kotlin, CameraX, framework Views.
- Goals: `GOALS.md`. Project state and how it fits together: `HANDOVER.md`.

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

- `ic_settings.xml`: Material Icons "settings", Google, Apache License 2.0.
- The launcher icon is original artwork.
