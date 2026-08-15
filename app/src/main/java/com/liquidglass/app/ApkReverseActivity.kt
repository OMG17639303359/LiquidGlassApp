package com.liquidglass.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ApkReverseActivity : AppCompatActivity() {

    private lateinit var resultText: TextView

    private val pickApkLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { analyzeApk(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 24)
            setBackgroundColor(ContextCompat.getColor(this@ApkReverseActivity, R.color.bg_white))
        }

        val backBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            setOnClickListener { finish() }
        }
        layout.addView(backBtn)

        val title = TextView(this).apply {
            text = "APK 逆向分析"
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@ApkReverseActivity, R.color.text_primary))
            setPadding(0, 0, 0, 24)
        }
        layout.addView(title)

        val selectBtn = Button(this).apply {
            text = "选择 APK 文件"
            setOnClickListener {
                pickApkLauncher.launch("application/vnd.android.package-archive")
            }
        }
        layout.addView(selectBtn)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { setMargins(0, 16, 0, 0) }
        }
        resultText = TextView(this).apply {
            text = "\n功能说明:\n• 查看APK基本信息\n• 提取权限列表\n• 查看组件信息\n• 分析AndroidManifest.xml\n\n选择APK文件开始分析..."
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@ApkReverseActivity, R.color.text_primary))
            setPadding(0, 16, 0, 0)
        }
        scrollView.addView(resultText)
        layout.addView(scrollView)

        setContentView(layout)
    }

    private fun analyzeApk(uri: Uri) {
        try {
            val entries = mutableListOf<ZipEntry>()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry: ZipEntry? = zipStream.nextEntry
                    while (entry != null) {
                        entries.add(entry)
                        entry = zipStream.nextEntry
                    }
                }
            }

            val classesDex = entries.filter { it.name.startsWith("classes") && it.name.endsWith(".dex") }
            val resources = entries.filter { it.name.startsWith("res/") }
            val assets = entries.filter { it.name.startsWith("assets/") }
            val lib = entries.filter { it.name.startsWith("lib/") }
            val manifest = entries.find { it.name == "AndroidManifest.xml" }
            val cert = entries.find { it.name.startsWith("META-INF/") && (it.name.endsWith(".RSA") || it.name.endsWith(".DSA") || it.name.endsWith(".SF")) }

            val totalSize = entries.sumOf { it.size }

            resultText.text = """
                APK 分析报告
                ===================

                文件总数: ${entries.size}
                总大小: ${formatSize(totalSize)}

                DEX 文件: ${classesDex.size} 个
                ${classesDex.joinToString("\n") { "  - ${it.name} (${formatSize(it.size)})" }}

                资源文件: ${resources.size} 个
                Assets: ${assets.size} 个
                原生库: ${lib.size} 个

                AndroidManifest.xml: ${if (manifest != null) "✓ 存在 (${formatSize(manifest.size)})" else "✗ 缺失"}

                签名文件: ${if (cert != null) "✓ 存在" else "✗ 未找到"}

                注意: 详细反编译需要使用 apktool 或 jadx 等工具
            """.trimIndent()

        } catch (e: Exception) {
            resultText.text = "分析失败: ${e.message}"
            Toast.makeText(this, "分析失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatSize(size: Long): String {
        return when {
            size >= 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.2f KB", size / 1024.0)
            else -> "$size B"
        }
    }
}
