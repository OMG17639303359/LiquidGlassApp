package com.liquidglass.app

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 32)
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.bg_white))
        }
        scrollView.addView(rootLayout)

        val titleText = TextView(this).apply {
            text = "液态玻璃工具箱"
            textSize = 28f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
            setPadding(0, 0, 0, 8)
        }
        rootLayout.addView(titleText)

        val timeText = TextView(this).apply {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            text = sdf.format(Date())
            textSize = 48f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
            setPadding(0, 0, 0, 8)
        }
        rootLayout.addView(timeText)

        val dateText = TextView(this).apply {
            val sdf = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA)
            text = sdf.format(Date())
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            setPadding(0, 0, 0, 32)
        }
        rootLayout.addView(dateText)

        val features = listOf(
            Triple("AI 助手", "与AI聊天、上传文件分析", ChatActivity::class.java),
            Triple("应用管理", "冻结/解冻/卸载应用", AppManagerActivity::class.java),
            Triple("APK逆向", "反编译和分析APK文件", ApkReverseActivity::class.java),
            Triple("文件分析", "查看压缩包和文本文件", FileBrowserActivity::class.java),
            Triple("计算器", "科学计算器", CalculatorActivity::class.java),
            Triple("工具箱", "更多实用工具", ToolsActivity::class.java),
            Triple("设置", "主题、API配置", SettingsActivity::class.java)
        )

        features.forEach { (name, desc, activityClass) ->
            rootLayout.addView(createFeatureCard(name, desc, activityClass))
            rootLayout.addView(createSpacer(16))
        }

        setContentView(scrollView)
    }

    private fun createFeatureCard(name: String, desc: String, activityClass: Class<*>): CardView {
        return CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            radius = 24f
            cardElevation = 4f

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(ContextCompat.getColor(this@MainActivity, R.color.card_white))
            }
            background = drawable

            val contentLayout = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 24, 24, 24)
                gravity = Gravity.CENTER_VERTICAL
            }

            val iconBox = LinearLayout(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(48, 48)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                }
            }
            val iconText = TextView(this@MainActivity).apply {
                text = name.take(1)
                textSize = 20f
                setTextColor(android.graphics.Color.WHITE)
            }
            iconBox.addView(iconText)
            contentLayout.addView(iconBox)

            val textLayout = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nameText = TextView(this@MainActivity).apply {
                text = name
                textSize = 18f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
            }
            textLayout.addView(nameText)

            val descText = TextView(this@MainActivity).apply {
                text = desc
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            }
            textLayout.addView(descText)

            contentLayout.addView(textLayout)
            addView(contentLayout)

            setOnClickListener {
                startActivity(Intent(this@MainActivity, activityClass))
            }

            isClickable = true
            isFocusable = true
        }
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
