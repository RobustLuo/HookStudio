#!/bin/zsh

set -u

SCRIPT_DIR="${0:A:h}"
cd "$SCRIPT_DIR" || exit 1

if ! command -v fswatch >/dev/null 2>&1; then
  echo "缺少 fswatch，请先运行：brew install fswatch"
  read -r "?按回车键退出..."
  exit 1
fi

APP_PID=""

stop_app() {
  if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID" 2>/dev/null
    wait "$APP_PID" 2>/dev/null
  fi
  osascript -e 'tell application "HookStudio" to quit' >/dev/null 2>&1 || true
  APP_PID=""
}

start_app() {
  echo ""
  echo "[$(date '+%H:%M:%S')] 正在构建 HookStudio.app..."
  if ! "$SCRIPT_DIR/package-macos.command"; then
    echo "[$(date '+%H:%M:%S')] 构建失败，修复代码后会自动重试。"
    return 1
  fi

  stop_app
  open "$SCRIPT_DIR/build/macos/HookStudio.app"
  APP_PID=""
  echo "[$(date '+%H:%M:%S')] HookStudio 已启动，正在监听代码改动。"
}

cleanup() {
  stop_app
}

trap cleanup EXIT INT TERM

start_app

while read -r _; do
  start_app
done < <(fswatch -o -l 0.5 \
  "$SCRIPT_DIR/src" \
  "$SCRIPT_DIR/build.gradle.kts" \
  "$SCRIPT_DIR/settings.gradle.kts" \
  "$SCRIPT_DIR/package-macos.command")
