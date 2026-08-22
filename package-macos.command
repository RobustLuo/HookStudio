#!/bin/zsh

set -euo pipefail

SCRIPT_DIR="${0:A:h}"
cd "$SCRIPT_DIR"

ICON_SOURCE="$SCRIPT_DIR/src/main/resources/hookstudio-icon.png"
ICONSET_DIR="$SCRIPT_DIR/build/macos/HookStudio.iconset"
ICON_FILE="$SCRIPT_DIR/build/macos/HookStudio.icns"
APP_DIR="$SCRIPT_DIR/build/macos/HookStudio.app"
VERSION=$(tr -d '[:space:]' < "$SCRIPT_DIR/VERSION")
INSTALL_DIR="$HOME/Applications"
INSTALLED_APP="$INSTALL_DIR/HookStudio.app"
DESKTOP_SHORTCUT="$HOME/Desktop/HookStudio.app"

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
  --app-version "$VERSION" \
  --vendor HookStudio \
  --input "$SCRIPT_DIR/build/install/HookStudio/lib" \
  --main-jar HookStudio-0.1.0.jar \
  --main-class dev.hookstudio.MainKt \
  --icon "$ICON_FILE" \
  --dest "$SCRIPT_DIR/build/macos" \
  --java-options "-Dapple.awt.application.name=HookStudio"

echo "正在安装到用户应用程序目录..."
mkdir -p "$INSTALL_DIR"
rm -rf "$INSTALLED_APP"
ditto "$APP_DIR" "$INSTALLED_APP"
rm -f "$DESKTOP_SHORTCUT"
ln -s "$INSTALLED_APP" "$DESKTOP_SHORTCUT"

echo "已生成：$APP_DIR"
echo "已安装：$INSTALLED_APP"
echo "桌面快捷方式：$DESKTOP_SHORTCUT"
open "$INSTALLED_APP"
