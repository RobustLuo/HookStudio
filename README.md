# HookStudio

## 普通启动

```bash
gradle run
```

### Windows

安装 JDK 17、Gradle、Android Platform Tools（ADB）和 JADX，并确保它们在 `PATH` 中。然后双击 `run-windows.bat`，或执行：

```bat
gradle run
```

生成带图标的 Windows 应用目录：

```bat
package-windows.bat
```

生成结果位于 `build\\windows\\HookStudio\\HookStudio.exe`。

## macOS 应用包

生成带有 HookStudio 图标的 macOS 应用：

```bash
./package-macos.command
open build/macos/HookStudio.app
```

## 开发模式（自动更新）

双击 `dev-run.command`，或者在终端执行：

```bash
./dev-run.command
```

开发模式会监控 `src`、`build.gradle.kts` 和 `settings.gradle.kts`。文件发生变化后，程序会自动重新构建并重启，无需手动关闭和再次运行。

由于项目使用 Swing，界面无法像网页一样原地热刷新；自动重启是 JVM 桌面应用开发中更稳定的更新方式。

## 导出给 AI 分析

选择手机 App 并完成 APK 读取后，点击“JADX 反编译”，再点击“导出 AI 分析文件”。
软件会在桌面生成：

```text
~/Desktop/HookStudio-AI/<包名-时间>/
```

其中包含完整反编译源码、JADX 资源、原始 APK、文件索引、分析元数据和 `AI_PROMPT.md`。把这个目录交给 AI，让它先定位可 Hook 的类和方法，再生成独立的 LSPosed 模块。

## 开源发布

项目使用 MIT License。源码仓库不包含构建目录、APK 或本地分析结果；这些内容由 `.gitignore` 排除。
