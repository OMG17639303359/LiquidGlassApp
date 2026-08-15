package com.liquidglass.app

import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.net.InetAddress

class ToolsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scrollView = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 24)
            setBackgroundColor(ContextCompat.getColor(this@ToolsActivity, R.color.bg_white))
        }
        scrollView.addView(root)

        val backBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            setOnClickListener { finish() }
        }
        root.addView(backBtn)

        val title = TextView(this).apply {
            text = "工具箱"
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@ToolsActivity, R.color.text_primary))
            setPadding(0, 0, 0, 24)
        }
        root.addView(title)

        val tools = listOf(
            Triple("系统信息", "查看设备详细信息") { showSystemInfo() },
            Triple("网络工具", "Ping测试和IP信息") { showNetworkTools() },
            Triple("二维码", "生成二维码图片") { showQRCodeGenerator() },
            Triple("Base64", "文本编解码") { showBase64Tool() }
        )

        tools.forEach { (name, desc, action) ->
            val card = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 12) }
                radius = 16f
                setCardBackgroundColor(ContextCompat.getColor(this@ToolsActivity, R.color.card_white))

                val layout = LinearLayout(this@ToolsActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(24, 20, 24, 20)
                }
                val nameText = TextView(this@ToolsActivity).apply {
                    text = name
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(this@ToolsActivity, R.color.text_primary))
                }
                layout.addView(nameText)
                val descText = TextView(this@ToolsActivity).apply {
                    text = desc
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(this@ToolsActivity, R.color.text_secondary))
                }
                layout.addView(descText)
                addView(layout)
                setOnClickListener { action() }
            }
            root.addView(card)
        }

        setContentView(scrollView)
    }

    private fun showSystemInfo() {
        val info = buildString {
            appendLine("设备信息")
            appendLine("==========")
            appendLine("制造商: ${Build.MANUFACTURER}")
            appendLine("品牌: ${Build.BRAND}")
            appendLine("型号: ${Build.MODEL}")
            appendLine("设备: ${Build.DEVICE}")
            appendLine("产品: ${Build.PRODUCT}")
            appendLine()
            appendLine("系统信息")
            appendLine("==========")
            appendLine("Android版本: ${Build.VERSION.RELEASE}")
            appendLine("SDK级别: ${Build.VERSION.SDK_INT}")
            appendLine("安全补丁: ${Build.VERSION.SECURITY_PATCH ?: "N/A"}")
            appendLine()
            appendLine("硬件信息")
            appendLine("==========")
            appendLine("主板: ${Build.BOARD}")
            appendLine("硬件: ${Build.HARDWARE}")
            appendLine("处理器: ${Build.SUPPORTED_ABIS?.joinToString() ?: "N/A"}")
        }

        AlertDialog.Builder(this)
            .setTitle("系统信息")
            .setMessage(info)
            .setPositiveButton("复制") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setText(info)
                Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showNetworkTools() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)

        val ipText = TextView(this).apply {
            text = "本机IP: $ipAddress"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@ToolsActivity, R.color.text_primary))
            setPadding(0, 0, 0, 16)
        }
        layout.addView(ipText)

        val input = EditText(this).apply {
            hint = "输入IP或域名进行Ping测试"
            setText("8.8.8.8")
        }
        layout.addView(input)

        val resultText = TextView(this).apply {
            text = "点击开始测试"
            setPadding(0, 16, 0, 0)
            setTextColor(ContextCompat.getColor(this@ToolsActivity, R.color.text_secondary))
        }
        layout.addView(resultText)

        val pingBtn = Button(this).apply {
            text = "开始Ping测试"
            setOnClickListener {
                val host = input.text.toString().trim()
                if (host.isEmpty()) return@setOnClickListener
                resultText.text = "正在测试 $host..."
                Thread {
                    try {
                        val inet = InetAddress.getByName(host)
                        val reachable = inet.isReachable(3000)
                        val info = if (reachable) "✓ $host 可达\nIP: ${inet.hostAddress}" else "✗ $host 不可达"
                        runOnUiThread { resultText.text = info }
                    } catch (e: Exception) {
                        runOnUiThread { resultText.text = "错误: ${e.message}" }
                    }
                }.start()
            }
        }
        layout.addView(pingBtn)

        AlertDialog.Builder(this)
            .setTitle("网络工具")
            .setView(layout)
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showQRCodeGenerator() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val input = EditText(this).apply {
            hint = "输入要生成二维码的内容"
        }
        layout.addView(input)

        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(400, 400).apply {
                setMargins(0, 24, 0, 24)
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            setBackgroundColor(Color.LTGRAY)
        }
        layout.addView(imageView)

        val generateBtn = Button(this).apply {
            text = "生成二维码"
            setOnClickListener {
                val content = input.text.toString().trim()
                if (content.isEmpty()) {
                    Toast.makeText(this@ToolsActivity, "请输入内容", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                try {
                    val writer = QRCodeWriter()
                    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 400, 400)
                    val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565)
                    for (x in 0 until 400) {
                        for (y in 0 until 400) {
                            bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                        }
                    }
                    imageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Toast.makeText(this@ToolsActivity, "生成失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(generateBtn)

        AlertDialog.Builder(this)
            .setTitle("二维码生成器")
            .setView(layout)
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showBase64Tool() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val input = EditText(this).apply {
            hint = "输入文本"
            minLines = 3
        }
        layout.addView(input)

        val result = TextView(this).apply {
            text = "结果将显示在这里"
            setPadding(0, 16, 0, 16)
            setTextIsSelectable(true)
        }
        layout.addView(result)

        val btnLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val encodeBtn = Button(this).apply {
            text = "编码"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val text = input.text.toString()
                result.text = android.util.Base64.encodeToString(text.toByteArray(), android.util.Base64.DEFAULT)
            }
        }
        btnLayout.addView(encodeBtn)

        val decodeBtn = Button(this).apply {
            text = "解码"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                try {
                    val text = input.text.toString()
                    result.text = String(android.util.Base64.decode(text, android.util.Base64.DEFAULT))
                } catch (e: Exception) {
                    result.text = "解码失败: ${e.message}"
                }
            }
        }
        btnLayout.addView(decodeBtn)
        layout.addView(btnLayout)

        AlertDialog.Builder(this)
            .setTitle("Base64 工具")
            .setView(layout)
            .setNegativeButton("关闭", null)
            .show()
    }
}
