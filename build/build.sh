#!/bin/bash
# 构建 未知伤亡管理器（smali 版源码）
# 依赖：JDK 17+、libsmali-java（smali/baksmali）、zipalign、apksigner、python3
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT="$ROOT/build/out"
KS=${KS:-$ROOT/build/release.jks}
KSPASS=${KSPASS:-changeit}
ALIAS=${ALIAS:-cuinstaller}

mkdir -p "$OUT"

echo "[1/4] smali -> classes.dex"
smali a "$ROOT/src/smali" -o "$OUT/classes.dex"

echo "[2/4] 打包（base 资源 + classes.dex）"
python3 "$ROOT/build/repack.py" "$ROOT/base" "$OUT/classes.dex" "$OUT/app-unsigned.apk"

echo "[3/4] zipalign"
zipalign -f 4 "$OUT/app-unsigned.apk" "$OUT/app-aligned.apk"

if [ ! -f "$KS" ]; then
  echo "!! 没有签名密钥：$KS"
  echo "   先生成一把："
  echo "   keytool -genkeypair -keystore $KS -alias $ALIAS -keyalg RSA -keysize 2048 -validity 10000 -storepass $KSPASS -keypass $KSPASS -dname 'CN=CuInstaller'"
  exit 1
fi

echo "[4/4] 签名"
apksigner sign --ks "$KS" --ks-key-alias "$ALIAS" \
  --ks-pass "pass:$KSPASS" --key-pass "pass:$KSPASS" \
  --out "$OUT/cuinstaller.apk" "$OUT/app-aligned.apk"
apksigner verify --print-certs "$OUT/cuinstaller.apk" | head -3
echo "完成：$OUT/cuinstaller.apk"
