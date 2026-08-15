package com.liquidglass.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class FileBrowserActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvFileName: TextView
    private lateinit var tvFileSize: TextView
    private lateinit var tvFileType: TextView
    private lateinit var tvFileContent: TextView
    private lateinit var btnAnalyze: Button
    private lateinit var btnExtract: Button
    private lateinit var btnViewManifest: Button

    private var currentUri: Uri? = null
    private var currentFileName: String = ""
    private var currentFileSize: Long = 0
    private var currentFileType: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openFilePicker()
        } else {
            Toast.makeText(this, "需要存储权限才能选择文件", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            currentUri = it
            loadFileInfo(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@FileBrowserActivity, R.color.bg_white))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 48, 24, 16)
            gravity = Gravity.CENTER_VERTICAL
        }
        val backBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            setOnClickListener { finish() }
        }
        header.addView(backBtn)
        val title = TextView(this).apply {
            text = "文件分析"
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@FileBrowserActivity, R.color.text_primary))
            setPadding(16, 0, 0, 0)
        }
        header.addView(title)
        rootLayout.addView(header)

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
        }
        tvFileName = TextView(this).apply {
            text = "文件名: 未选择"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@FileBrowserActivity, R.color.text_primary))
        }
        infoLayout.addView(tvFileName)
        tvFileSize = TextView(this).apply {
            text = "大小: -"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@FileBrowserActivity, R.color.text_secondary))
        }
        infoLayout.addView(tvFileSize)
        tvFileType = TextView(this).apply {
            text = "类型: -"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@FileBrowserActivity, R.color.text_secondary))
        }
        infoLayout.addView(tvFileType)
        rootLayout.addView(infoLayout)

        val btnLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 8, 16, 8)
        }
        btnAnalyze = Button(this).apply {
            text = "分析"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 0, 4, 0) }
            visibility = View.GONE
            setOnClickListener { currentUri?.let { analyzeFile(it) } ?: Toast.makeText(this@FileBrowserActivity, "请先选择文件", Toast.LENGTH_SHORT).show() }
        }
        btnLayout.addView(btnAnalyze)
        btnExtract = Button(this).apply {
            text = "提取"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 0, 4, 0) }
            visibility = View.GONE
            setOnClickListener { currentUri?.let { extractFile(it) } ?: Toast.makeText(this@FileBrowserActivity, "请先选择文件", Toast.LENGTH_SHORT).show() }
        }
        btnLayout.addView(btnExtract)
        btnViewManifest = Button(this).apply {
            text = "Manifest"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 0, 4, 0) }
            visibility = View.GONE
            setOnClickListener { currentUri?.let { viewManifest(it) } ?: Toast.makeText(this@FileBrowserActivity, "请先选择文件", Toast.LENGTH_SHORT).show() }
        }
        btnLayout.addView(btnViewManifest)
        rootLayout.addView(btnLayout)

        val selectBtn = Button(this).apply {
            text = "选择文件"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(16, 8, 16, 8) }
            setOnClickListener { checkPermissionAndPick() }
        }
        rootLayout.addView(selectBtn)

        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@FileBrowserActivity)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        rootLayout.addView(recyclerView)

        val contentScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 200)
        }
        tvFileContent = TextView(this).apply {
            text = "选择文件后显示内容..."
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@FileBrowserActivity, R.color.text_primary))
            setPadding(24, 16, 24, 16)
        }
        contentScroll.addView(tvFileContent)
        rootLayout.addView(contentScroll)

        setContentView(rootLayout)
    }

    private fun checkPermissionAndPick() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                openFilePicker()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun openFilePicker() {
        pickFileLauncher.launch("*/*")
    }

    private fun loadFileInfo(uri: Uri) {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) currentFileName = cursor.getString(nameIndex)
                if (sizeIndex >= 0) currentFileSize = cursor.getLong(sizeIndex)
            }
        }

        currentFileType = when {
            currentFileName.endsWith(".zip", true) -> "ZIP 压缩包"
            currentFileName.endsWith(".rar", true) -> "RAR 压缩包"
            currentFileName.endsWith(".7z", true) -> "7Z 压缩包"
            currentFileName.endsWith(".apk", true) -> "APK 安装包"
            currentFileName.endsWith(".txt", true) -> "文本文件"
            currentFileName.endsWith(".json", true) -> "JSON 文件"
            currentFileName.endsWith(".xml", true) -> "XML 文件"
            currentFileName.endsWith(".html", true) -> "HTML 文件"
            currentFileName.endsWith(".md", true) -> "Markdown 文件"
            else -> "未知类型"
        }

        tvFileName.text = "文件名: $currentFileName"
        tvFileSize.text = "大小: ${formatFileSize(currentFileSize)}"
        tvFileType.text = "类型: $currentFileType"

        updateButtonVisibility()
    }

    private fun updateButtonVisibility() {
        when {
            currentFileType.contains("压缩包") || currentFileType.contains("ZIP") -> {
                btnAnalyze.visibility = View.VISIBLE
                btnExtract.visibility = View.VISIBLE
                btnViewManifest.visibility = View.GONE
            }
            currentFileType.contains("APK") -> {
                btnAnalyze.visibility = View.VISIBLE
                btnExtract.visibility = View.VISIBLE
                btnViewManifest.visibility = View.VISIBLE
            }
            currentFileType.contains("文本") || currentFileType.contains("JSON") || currentFileType.contains("XML") || currentFileType.contains("HTML") || currentFileType.contains("Markdown") -> {
                btnAnalyze.visibility = View.VISIBLE
                btnExtract.visibility = View.GONE
                btnViewManifest.visibility = View.GONE
            }
            else -> {
                btnAnalyze.visibility = View.VISIBLE
                btnExtract.visibility = View.GONE
                btnViewManifest.visibility = View.GONE
            }
        }
    }

    private fun analyzeFile(uri: Uri) {
        when {
            currentFileType.contains("ZIP") || currentFileType.contains("压缩包") -> analyzeZipFile(uri)
            currentFileType.contains("APK") -> analyzeApkFile(uri)
            currentFileType.contains("文本") || currentFileType.contains("JSON") || currentFileType.contains("XML") || currentFileType.contains("HTML") || currentFileType.contains("Markdown") -> analyzeTextFile(uri)
            else -> Toast.makeText(this, "暂不支持分析此文件类型", Toast.LENGTH_SHORT).show()
        }
    }

    private fun analyzeZipFile(uri: Uri) {
        try {
            val entries = mutableListOf<ZipEntryInfo>()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry: ZipEntry? = zipStream.nextEntry
                    while (entry != null) {
                        entries.add(ZipEntryInfo(
                            name = entry.name,
                            size = entry.size,
                            compressedSize = entry.compressedSize,
                            isDirectory = entry.isDirectory,
                            lastModified = entry.time
                        ))
                        entry = zipStream.nextEntry
                    }
                }
            }

            recyclerView.adapter = ZipEntryAdapter(entries)

            val totalSize = entries.sumOf { it.size }
            val totalCompressed = entries.sumOf { it.compressedSize }
            val ratio = if (totalSize > 0) (totalCompressed * 100 / totalSize) else 0

            tvFileContent.text = """
                ZIP 文件分析结果:
                文件总数: ${entries.size}
                总大小: ${formatFileSize(totalSize)}
                压缩后: ${formatFileSize(totalCompressed)}
                压缩率: ${100 - ratio}%
            """.trimIndent()

        } catch (e: Exception) {
            Log.e("FileBrowser", "分析ZIP失败", e)
            Toast.makeText(this, "分析失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun analyzeApkFile(uri: Uri) {
        try {
            val entries = mutableListOf<ZipEntryInfo>()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry: ZipEntry? = zipStream.nextEntry
                    while (entry != null) {
                        entries.add(ZipEntryInfo(
                            name = entry.name,
                            size = entry.size,
                            compressedSize = entry.compressedSize,
                            isDirectory = entry.isDirectory,
                            lastModified = entry.time
                        ))
                        entry = zipStream.nextEntry
                    }
                }
            }

            val classes = entries.filter { it.name.startsWith("classes") && it.name.endsWith(".dex") }
            val resources = entries.filter { it.name.startsWith("res/") }
            val assets = entries.filter { it.name.startsWith("assets/") }
            val lib = entries.filter { it.name.startsWith("lib/") }
            val manifest = entries.find { it.name == "AndroidManifest.xml" }

            recyclerView.adapter = ZipEntryAdapter(entries)

            tvFileContent.text = """
                APK 文件分析结果:
                文件总数: ${entries.size}
                DEX 文件: ${classes.size} 个
                资源文件: ${resources.size} 个
                Assets: ${assets.size} 个
                原生库: ${lib.size} 个
                AndroidManifest: ${if (manifest != null) "存在" else "缺失"}
                总大小: ${formatFileSize(entries.sumOf { it.size })}
            """.trimIndent()

        } catch (e: Exception) {
            Log.e("FileBrowser", "分析APK失败", e)
            Toast.makeText(this, "分析失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun analyzeTextFile(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val content = StringBuilder()
                    var lineCount = 0
                    var charCount = 0
                    var line: String?

                    while (reader.readLine().also { line = it } != null && lineCount < 500) {
                        content.append(line).append("
")
                        charCount += line!!.length
                        lineCount++
                    }

                    tvFileContent.text = content.toString()

                    val summary = """
                        文本分析:
                        行数: $lineCount
                        字符数: $charCount
                        ${if (lineCount >= 500) "(只显示前500行)" else ""}
                    """.trimIndent()

                    Toast.makeText(this, summary, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("FileBrowser", "读取文本失败", e)
            Toast.makeText(this, "读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractFile(uri: Uri) {
        android.app.AlertDialog.Builder(this)
            .setTitle("提取文件")
            .setMessage("将文件提取到应用私有目录")
            .setPositiveButton("提取") { _, _ -> extractToAppDir(uri) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun extractToAppDir(uri: Uri) {
        try {
            val outputDir = File(filesDir, "Extracted/${currentFileName.removeSuffix(".zip").removeSuffix(".apk").removeSuffix(".rar").removeSuffix(".7z")}")
            if (!outputDir.exists()) outputDir.mkdirs()

            contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry: ZipEntry? = zipStream.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val outFile = File(outputDir, entry.name)
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { out -> zipStream.copyTo(out) }
                        }
                        entry = zipStream.nextEntry
                    }
                }
            }

            Toast.makeText(this, "已提取到: ${outputDir.absolutePath}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e("FileBrowser", "提取失败", e)
            Toast.makeText(this, "提取失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun viewManifest(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry: ZipEntry? = zipStream.nextEntry
                    while (entry != null) {
                        if (entry.name == "AndroidManifest.xml") {
                            val content = zipStream.readBytes()
                            tvFileContent.text = "AndroidManifest.xml (二进制格式，大小: ${content.size} 字节)

注意: 这是二进制 XML 格式，需要反编译才能查看可读内容。"
                            return
                        }
                        entry = zipStream.nextEntry
                    }
                }
            }
            Toast.makeText(this, "未找到 AndroidManifest.xml", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("FileBrowser", "读取Manifest失败", e)
            Toast.makeText(this, "读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.2f KB", size / 1024.0)
            else -> "$size B"
        }
    }

    data class ZipEntryInfo(val name: String, val size: Long, val compressedSize: Long, val isDirectory: Boolean, val lastModified: Long)

    inner class ZipEntryAdapter(private val entries: List<ZipEntryInfo>) : RecyclerView.Adapter<ZipEntryAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(android.R.id.text1)
            val tvSize: TextView = itemView.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = LinearLayout(this@FileBrowserActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 16, 24, 16)
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val nameTv = TextView(this@FileBrowserActivity).apply {
                id = android.R.id.text1
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@FileBrowserActivity, R.color.text_primary))
                ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
                maxLines = 1
            }
            layout.addView(nameTv)
            val sizeTv = TextView(this@FileBrowserActivity).apply {
                id = android.R.id.text2
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@FileBrowserActivity, R.color.text_secondary))
                setPadding(16, 0, 0, 0)
            }
            layout.addView(sizeTv)
            return ViewHolder(layout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = entries[position]
            holder.tvName.text = entry.name
            holder.tvSize.text = if (entry.isDirectory) "目录" else formatFileSize(entry.size)
        }

        override fun getItemCount(): Int = entries.size
    }
}
