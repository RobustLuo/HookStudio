@echo off
setlocal

cd /d "%~dp0"

echo Building HookStudio...
call gradle --quiet installDist
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)

if not exist "build\windows" mkdir "build\windows"
if exist "build\windows\HookStudio" rmdir /s /q "build\windows\HookStudio"

jpackage ^
  --type app-image ^
  --name HookStudio ^
  --app-version 1.0.0 ^
  --vendor HookStudio ^
  --input "build\install\HookStudio\lib" ^
  --main-jar HookStudio-0.1.0.jar ^
  --main-class dev.hookstudio.MainKt ^
  --icon "src\main\resources\hookstudio-icon.ico" ^
  --dest "build\windows"

if errorlevel 1 (
  echo jpackage failed. Please confirm JDK 17 and WiX are installed.
  exit /b 1
)

echo Created: build\windows\HookStudio
echo Launch: build\windows\HookStudio\HookStudio.exe
endlocal
