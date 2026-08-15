package com.liquidglass.app

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val scrollView = ScrollView(this)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 24)
            setBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.bg_white))
        }
        scrollView.addView(rootLayout)

        val titleText = TextView(this).apply {
            text = "设置"
            textSize = 28f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_primary))
            setPadding(0, 0, 0, 32)
        }
        rootLayout.addView(titleText)

        rootLayout.addView(createSectionTitle("外观设置"))
        rootLayout.addView(createSettingCard("主题颜色", prefs.getString("theme", "纯白") ?: "纯白") {
            showThemeSelector()
        })
        rootLayout.addView(createSettingCard("玻璃效果", if (prefs.getBoolean("liquid_glass", true)) "液态玻璃" else "毛玻璃") {
            prefs.edit().putBoolean("liquid_glass", !prefs.getBoolean("liquid_glass", true)).apply()
            recreate()
        })

        rootLayout.addView(createSpacer(24))
        rootLayout.addView(createSectionTitle("AI 设置"))

        val providerName = prefs.getString("ai_provider_name", "OpenAI") ?: "OpenAI"
        rootLayout.addView(createSettingCard("AI 服务商", providerName) {
            showProviderSelector()
        })

        val apiKey = prefs.getString("api_key", "") ?: ""
        rootLayout.addView(createSettingCard("API 密钥", if (apiKey.isNotBlank()) "已配置" else "未配置") {
            showApiKeyInput()
        })

        rootLayout.addView(createSettingCard("获取 API 密钥", "前往官网") {
            val provider = prefs.getString("ai_provider", "openai") ?: "openai"
            val url = when (provider) {
                "openai" -> "https://platform.openai.com/api-keys"
                "anthropic" -> "https://console.anthropic.com/settings/keys"
                "deepseek" -> "https://platform.deepseek.com/api_keys"
                "moonshot" -> "https://platform.moonshot.cn/console/api-keys"
                else -> "https://www.google.com/search?q=$provider+api+key"
            }
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        })

        rootLayout.addView(createSpacer(24))
        rootLayout.addView(createSectionTitle("关于"))
        rootLayout.addView(createSettingCard("版本", "1.0.0") {})
        rootLayout.addView(createSettingCard("开源协议", "Apache 2.0") {})

        setContentView(scrollView)
    }

    private fun createSectionTitle(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_secondary))
            setPadding(8, 16, 0, 8)
        }
    }

    private fun createSettingCard(title: String, value: String, onClick: () -> Unit): CardView {
        return CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8) }
            radius = 16f
            setCardBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.card_white))
            setOnClickListener { onClick() }

            val layout = LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 20, 24, 20)
            }

            val titleText = TextView(this@SettingsActivity).apply {
                text = title
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            layout.addView(titleText)

            val valueText = TextView(this@SettingsActivity).apply {
                text = value
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_secondary))
            }
            layout.addView(valueText)

            addView(layout)
        }
    }

    private fun showThemeSelector() {
        val themes = arrayOf("纯白", "深海蓝", "琥珀黄", "暗夜")
        AlertDialog.Builder(this)
            .setTitle("选择主题")
            .setItems(themes) { _, which ->
                prefs.edit().putString("theme", themes[which]).apply()
                recreate()
            }
            .show()
    }

    private fun showProviderSelector() {
        val providers = arrayOf(
            Pair("OpenAI", "openai"),
            Pair("DeepSeek", "deepseek"),
            Pair("Moonshot AI", "moonshot"),
            Pair("阿里云通义千问", "aliyun")
        )
        AlertDialog.Builder(this)
            .setTitle("选择 AI 服务商")
            .setItems(providers.map { it.first }.toTypedArray()) { _, which ->
                prefs.edit()
                    .putString("ai_provider", providers[which].second)
                    .putString("ai_provider_name", providers[which].first)
                    .apply()
                recreate()
            }
            .show()
    }

    private fun showApiKeyInput() {
        val input = EditText(this).apply {
            hint = "输入 API 密钥"
            setText(prefs.getString("api_key", "") ?: "")
        }
        AlertDialog.Builder(this)
            .setTitle("配置 API 密钥")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                prefs.edit().putString("api_key", input.text.toString().trim()).apply()
                recreate()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun createSpacer(height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
            )
        }
    }
}
