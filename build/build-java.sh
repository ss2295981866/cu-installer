#!/bin/bash
# 从 Java 源码构建 未知伤亡管理器
# 需要：JDK 17+、R8/d8、zipalign、apksigner
# 环境变量：ANDROID_JAR（android.jar 路径）、R8_JAR（r8.jar 路径，含 d8）
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT="$ROOT/build/out"
ANDROID_JAR=${ANDROID_JAR:-$HOME/android-platforms/android-30/android.jar}
R8_JAR=${R8_JAR:-$HOME/r8.jar}
KS=${KS:-$ROOT/build/release.jks}
KSPASS=${KSPASS:-changeit}
ALIAS=${ALIAS:-cuinstaller}
LIBS="$ROOT/libs/junrar-7.5.5.jar:$ROOT/libs/slf4j-api-1.7.36.jar:$ROOT/libs/commons-compress-1.21.jar:$ROOT/libs/xz-1.9.jar"

mkdir -p "$OUT/classes" "$OUT/dex"

echo "[1/4] javac"
javac --release 8 -cp "$ANDROID_JAR:$LIBS" -d "$OUT/classes" "$ROOT"/src/java/com/rblh/cu/*.java
(cd "$OUT/classes" && jar cf "$OUT/classes.jar" .)

echo "[2/4] d8"
java -cp "$R8_JAR" com.android.tools.r8.D8 --min-api 28 --lib "$ANDROID_JAR" \
  --output "$OUT/dex" "$OUT/classes.jar" "$ROOT/libs/junrar-7.5.5.jar" "$ROOT/libs/slf4j-api-1.7.36.jar" "$ROOT/libs/commons-compress-1.21.jar" "$ROOT/libs/xz-1.9.jar"

echo "[3/4] repack"
python3 "$ROOT/build/repack.py" "$ROOT/base" "$OUT/dex/classes.dex" "$OUT/app-unsigned.apk"
zipalign -f 4 "$OUT/app-unsigned.apk" "$OUT/app-aligned.apk"

echo "[4/4] sign"
if [ ! -f "$KS" ]; then
  echo "缺少签名密钥：$KS"
  echo "keytool -genkeypair -keystore $KS -alias $ALIAS -keyalg RSA -keysize 2048 -validity 10000 -storepass $KSPASS -keypass $KSPASS -dname 'CN=CuInstaller'"
  exit 1
fi
apksigner sign --ks "$KS" --ks-key-alias "$ALIAS" \
  --ks-pass "pass:$KSPASS" --key-pass "pass:$KSPASS" \
  --out "$OUT/cuinstaller.apk" "$OUT/app-aligned.apk"
echo "完成：$OUT/cuinstaller.apk"
