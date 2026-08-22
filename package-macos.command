#!/bin/zsh

set -euo pipefail

SCRIPT_DIR="${0:A:h}"
cd "$SCRIPT_DIR"

ICON_SOURCE="$SCRIPT_DIR/src/main/resources/hookstudio-icon.png"
ICONSET_DIR="$SCRIPT_DIR/build/macos/HookStudio.iconset"
ICON_FILE="$SCRIPT_DIR/build/macos/HookStudio.icns"
APP_DIR="$SCRIPT_DIR/build/macos/HookStudio.app"

echo "正在构建 HookStudio..."
gradle --quiet installDist

mkdir -p "$SCRIPT_DIR/build/macos"
rm -rf "$ICONSET_DIR"
rm -f "$ICON_FILE"
mkdir -p "$ICONSET_DIR"

for size in 16 32 128 256 512; do
  sips -z "$size" "$size" "$ICON_SOURCE" --out "$ICONSET_DIR/icon_${size}x${size}.png" >/dev/null
  double=$((size * 2))
  sips -z "$double" "$double" "$ICON_SOURCE" --out "$ICONSET_DIR/icon_${size}x${size}@2x.png" >/dev/null
done

iconutil -c icns "$ICONSET_DIR" -o "$ICON_FILE"
rm -rf "$ICONSET_DIR"
rm -rf "$APP_DIR"

jpackage \
  --type app-image \
  --name HookStudio \
  --app-version 1.0.0 \
  --vendor HookStudio \
  --input "$SCRIPT_DIR/build/install/HookStudio/lib" \
  --main-jar HookStudio-0.1.0.jar \
  --main-class dev.hookstudio.MainKt \
  --icon "$ICON_FILE" \
  --dest "$SCRIPT_DIR/build/macos" \
  --java-options "-Dapple.awt.application.name=HookStudio"

echo "已生成：$APP_DIR"
echo "启动命令：open \"$APP_DIR\""
