package com.liquidglass.app

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import rikka.shizuku.Shizuku

class AppManagerActivity : AppCompatActivity() {

    private var appList = listOf<AppInfo>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var statusText: TextView

    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == 1) {
            updateShizukuStatus()
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                loadApps()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shizuku.addRequestPermissionResultListener(shizukuListener)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@AppManagerActivity, R.color.bg_white))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 48, 24, 16)
        }
        val backBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            setOnClickListener { finish() }
        }
        header.addView(backBtn)
        val title = TextView(this).apply {
            text = "应用管理"
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@AppManagerActivity, R.color.text_primary))
            setPadding(16, 0, 0, 0)
        }
        header.addView(title)
        rootLayout.addView(header)

        statusText = TextView(this).apply {
            textSize = 14f
            setPadding(24, 0, 24, 16)
        }
        updateShizukuStatus()
        rootLayout.addView(statusText)

        val searchInput = EditText(this).apply {
            hint = "搜索应用..."
            setPadding(24, 16, 24, 16)
        }
        rootLayout.addView(searchInput)

        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@AppManagerActivity)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        rootLayout.addView(recyclerView)

        loadApps()

        setContentView(rootLayout)
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuListener)
    }

    private fun updateShizukuStatus() {
        val status = when {
            !Shizuku.pingBinder() -> "Shizuku 未运行 - 请先启动 Shizuku 服务"
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> "需要 Shizuku 权限 - 点击授权"
            else -> "Shizuku 已就绪"
        }
        statusText.text = status
        statusText.setTextColor(ContextCompat.getColor(this, if (status.contains("已就绪")) R.color.success else R.color.warning))

        if (status.contains("需要 Shizuku 权限")) {
            statusText.setOnClickListener {
                if (Shizuku.shouldShowRequestPermissionRationale()) {
                    Toast.makeText(this, "请在系统设置中授予权限", Toast.LENGTH_SHORT).show()
                } else {
                    Shizuku.requestPermission(1)
                }
            }
        }
    }

    private fun loadApps() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map {
                AppInfo(
                    packageName = it.packageName,
                    appName = it.loadLabel(pm).toString(),
                    isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isEnabled = it.enabled
                )
            }
            .sortedBy { it.appName }

        appList = apps
        recyclerView.adapter = AppAdapter(apps)
    }

    data class AppInfo(val packageName: String, val appName: String, val isSystem: Boolean, val isEnabled: Boolean)

    inner class AppAdapter(private val items: List<AppInfo>) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        inner class ViewHolder(val card: CardView) : RecyclerView.ViewHolder(card)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val card = CardView(this@AppManagerActivity).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(16, 8, 16, 8) }
                radius = 16f
                setCardBackgroundColor(ContextCompat.getColor(this@AppManagerActivity, R.color.card_white))
            }
            return ViewHolder(card)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = items[position]
            val card = holder.card
            card.removeAllViews()

            val layout = LinearLayout(this@AppManagerActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)
            }

            val nameRow = LinearLayout(this@AppManagerActivity).apply { orientation = LinearLayout.HORIZONTAL }
            val nameText = TextView(this@AppManagerActivity).apply {
                text = app.appName
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@AppManagerActivity, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            nameRow.addView(nameText)

            if (app.isSystem) {
                val badge = TextView(this@AppManagerActivity).apply {
                    text = "系统"
                    textSize = 11f
                    setPadding(8, 2, 8, 2)
                    background = GradientDrawable().apply {
                        cornerRadius = 8f
                        setColor(ContextCompat.getColor(this@AppManagerActivity, R.color.warning_bg))
                    }
                    setTextColor(ContextCompat.getColor(this@AppManagerActivity, R.color.warning))
                }
                nameRow.addView(badge)
            }
            layout.addView(nameRow)

            val pkgText = TextView(this@AppManagerActivity).apply {
                text = app.packageName
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@AppManagerActivity, R.color.text_secondary))
            }
            layout.addView(pkgText)

            val btnLayout = LinearLayout(this@AppManagerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 0)
            }

            val freezeBtn = Button(this@AppManagerActivity).apply {
                text = if (app.isEnabled) "冻结" else "解冻"
                textSize = 12f
                setOnClickListener {
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        if (app.isEnabled) freezeApp(app.packageName) else unfreezeApp(app.packageName)
                    } else {
                        Toast.makeText(this@AppManagerActivity, "请先授权Shizuku权限", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            btnLayout.addView(freezeBtn)

            val stopBtn = Button(this@AppManagerActivity).apply {
                text = "停止"
                textSize = 12f
                setOnClickListener {
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        forceStopApp(app.packageName)
                    } else {
                        Toast.makeText(this@AppManagerActivity, "请先授权Shizuku权限", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            btnLayout.addView(stopBtn)

            val uninstallBtn = Button(this@AppManagerActivity).apply {
                text = "卸载"
                textSize = 12f
                setOnClickListener {
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        uninstallApp(app.packageName)
                    } else {
                        Toast.makeText(this@AppManagerActivity, "请先授权Shizuku权限", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            btnLayout.addView(uninstallBtn)

            layout.addView(btnLayout)
            card.addView(layout)
        }

        override fun getItemCount() = items.size
    }

    private fun freezeApp(packageName: String) {
        try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", "pm disable-user $packageName"), null, null)
            val result = process.waitFor()
            Toast.makeText(this, if (result == 0) "冻结成功" else "冻结失败", Toast.LENGTH_SHORT).show()
            loadApps()
        } catch (e: Exception) {
            Toast.makeText(this, "错误: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unfreezeApp(packageName: String) {
        try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", "pm enable $packageName"), null, null)
            val result = process.waitFor()
            Toast.makeText(this, if (result == 0) "解冻成功" else "解冻失败", Toast.LENGTH_SHORT).show()
            loadApps()
        } catch (e: Exception) {
            Toast.makeText(this, "错误: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun forceStopApp(packageName: String) {
        try {
            Shizuku.newProcess(arrayOf("sh", "-c", "am force-stop $packageName"), null, null)
            Toast.makeText(this, "已强制停止", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "错误: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uninstallApp(packageName: String) {
        try {
            Shizuku.newProcess(arrayOf("sh", "-c", "pm uninstall $packageName"), null, null)
            Toast.makeText(this, "卸载请求已发送", Toast.LENGTH_SHORT).show()
            loadApps()
        } catch (e: Exception) {
            Toast.makeText(this, "错误: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
