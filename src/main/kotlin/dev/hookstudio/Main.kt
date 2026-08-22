package dev.hookstudio

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.LayoutManager
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder
import java.io.InputStream

private data class CommandResult(val exitCode: Int, val output: String)
private data class AppNames(val english: String = "", val chinese: String = "") {
    fun bestLabel(packageName: String): String = listOf(english, chinese)
        .firstOrNull { it.isNotBlank() }
        ?: packageName

    fun displayLabel(packageName: String): String = listOf(english, chinese)
        .filter { it.isNotBlank() }
        .distinct()
        .ifEmpty { listOf(packageName) }
        .joinToString(" / ")
}

private data class AppChoice(val label: String, val packageName: String) {
    override fun toString(): String = "$label（$packageName）"
}

private fun isWindows(): Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

private fun normalizeTool(command: String): String {
    if (!isWindows() || command.contains(File.separator) || command.contains('/')) return command
    return when (command) {
        "adb" -> "adb.exe"
        "jadx" -> "jadx.bat"
        else -> command
    }
}

private object GlassTheme {
    val ink = Color(239, 245, 255)
    val muted = Color(169, 184, 207)
    val accent = Color(132, 197, 255)
    val panel = Color(255, 255, 255, 34)
    val panelStrong = Color(255, 255, 255, 48)
    val border = Color(255, 255, 255, 92)
    val field = Color(8, 20, 43, 105)
}

private class LiquidGlassRootPanel : JPanel(BorderLayout()) {
    init { isOpaque = false }

    override fun paintComponent(graphics: Graphics) {
        val g = graphics.create() as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val width = width.toFloat()
        val height = height.toFloat()
        g.paint = GradientPaint(0f, 0f, Color(7, 17, 38), width, height, Color(19, 40, 78))
        g.fillRect(0, 0, width.toInt(), height.toInt())

        fun glow(x: Float, y: Float, radius: Float, color: Color) {
            g.paint = RadialGradientPaint(x, y, radius, floatArrayOf(0f, 0.62f, 1f), arrayOf(
                Color(color.red, color.green, color.blue, 92),
                Color(color.red, color.green, color.blue, 28),
                Color(color.red, color.green, color.blue, 0)
            ))
            g.fillOval((x - radius).toInt(), (y - radius).toInt(), (radius * 2).toInt(), (radius * 2).toInt())
        }
        glow(width * 0.12f, height * 0.08f, 330f, Color(44, 153, 255))
        glow(width * 0.88f, height * 0.18f, 300f, Color(151, 102, 255))
        glow(width * 0.52f, height * 1.05f, 420f, Color(38, 216, 190))
        g.dispose()
        super.paintComponent(graphics)
    }
}

private open class GlassPanel(layout: LayoutManager? = null) : JPanel(layout) {
    init {
        isOpaque = false
        border = EmptyBorder(16, 18, 16, 18)
    }

    override fun paintComponent(graphics: Graphics) {
        val g = graphics.create() as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val shape = RoundRectangle2D.Float(0f, 0f, width - 1f, height - 1f, 24f, 24f)
        g.color = Color(0, 0, 0, 35)
        g.fillRoundRect(0, 6, width, height, 24, 24)
        g.color = GlassTheme.panel
        g.fill(shape)
        g.color = GlassTheme.border
        g.draw(shape)
        g.dispose()
        super.paintComponent(graphics)
    }
}

private class GlassButton(text: String) : JButton(text) {
    init {
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        isFocusPainted = false
        foreground = GlassTheme.ink
        font = font.deriveFont(13f)
        border = EmptyBorder(9, 15, 9, 15)
        cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
    }

    override fun paintComponent(graphics: Graphics) {
        val g = graphics.create() as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val fill = if (model.isPressed) GlassTheme.accent else if (model.isRollover) GlassTheme.panelStrong else GlassTheme.panel
        g.color = fill
        g.fillRoundRect(0, 0, width - 1, height - 1, 15, 15)
        g.color = if (model.isRollover) Color(205, 233, 255, 180) else GlassTheme.border
        g.drawRoundRect(0, 0, width - 1, height - 1, 15, 15)
        g.dispose()
        super.paintComponent(graphics)
    }
}

private class GlassTextField(columns: Int) : JTextField(columns) {
    init {
        isOpaque = false
        foreground = GlassTheme.ink
        caretColor = GlassTheme.accent
        font = font.deriveFont(13f)
        border = EmptyBorder(9, 12, 9, 12)
    }

