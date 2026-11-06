#!/usr/bin/env bash
# Release-readiness check for a Google Play upload (G-006 M1). Builds the signed App Bundle and checks what Play
# would refuse or flag: lintVitalRelease clean, the bundle signed with the release key, nothing test-only inside,
# every native library 16 KB aligned. Prints the version the bundle carries.
# Usage: scripts/play-release-check.sh   (Git Bash on Windows or a Linux shell; needs keystore.properties (D007),
# the Android SDK from local.properties with an NDK for llvm-readelf and build-tools 36 for dexdump, and a JDK.)
set -u
cd "$(dirname "$0")/.."
fail=0
check() { if [ "$1" -eq 0 ]; then echo "PASS  $2"; else echo "FAIL  $2"; fail=1; fi; }

SDK=${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties)}
READELF=$(ls "$SDK"/ndk/*/toolchains/llvm/prebuilt/*/bin/llvm-readelf* 2>/dev/null | tail -1)
DEXDUMP=$(ls "$SDK"/build-tools/36.*/dexdump* 2>/dev/null | tail -1)
JARSIGNER=""
for c in "${JAVA_HOME:-/nonexistent}/bin/jarsigner" "/c/Program Files/Android/Android Studio/jbr/bin/jarsigner.exe" "$(command -v jarsigner || true)"; do
    [ -n "$c" ] && [ -x "$c" ] && { JARSIGNER=$c; break; }
done
for t in READELF DEXDUMP JARSIGNER; do
    [ -n "${!t}" ] || { echo "FAIL  tool $t not found"; exit 1; }
done

# 1. Build: release lint (the checks that block a Play upload) and the signed bundle. MSYS_NO_PATHCONV must stay
#    unset here, or the Gradle wrapper fails to start under Git Bash.
./gradlew -q :app:lintVitalRelease :app:bundleRelease
check $? "lintVitalRelease clean and bundleRelease built"
AAB=app/build/outputs/bundle/release/app-release.aab
[ -f "$AAB" ] || { echo "FAIL  $AAB missing"; exit 1; }

# 2. Signed with the release key (D007), so Play App Signing can take it as the upload key.
"$JARSIGNER" -verify -verbose -certs "$AAB" 2>/dev/null | grep -q "CN=Report Cam"
check $? "bundle signed with the Report Cam key"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
# The bundle is a zip. Git Bash's tar is GNU tar and does not read zip; Windows' own tar (bsdtar) does.
if command -v unzip >/dev/null; then unzip -q "$AAB" -d "$TMP"
elif [ -x /c/Windows/System32/tar.exe ]; then /c/Windows/System32/tar.exe -xf "$AAB" -C "$TMP"
else python3 -m zipfile -e "$AAB" "$TMP"; fi
DEX=$TMP/base/dex/classes.dex; MANIFEST=$TMP/base/manifest/AndroidManifest.xml
[ -f "$DEX" ] && [ -f "$MANIFEST" ] || { echo "FAIL  bundle not extracted (no base/dex or base/manifest)"; exit 1; }

# 3. Nothing test-only ships: no test runner, no instrumentation, no stand-in messenger in the dex or the manifest.
"$DEXDUMP" "$DEX" > "$TMP/dex.txt" 2>/dev/null
classes=$(grep -c "Class descriptor" "$TMP/dex.txt")
grep "Class descriptor" "$TMP/dex.txt" | grep -Ei "androidx/test|org/junit|junit/|espresso|uiautomator|stubmessenger|stub_messenger" > "$TMP/test-classes.txt"
[ "$classes" -gt 0 ] && [ ! -s "$TMP/test-classes.txt" ]
check $? "no test classes among the $classes in the dex ($(wc -l < "$TMP/test-classes.txt") found)"
! grep -aqi "androidx.test\|AndroidJUnitRunner" "$MANIFEST"
check $? "no instrumentation in the manifest"

# 4. Every native library 16 KB aligned (Play requires it for new apps; Android 15+ devices may use 16 KB pages).
bad=0; n=0
for so in "$TMP"/base/lib/*/*.so; do
    [ -f "$so" ] || continue
    n=$((n + 1))
    loads=0
    while read -r align; do
        loads=$((loads + 1))
        [ $((align)) -ge 16384 ] || { bad=1; echo "      $(basename "$(dirname "$so")")/$(basename "$so") LOAD align $align"; }
    done < <("$READELF" -l "$so" | awk '$1 == "LOAD" { print $NF }')
    [ "$loads" -gt 0 ] || { bad=1; echo "      $(basename "$so"): no LOAD segment read"; }
done
[ "$bad" -eq 0 ] && [ "$n" -gt 0 ]; check $? "$n native libraries 16 KB aligned"

# 5. What the bundle carries, for the record.
echo "INFO  version $(grep -ao '[0-9]\+\.[0-9]\+\.[0-9]\+' "$MANIFEST" | head -1), $(stat -c %s "$AAB") bytes, ABIs: $(ls "$TMP/base/lib" | tr '\n' ' ')"
exit $fail
