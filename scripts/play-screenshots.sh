#!/usr/bin/env bash
# Takes the Google Play phone screenshots (1080 x 2220) on the Pixel 3a API 34 emulator (G-006 M3).
# Output: docs/play/screenshots/NN-<name>.png. Run from Git Bash with the debug build installed and the
# WhatsApp and Telegram stand-ins from stub-messenger/ installed, the emulator online.
#
# The emulator's camera is a test pattern, so the photo under review is seeded: the Owner's own photo
# (app/src/main/res/drawable-nodpi/preview_photo.jpg, Český Krumlov) is written into the app's waiting
# entry (review/PendingStore) and the real pipeline stamps it on start, with the emulator's location set to
# Český Krumlov so the stamp's place name matches the picture. The live camera screen is not taken here:
# that one comes from a phone.
set -euo pipefail
cd "$(dirname "$0")/.."
export MSYS_NO_PATHCONV=1
ADB=${ADB:-"$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"}
PKG=cz.julienika.reportcam
OUT=docs/play/screenshots
TMP=/data/local/tmp/play-shots
mkdir -p "$OUT"
"$ADB" shell mkdir -p "$TMP"

shot() { # <file>: the screen as it is
  "$ADB" shell screencap -p /sdcard/play-shot.png
  "$ADB" pull /sdcard/play-shot.png "$OUT/$1" >/dev/null
  echo "  $OUT/$1"
}

prefs() { # <theme> <placement> <wait_precise>: the app's settings for the next start, written while it is stopped
  cat > "$OUT/.prefs.xml" <<EOF
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="stamp_text">Roof inspection, hall B</string>
    <string name="app_theme">$1</string>
    <string name="placement">$2</string>
    <boolean name="wait_precise" value="$3" />
    <string name="place_format">street_city</string>
    <boolean name="show_coordinates" value="true" />
    <boolean name="show_location" value="true" />
    <boolean name="show_datetime" value="true" />
    <boolean name="show_weekday" value="true" />
    <string name="frame_border">medium</string>
    <string name="stamp_scheme">dark</string>
    <string name="output_size">1280</string>
    <string name="aspect_ratio">full</string>
    <boolean name="caption" value="true" />
    <boolean name="save_gallery" value="false" />
    <set name="quick_messengers">
        <string>whatsapp</string>
        <string>telegram</string>
    </set>
</map>
EOF
  "$ADB" push "$OUT/.prefs.xml" "$TMP/prefs.xml" >/dev/null
  # The remote shell needs its own quoting: adb passes one string on.
  "$ADB" shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cp $TMP/prefs.xml shared_prefs/${PKG}_preferences.xml'"
  rm "$OUT/.prefs.xml"
}

seed_photo() { # the Owner's photo as a waiting entry, taken now
  "$ADB" push app/src/main/res/drawable-nodpi/preview_photo.jpg "$TMP/photo.jpg" >/dev/null
  local now
  now=$(date +%Y-%m-%dT%H:%M:%S)+01:00[Europe/Prague]
  printf 'capture_time=%s\nuser_text=Roof inspection, hall B\nrotation=0\nlong_edge=1280\naspect=full\n' "$now" > "$OUT/.meta.properties"
  "$ADB" push "$OUT/.meta.properties" "$TMP/meta.properties" >/dev/null
  rm "$OUT/.meta.properties"
  "$ADB" shell "run-as $PKG sh -c 'rm -rf files/pending && mkdir -p files/pending && cp $TMP/photo.jpg files/pending/photo.jpg && cp $TMP/meta.properties files/pending/meta.properties'"
}

clear_photo() { "$ADB" shell "run-as $PKG rm -rf files/pending"; }

start() { # <seconds to wait>: the location first, so a restored photo is stamped with it and not an older fix
  "$ADB" emu geo fix 14.3086 48.8127 >/dev/null   # Český Krumlov
  sleep 2
  "$ADB" shell am start -n $PKG/.CameraActivity >/dev/null
  sleep 2
  "$ADB" emu geo fix 14.3086 48.8127 >/dev/null
  sleep "$1"
}

open_settings() { # the gear; Settings can take several seconds to appear on the emulator while the camera closes
  local tries
  for tries in 1 2 3; do
    "$ADB" shell input tap 992 132
    for _ in 1 2 3 4 5 6; do
      sleep 2
      # A dump mid-transition fails and leaves the old file, so it is removed first.
      "$ADB" shell rm -f /sdcard/play-ui.xml
      "$ADB" shell uiautomator dump /sdcard/play-ui.xml >/dev/null 2>&1 || true
      if "$ADB" shell cat /sdcard/play-ui.xml 2>/dev/null | grep -q 'text="Settings"'; then sleep 1; return; fi
    done
  done
  echo "Settings did not open" >&2; exit 1
}

stop() { "$ADB" shell am force-stop $PKG; sleep 1; }

tap_node() { # <attribute> <value>: scrolls the list until the node is on screen, then taps its centre
  local attr=$1 value=$2 b cy
  for _ in 1 2 3 4 5 6 7 8; do
    "$ADB" shell uiautomator dump /sdcard/play-ui.xml >/dev/null
    b=$("$ADB" shell cat /sdcard/play-ui.xml | tr '>' '\n' | grep "$attr=\"$value\"" | grep -o 'bounds="[^"]*"' | head -1 | grep -o '[0-9]\+' | tr '\n' ' ' || true)
    if [ -n "$b" ]; then
      set -- $b
      cy=$(( ($2 + $4) / 2 ))
      if [ "$cy" -gt 300 ] && [ "$cy" -lt 2000 ]; then
        "$ADB" shell input tap $(( ($1 + $3) / 2 )) "$cy"
        sleep 2
        return
      fi
    fi
    "$ADB" shell input swipe 540 1600 540 800 300
    sleep 1
  done
  echo "no node with $attr=$value" >&2; exit 1
}

"$ADB" shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
"$ADB" shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
"$ADB" shell pm grant $PKG android.permission.CAMERA

ONLY=${1:-all}   # all | review | settings
if [ "$ONLY" = all ] || [ "$ONLY" = review ]; then
echo "review, light theme"
stop; prefs light overlay_bottom_start false; seed_photo; start 12; shot 01-review-light.png

echo "review, dark theme"
stop; prefs dark overlay_bottom_start false; seed_photo; start 12; shot 02-review-dark.png

echo "frame stamp, light theme"
stop; prefs light frame_bottom false; seed_photo; start 12; shot 03-frame-light.png

echo "waiting for a precise location (fine location withheld)"
stop; prefs light overlay_bottom_start true; seed_photo
"$ADB" shell pm revoke $PKG android.permission.ACCESS_FINE_LOCATION
start 8; shot 04-waiting-location.png
stop; "$ADB" shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
fi

if [ "$ONLY" = all ] || [ "$ONLY" = settings ]; then
echo "settings, light and dark"
stop; clear_photo; prefs light overlay_bottom_start false; start 8
open_settings; shot 05-settings-light.png
tap_node content-desc "Dark stamp preview"; shot 06-scheme-dialog.png
"$ADB" shell input keyevent BACK; sleep 1
stop; prefs dark overlay_bottom_start false; start 8
open_settings; shot 07-settings-dark.png
stop
fi

"$ADB" shell rm -rf "$TMP" /sdcard/play-shot.png /sdcard/play-ui.xml
echo "done: $(ls "$OUT" | wc -l) files in $OUT"