    override fun paintComponent(graphics: Graphics) {
        val g = graphics.create() as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = GlassTheme.field
        g.fillRoundRect(0, 0, width, height, 14, 14)
        g.color = GlassTheme.border
        g.drawRoundRect(0, 0, width - 1, height - 1, 14, 14)
        g.dispose()
        super.paintComponent(graphics)
    }
}

private object Shell {
    fun run(vararg args: String, timeoutSeconds: Long = 60): CommandResult {
        val normalizedArgs = args.toMutableList().apply {
            if (isNotEmpty()) this[0] = normalizeTool(this[0])
        }
        val processArgs = if (isWindows() && normalizedArgs.firstOrNull() == "jadx.bat") {
            listOf("cmd.exe", "/c") + normalizedArgs
        } else {
            normalizedArgs
        }
        val process = ProcessBuilder(*processArgs.toTypedArray())
            .redirectErrorStream(true)
            .start()
        val output = StringBuilder()
        val reader = Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    synchronized(output) { output.appendLine(line) }
                }
            }
        }.apply { isDaemon = true }
        reader.start()
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            reader.join(2_000)
            return CommandResult(124, "Command timed out: ${args.joinToString(" ")}\n${output.toString().trim()}")
        }
        reader.join(2_000)
        return CommandResult(process.exitValue(), output.toString().trim())
    }
}

private class HookStudioFrame : JFrame("HookStudio") {
    private val packageField = GlassTextField(28)
    private val projectField = GlassTextField(28).apply {
        text = Path.of(System.getProperty("user.home"), "HookStudioProjects").toString()
    }
    private val classField = GlassTextField(28).apply { text = "com.example.Target" }
    private val methodField = GlassTextField(28).apply { text = "targetMethod" }
    private val output = JTextArea()
    private val appLabelCache = mutableMapOf<String, String>()
    private val appNamesCache = mutableMapOf<String, AppNames>()
    private var lastApk: Path? = null

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        loadWindowIcon()?.let { iconImage = it }
        minimumSize = Dimension(1000, 700)
        setSize(1160, 780)
        setLocationRelativeTo(null)
        background = Color(7, 17, 38)
        output.isEditable = false
        output.lineWrap = true
        output.wrapStyleWord = true
        output.foreground = GlassTheme.ink
        output.background = Color(4, 12, 27, 150)
        output.font = output.font.deriveFont(12f)
        output.border = EmptyBorder(14, 16, 14, 16)

        val form = GlassPanel(GridBagLayout())
        form.border = EmptyBorder(18, 20, 18, 20)
        addRow(form, 0, "目标包名", packageField)
        addRow(form, 1, "项目目录", projectField)
        addRow(form, 2, "Hook 类名", classField)
        addRow(form, 3, "Hook 方法", methodField)

        val actions = GlassPanel(FlowLayout(FlowLayout.LEFT, 8, 4))
        button(actions, "检测设备") { detectDevice() }
        button(actions, "选择手机 App") { choosePhoneApp() }
        button(actions, "按包名读取 APK") { pullApk() }
        button(actions, "JADX 反编译") { decompile() }
        button(actions, "导出 AI 分析文件") { exportAiWorkspace() }
        button(actions, "生成模块") { generateModule() }
        button(actions, "查看 Logcat") { readLogcat() }

        val title = JLabel("HookStudio")
        title.foreground = GlassTheme.ink
        title.font = title.font.deriveFont(java.awt.Font.BOLD, 27f)
        val subtitle = JLabel("Android APK 分析与 LSPosed Hook 工作台")
        subtitle.foreground = GlassTheme.muted
        subtitle.font = subtitle.font.deriveFont(13f)
        val titleBlock = JPanel()
        titleBlock.layout = javax.swing.BoxLayout(titleBlock, javax.swing.BoxLayout.Y_AXIS)
        titleBlock.isOpaque = false
        titleBlock.border = EmptyBorder(22, 24, 8, 24)
        titleBlock.add(title)
        titleBlock.add(javax.swing.Box.createVerticalStrut(4))
        titleBlock.add(subtitle)

