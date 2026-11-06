#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17 or newer}"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
BUILD_TOOLS="${BUILD_TOOLS:-$ANDROID_SDK_ROOT/build-tools/35.0.0}"
PLATFORM="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
OUT="$PWD/app/build/manual"
mkdir -p "$OUT/classes" "$OUT/tests" "$OUT/dex" "$OUT/apk"
export PATH="$JAVA_HOME/bin:$PATH"
javac -encoding UTF-8 -d "$OUT/tests" app/src/main/java/ru/tileclock/TurnTimer.java app/src/main/java/ru/tileclock/GameLayoutPlan.java app/src/main/java/ru/tileclock/SetupLayoutPlan.java app/src/main/java/ru/tileclock/PlayerOrder.java tests/*.java
java -cp "$OUT/tests" TurnTimerTest
java -cp "$OUT/tests" GameLayoutTest
java -cp "$OUT/tests" SetupLayoutTest
java -cp "$OUT/tests" PlayerOrderTest
javac --release 17 -encoding UTF-8 -classpath "$PLATFORM" -d "$OUT/classes" app/src/main/java/ru/tileclock/*.java
jar --create --file "$OUT/classes.jar" -C "$OUT/classes" .
"$BUILD_TOOLS/d8" --lib "$PLATFORM" --min-api 26 --output "$OUT/dex" "$OUT/classes.jar"
"$BUILD_TOOLS/aapt2" compile --dir app/src/main/res -o "$OUT/resources.zip"
python3 - "$OUT/AndroidManifest.xml" <<'PY'
import sys
from pathlib import Path
s=Path('app/src/main/AndroidManifest.xml').read_text()
s=s.replace('<manifest ', '<manifest package="ru.tileclock" ', 1)
Path(sys.argv[1]).write_text(s)
PY
"$BUILD_TOOLS/aapt2" link -o "$OUT/apk/unsigned.apk" --manifest "$OUT/AndroidManifest.xml" -I "$PLATFORM" --min-sdk-version 26 --target-sdk-version 35 --version-code 5 --version-name 1.4 "$OUT/resources.zip"
python3 - "$OUT" <<'PY'
import sys, zipfile
from pathlib import Path
p=Path(sys.argv[1])
with zipfile.ZipFile(p/'apk/unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
    for f in (p/'dex').glob('*.dex'): z.write(f,f.name)
PY
"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT/apk/unsigned.apk" "$OUT/apk/aligned.apk"
# A development key for this first installable build. Keep it to update this APK.
KEYSTORE="${KEYSTORE:-$PWD/.local-signing/development.jks}"
if [ ! -f "$KEYSTORE" ]; then
    mkdir -p "$(dirname "$KEYSTORE")"
    keytool -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=TileClock Development,O=TileClock,C=RU'
    chmod 600 "$KEYSTORE"
fi
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$OUT/apk/rummikub-timer.apk" "$OUT/apk/aligned.apk"
"$BUILD_TOOLS/apksigner" verify --verbose "$OUT/apk/rummikub-timer.apk"
"$BUILD_TOOLS/aapt2" dump badging "$OUT/apk/rummikub-timer.apk"
echo "APK: $OUT/apk/rummikub-timer.apk"
