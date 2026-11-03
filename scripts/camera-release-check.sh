#!/usr/bin/env bash
# Checks on a connected device/emulator that Report Cam holds the camera only while its preview is visible.
# Usage: scripts/camera-release-check.sh   (app must be installed with permissions granted: adb install -g)
# On a slow emulator camera close can take 2-5 s; run with WAIT=6 there (G-002 M4 log).
set -u
export MSYS_NO_PATHCONV=1  # Git Bash on Windows would rewrite /sdcard paths
PKG=cz.julienika.reportcam
ACT=$PKG/.CameraActivity
fail=0

holds_camera() { adb shell dumpsys media.camera | grep -q "Client Package Name: $PKG"; }
expect() { # expect <open|closed> <label>
    sleep "${WAIT:-3}"
    if holds_camera; then state=open; else state=closed; fi
    if [ "$state" = "$1" ]; then echo "PASS  $2 (camera $state)"; else echo "FAIL  $2 (camera $state, expected $1)"; fail=1; fi
}
tap_id() { # tap the centre of the view with resource id $1
    local id=$1
    adb shell uiautomator dump /sdcard/rc-ui.xml >/dev/null
    b=$(adb shell cat /sdcard/rc-ui.xml | grep -o "resource-id=\"$PKG:id/$1\"[^>]*bounds=\"[^\"]*\"" | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+')
    set -- $b
    [ $# -eq 4 ] || { echo "FAIL  view $id not on screen"; fail=1; return; }
    adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
}

adb shell am force-stop $PKG
adb shell am start -n $ACT >/dev/null;                       expect open   "launch"
adb shell input keyevent KEYCODE_HOME;                       expect closed "home"
adb shell am start -n $ACT >/dev/null;                       expect open   "return from home"
adb shell input keyevent KEYCODE_SLEEP;                      expect closed "screen off"
adb shell input keyevent KEYCODE_WAKEUP; adb shell wm dismiss-keyguard; expect open "screen on"
tap_id shutter;                                              expect closed "reviewing a photo"
tap_id share_other;                                          expect closed "share sheet"
adb shell input keyevent KEYCODE_BACK;                       expect closed "back from share sheet to review"
tap_id retake;                                               expect open   "retake"
adb shell am force-stop $PKG;                                expect closed "process killed"
exit $fail