        val top = JPanel(BorderLayout(0, 10))
        top.isOpaque = false
        top.border = EmptyBorder(0, 18, 0, 18)
        top.add(titleBlock, BorderLayout.NORTH)
        top.add(form, BorderLayout.CENTER)
        top.add(actions, BorderLayout.SOUTH)

        val help = JTextArea("建议流程：检测设备 → 选择手机 App → 导出 AI 分析文件 → 生成模块")
        help.isEditable = false
        help.lineWrap = true
        help.wrapStyleWord = true
        help.foreground = GlassTheme.muted
        help.background = Color(0, 0, 0, 0)
        help.border = EmptyBorder(10, 4, 2, 4)

        val logPanel = GlassPanel(BorderLayout(0, 8))
        logPanel.add(JScrollPane(output).apply {
            border = EmptyBorder(0, 0, 0, 0)
            viewport.isOpaque = false
            isOpaque = false
        }, BorderLayout.CENTER)
        logPanel.add(help, BorderLayout.SOUTH)
        val bottom = JPanel(BorderLayout())
        bottom.isOpaque = false
        bottom.border = EmptyBorder(10, 18, 18, 18)
        bottom.add(logPanel, BorderLayout.CENTER)
        val content = JPanel(BorderLayout(0, 0))
        content.isOpaque = false
        content.add(top, BorderLayout.NORTH)
        content.add(bottom, BorderLayout.CENTER)
        contentPane = LiquidGlassRootPanel()
        contentPane.add(content, BorderLayout.CENTER)
    }

    private fun loadWindowIcon(): BufferedImage? = runCatching {
        val stream: InputStream = javaClass.getResourceAsStream("/hookstudio-icon.png") ?: return null
        stream.use { ImageIO.read(it) }
    }.getOrNull()

    private fun addRow(panel: JPanel, row: Int, label: String, field: JTextField) {
        val c = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
        }
        c.gridx = 0; c.gridy = row; c.weightx = 0.0
        panel.add(JLabel(label).apply {
            foreground = GlassTheme.muted
            font = font.deriveFont(13f)
        }, c)
        c.gridx = 1; c.weightx = 1.0
        panel.add(field, c)
    }

    private fun button(panel: JPanel, title: String, action: () -> Unit) {
        panel.add(GlassButton(title).apply { addActionListener { runAsync(action) } })
    }

    private fun runAsync(action: () -> Unit) {
        Thread {
            try { action() } catch (e: Exception) { log("错误：${e.message}") }
        }.apply { isDaemon = true }.start()
    }

    private fun log(message: String) {
        SwingUtilities.invokeLater {
            output.append("[${java.time.LocalTime.now().withNano(0)}] $message\n")
            output.caretPosition = output.document.length
        }
    }

    private fun detectDevice() {
        val result = Shell.run("adb", "devices", "-l")
        log(result.output.ifBlank { "没有检测到 ADB 输出" })
        if (result.exitCode != 0) log("ADB 退出码：${result.exitCode}")
        val devices = result.output.lineSequence()
            .drop(1)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (devices.any { it.contains("unauthorized") }) log("设备未授权，请在设备上确认 USB 调试授权")
        if (devices.count { it.endsWith("\tdevice") || it.contains(" device ") } > 1) {
            log("检测到多个设备，请指定目标设备后再执行操作")
        }
    }

    private fun choosePhoneApp() {
        val result = Shell.run("adb", "shell", "pm", "list", "packages", "-3", timeoutSeconds = 30)
        require(result.exitCode == 0) { result.output.ifBlank { "无法读取手机应用列表" } }
        val packages = result.output.lineSequence()
            .map { it.trim().removePrefix("package:").trim() }
            .filter { it.matches(Regex("[A-Za-z0-9_.]+")) }
            .distinct()
            .sorted()
            .toList()
        require(packages.isNotEmpty()) { "手机上没有找到第三方应用" }

        val launcherLabels = loadLauncherLabels()
        launcherLabels.forEach { (pkg, label) -> appLabelCache.putIfAbsent(pkg, label) }
        if (launcherLabels.isNotEmpty()) log("已快速读取 ${launcherLabels.size} 个 App 名称")

        var keyword: String? = null
        SwingUtilities.invokeAndWait {
            keyword = JOptionPane.showInputDialog(
                this,
                "输入 App 名称或包名（例如：微信、tencent；留空显示全部）",
                "搜索手机 App",
                JOptionPane.QUESTION_MESSAGE
            )
        }
        val query = keyword?.trim()?.lowercase() ?: return
        val matchedPackages = packages.filter { pkg ->
            pkg.lowercase().contains(query) || appLabelCache[pkg]?.lowercase()?.contains(query) == true
        }
        require(matchedPackages.isNotEmpty()) { "没有匹配的 App：$query" }
        log("搜索到 ${matchedPackages.size} 个 App，仅读取匹配项名称")
        val choices = loadAppChoices(matchedPackages)

        var selected: AppChoice? = null
        SwingUtilities.invokeAndWait {
            selected = JOptionPane.showInputDialog(
                this,
                "选择要读取的手机 App（共 ${choices.size} 个）",
                "选择手机 App",
                JOptionPane.PLAIN_MESSAGE,
                null,
                choices.toTypedArray(),
                choices.first()
            ) as? AppChoice
        }
        selected?.let { choice ->
            val pkg = choice.packageName
            SwingUtilities.invokeLater { packageField.text = pkg }
            pullApk(pkg)
        }
    }

    private fun loadLauncherLabels(): Map<String, String> {
        val result = Shell.run(
            "adb", "shell", "content", "query",
            "--uri", "content://com.miui.home.launcher.settings/favorites",
            "--projection", "title:intent",
            timeoutSeconds = 20
        )
        if (result.exitCode != 0) return emptyMap()
        return buildMap {
            result.output.lineSequence().forEach { line ->
                val label = line.substringAfter("title=", "").substringBefore(", intent=").trim()
                val pkg = Regex("component=([^/;]+)/").find(line)?.groupValues?.get(1).orEmpty()
                if (label.isNotBlank() && pkg.matches(Regex("[A-Za-z0-9_.]+"))) putIfAbsent(pkg, label)
            }
        }
    }

    private fun loadAppChoices(packages: List<String>): List<AppChoice> {
        val tempDir = Files.createTempDirectory("hookstudio-app-labels-")
        return try {
            packages.mapIndexed { index, pkg ->
                if (index == 0 || (index + 1) % 10 == 0 || index == packages.lastIndex) {
                    log("正在读取 App 名称：${index + 1}/${packages.size}")
                }
                val cachedLabel = appLabelCache[pkg]
                if (cachedLabel != null) {
                    AppChoice(cachedLabel, pkg)
                } else {
                    val names = readAppNames(pkg, tempDir, index).also {
                        appNamesCache[pkg] = it
                        appLabelCache[pkg] = it.bestLabel(pkg)
                    }
                    AppChoice(names.displayLabel(pkg), pkg)
                }
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        } finally {
            Files.list(tempDir).use { files -> files.forEach { Files.deleteIfExists(it) } }
            Files.deleteIfExists(tempDir)
        }
    }

    private fun readAppNames(pkg: String, tempDir: Path, index: Int): AppNames {
        val remoteApk = Shell.run("adb", "shell", "pm", "path", pkg, timeoutSeconds = 20).output
            .lineSequence()
            .firstOrNull { it.startsWith("package:") }
            ?.removePrefix("package:")
            ?.trim()
            ?: return AppNames()
        val localApk = tempDir.resolve("app-$index.apk")
        val pulled = Shell.run("adb", "pull", remoteApk, localApk.toString(), timeoutSeconds = 90)
        if (pulled.exitCode != 0 || !Files.isRegularFile(localApk)) return AppNames()
        return readAppNamesFromApk(localApk)
    }

    private fun readAppNamesFromApk(apk: Path): AppNames {
        val aapt = findAndroidTool("aapt") ?: return AppNames()
        val badging = Shell.run(aapt, "dump", "badging", apk.toString(), timeoutSeconds = 30)
        val labels = badging.output.lineSequence().mapNotNull { line ->
            Regex("application-label(?:-([^:]+))?:'([^']*)'").find(line)?.let { match ->
                match.groupValues[1].lowercase() to match.groupValues[2].trim()
            }
        }.filter { it.second.isNotBlank() }.toMap()
        val default = labels[""].orEmpty()
        val english = labels.entries.firstOrNull { it.key.startsWith("en") }?.value
            ?: default.takeIf { value -> value.any(Char::isLetter) && value.none { it.isChinese() } }
            ?: ""
        val chinese = labels.entries.firstOrNull { it.key.startsWith("zh") }?.value
            ?: default.takeIf { value -> value.any { it.isChinese() } }
            ?: ""
        return AppNames(english = english, chinese = chinese)
    }

    private fun Char.isChinese(): Boolean = this.code in 0x3400..0x9FFF

    private fun findAndroidTool(name: String): String? {
        val pathTool = System.getenv("PATH")?.split(java.io.File.pathSeparator)
            ?.asSequence()
            ?.map { Path.of(it, name) }
            ?.firstOrNull { Files.isExecutable(it) }
        if (pathTool != null) return pathTool.toString()
        val sdkRoots = listOfNotNull(System.getenv("ANDROID_HOME"), System.getenv("ANDROID_SDK_ROOT"))
            .distinct()
            .map { Path.of(it) }
        for (sdk in sdkRoots) {
            val candidates = mutableListOf<Path>()
            if (name == "apkanalyzer") candidates.add(sdk.resolve("cmdline-tools/latest/bin/apkanalyzer"))
            val buildTools = sdk.resolve("build-tools")
            if (Files.isDirectory(buildTools)) {
                Files.list(buildTools).use { dirs ->
                    dirs.filter { Files.isDirectory(it) }
                        .sorted(Comparator.reverseOrder())
                        .forEach { candidates.add(it.resolve(name)) }
                }
            }
            val match = candidates.firstOrNull { Files.isExecutable(it) }
            if (match != null) return match.toString()
        }
        return null
    }

    private fun pullApk(packageOverride: String? = null) {
        val pkg = (packageOverride ?: packageField.text).trim()
        require(pkg.matches(Regex("[A-Za-z0-9_.]+"))) { "包名格式不正确" }
        if (packageOverride != null) SwingUtilities.invokeLater { packageField.text = pkg }
        val root = Path.of(projectField.text.trim(), pkg)
        val existingBaseApk = root.resolve("apk/base.apk")
        if (packageOverride == null && lastApk == existingBaseApk && Files.isRegularFile(existingBaseApk)) {
            log("该 APK 已读取，跳过重复下载：$existingBaseApk")
            return
        }
        Files.createDirectories(root.resolve("apk"))
        val paths = Shell.run("adb", "shell", "pm", "path", pkg).output
            .lineSequence().filter { it.startsWith("package:") }
            .map { it.removePrefix("package:").trim() }.toList()
        require(paths.isNotEmpty()) { "设备上找不到包：$pkg" }
        paths.forEachIndexed { index, remote ->
            val name = if (index == 0) "base.apk" else "split-$index.apk"
            val destination = root.resolve("apk").resolve(name)
            val result = Shell.run("adb", "pull", remote, destination.toString(), timeoutSeconds = 180)
            require(result.exitCode == 0) { result.output }
            log("已读取 $remote -> $destination")
            if (index == 0) lastApk = destination
        }
        Files.writeString(root.resolve("target.json"), "{\n  \"package\": \"${jsonEscape(pkg)}\",\n  \"apkCount\": ${paths.size}\n}\n")
        log("APK 读取完成：${paths.size} 个文件")
    }

    private fun decompile() {
        val apk = lastApk ?: error("请先读取 APK")
        val apkDir = apk.parent
        val apkFiles = Files.list(apkDir).use { files ->
            files.filter { it.fileName.toString().endsWith(".apk", ignoreCase = true) }
                .sorted()
                .toList()
        }
        require(apkFiles.isNotEmpty()) { "APK 文件不存在，请先读取 APK" }
        val outputDir = apkDir.parent.resolve("decompiled")
        Files.createDirectories(outputDir)
        val args = mutableListOf("jadx", "-d", outputDir.toString())
        args.addAll(apkFiles.map(Path::toString))
        val result = Shell.run(*args.toTypedArray(), timeoutSeconds = 600)
        log(result.output.ifBlank { "JADX 已完成" })
        require(result.exitCode == 0) { "JADX 退出码：${result.exitCode}" }
    }

    private fun exportAiWorkspace() {
        val pkg = packageField.text.trim()
        require(pkg.matches(Regex("[A-Za-z0-9_.]+"))) { "请先选择手机 App 或填写正确的包名" }
        val projectRoot = Path.of(projectField.text.trim(), pkg)
        val apk = projectRoot.resolve("apk/base.apk")
        require(Files.isRegularFile(apk)) { "请先读取目标 APK" }
        lastApk = apk

        val decompiled = projectRoot.resolve("decompiled")
        val sourceDir = decompiled.resolve("sources")
        if (!Files.isDirectory(sourceDir)) {
            log("尚未反编译，正在先执行 JADX...")
            decompile()
        }

        val appNames = appNamesCache[pkg]
            ?: readAppNamesFromApk(apk).also { appNamesCache[pkg] = it }
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val nameParts = listOf(appNames.english, appNames.chinese)
            .filter { it.isNotBlank() }
            .distinct()
            .map(::safeFileNamePart)
            .filter { it.isNotBlank() }
        val exportName = (nameParts.ifEmpty { listOf(pkg) } + timestamp).joinToString("-")
        val exportRoot = Path.of(System.getProperty("user.home"), "Desktop", "HookStudio-AI", exportName)
        Files.createDirectories(exportRoot)
        copyTree(sourceDir, exportRoot.resolve("sources"))
        copyTree(decompiled.resolve("resources"), exportRoot.resolve("resources"))
        copyTree(projectRoot.resolve("apk"), exportRoot.resolve("apk"))

        val sourceCount = countFiles(exportRoot.resolve("sources"))
        val resourceCount = countFiles(exportRoot.resolve("resources"))
        Files.writeString(exportRoot.resolve("file-index.txt"), buildFileIndex(exportRoot))
        Files.writeString(exportRoot.resolve("analysis-info.json"), """
            {
              "englishName": "${jsonEscape(appNames.english)}",
              "chineseName": "${jsonEscape(appNames.chinese)}",
              "package": "${jsonEscape(pkg)}",
              "generatedAt": "${jsonEscape(timestamp)}",
              "sourceFileCount": $sourceCount,
              "resourceFileCount": $resourceCount,
              "containsApk": true
            }
        """.trimIndent() + "\n")
        Files.writeString(exportRoot.resolve("AI_PROMPT.md"), """
            # APK Hook 分析任务

            英文名称：`${appNames.english.ifBlank { "未读取到" }}`
            中文名称：`${appNames.chinese.ifBlank { "未读取到" }}`
            目标包名：`$pkg`

            请基于 `sources/`、`resources/` 和 `file-index.txt` 分析这个 Android APK：

            1. 找出适合 LSPosed/Xposed Hook 的关键类和方法。
            2. 说明每个候选点的调用时机、参数、返回值和风险。
            3. 优先给出最小可验证的 Hook 方案，包含完整类名、方法名和必要参数类型。
            4. 如果信息不足，明确指出还需要查看哪些文件或运行哪些日志。
            5. 不要修改原始 APK；输出独立的 Hook 模块代码和构建步骤。
        """.trimIndent() + "\n")

        openPath(exportRoot)
        log("AI 分析文件已导出：$exportRoot")
        log("源码 $sourceCount 个，资源 $resourceCount 个")
    }

    private fun safeFileNamePart(value: String): String = value
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]+"), "-")
        .replace(Regex("\\s+"), " ")
        .trim(' ', '.', '-')
        .take(60)

    private fun openPath(path: Path) {
        val command = when {
            isWindows() -> listOf("explorer.exe", path.toString())
            System.getProperty("os.name").contains("Mac", ignoreCase = true) -> listOf("open", path.toString())
            else -> listOf("xdg-open", path.toString())
        }
        Shell.run(*command.toTypedArray())
    }

    private fun copyTree(source: Path, destination: Path) {
        if (!Files.exists(source)) return
        Files.walk(source).use { paths ->
            paths.forEach { input ->
                val relative = source.relativize(input)
                val output = destination.resolve(relative)
                if (Files.isDirectory(input)) Files.createDirectories(output)
                else {
                    Files.createDirectories(output.parent)
                    Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    private fun countFiles(root: Path): Int {
        if (!Files.isDirectory(root)) return 0
        Files.walk(root).use { paths -> return paths.filter { Files.isRegularFile(it) }.count().toInt() }
    }

    private fun buildFileIndex(root: Path): String = buildString {
        Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .sorted()
                .forEach { path ->
                    append(root.relativize(path)).append("\t").append(Files.size(path)).append(" bytes\n")
                }
        }
    }

    private fun generateModule() {
        val pkg = packageField.text.trim()
        require(pkg.isNotBlank()) { "请填写目标包名" }
        require(classField.text.matches(Regex("[A-Za-z_$][A-Za-z0-9_$.]*"))) { "类名格式不正确" }
        require(methodField.text.matches(Regex("[A-Za-z_$][A-Za-z0-9_$]*"))) { "方法名格式不正确" }
        val root = Path.of(projectField.text.trim(), pkg, "generated-module")
        createModule(root, pkg, classField.text.trim(), methodField.text.trim())
        log("LSPosed 模块工程已生成：$root")
        log("进入该目录运行 gradle :app:assembleDebug 进行构建")
    }

    private fun readLogcat() {
        val pkg = packageField.text.trim()
        val result = Shell.run("adb", "logcat", "-d", "-t", "300")
        val lines = result.output.lineSequence()
            .filter { pkg.isBlank() || it.contains(pkg, true) || it.contains("LSPosed", true) || it.contains("Xposed", true) }
            .joinToString("\n")
        log(lines.ifBlank { "没有匹配的 LSPosed/目标包日志" })
    }
}

private fun jsonEscape(value: String): String = buildString {
    value.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
}

private fun createModule(root: Path, targetPackage: String, targetClass: String, targetMethod: String) {
    Files.createDirectories(root)
    Files.writeString(root.resolve("settings.gradle"), "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\n" +
        "dependencyResolutionManagement { repositories { google(); mavenCentral() } }\n" +
        "rootProject.name = 'GeneratedHook'\n" +
        "include ':app'\n")
    Files.writeString(root.resolve("build.gradle"), "plugins { id 'com.android.application' version '8.7.3' apply false }\n")
    Files.writeString(root.resolve("gradle.properties"), "android.useAndroidX=true\norg.gradle.jvmargs=-Xmx2g\n")
    val app = root.resolve("app")
    Files.createDirectories(app.resolve("src/main/java/dev/hookstudio/generated"))
    Files.createDirectories(app.resolve("src/main/res/values"))
    Files.createDirectories(app.resolve("src/main/assets"))
    Files.writeString(app.resolve("build.gradle"), """
        plugins { id 'com.android.application' }
        android { namespace 'dev.hookstudio.generated'; compileSdk 35
            defaultConfig { applicationId 'dev.hookstudio.generated'; minSdk 23; targetSdk 35; versionCode 1; versionName '0.1' }
        }
        dependencies { compileOnly 'de.robv.android.xposed:api:82' }
    """.trimIndent())
    Files.writeString(app.resolve("src/main/AndroidManifest.xml"), """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <application android:theme="@style/AppTheme" android:label="Generated Hook">
                <meta-data android:name="xposedmodule" android:value="true" />
                <meta-data android:name="xposeddescription" android:value="HookStudio generated module" />
                <meta-data android:name="xposedminversion" android:value="93" />
            </application>
        </manifest>
    """.trimIndent())
    Files.writeString(app.resolve("src/main/res/values/styles.xml"), """
        <resources><style name="AppTheme" parent="android:style/Theme.Material.Light.NoActionBar" /></resources>
    """.trimIndent())
    Files.writeString(app.resolve("src/main/assets/xposed_init"), "dev.hookstudio.generated.HookEntry\n")
    Files.writeString(app.resolve("src/main/java/dev/hookstudio/generated/HookEntry.java"), """
        package dev.hookstudio.generated;
        import de.robv.android.xposed.IXposedHookLoadPackage;
        import de.robv.android.xposed.XC_MethodHook;
        import de.robv.android.xposed.XposedBridge;
        import de.robv.android.xposed.XposedHelpers;
        import de.robv.android.xposed.callbacks.XC_LoadPackage;
        public final class HookEntry implements IXposedHookLoadPackage {
            private static final String TARGET = "$targetPackage";
            @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
                if (!TARGET.equals(lpparam.packageName)) return;
                try {
                    Class<?> type = XposedHelpers.findClass("$targetClass", lpparam.classLoader);
                    XposedBridge.hookAllMethods(type, "$targetMethod", new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            XposedBridge.log("HookStudio before: " + param.method);
                        }
                        @Override protected void afterHookedMethod(MethodHookParam param) {
                            XposedBridge.log("HookStudio after: " + param.method + " result=" + param.getResult());
                        }
                    });
                } catch (Throwable error) { XposedBridge.log("HookStudio hook failed: " + error); }
            }
        }
    """.trimIndent())
}

fun main() = SwingUtilities.invokeLater { HookStudioFrame().isVisible = true }
