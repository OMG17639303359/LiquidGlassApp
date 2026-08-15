package com.liquidglass.app

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ChatActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private val messages = mutableListOf<ChatMessage>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val apiKey = prefs.getString("api_key", "") ?: ""
        if (apiKey.isBlank()) {
            showApiKeyDialog()
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@ChatActivity, R.color.bg_white))
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 48, 24, 16)
            gravity = Gravity.CENTER_VERTICAL
        }

        val backButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            setOnClickListener { finish() }
        }
        headerLayout.addView(backButton)

        val titleText = TextView(this).apply {
            text = "AI 助手"
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@ChatActivity, R.color.text_primary))
            setPadding(16, 0, 0, 0)
        }
        headerLayout.addView(titleText)
        rootLayout.addView(headerLayout)

        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        recyclerView.adapter = ChatAdapter(messages)
        rootLayout.addView(recyclerView)

        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 32)
            setBackgroundColor(ContextCompat.getColor(this@ChatActivity, R.color.card_white))
        }

        messageInput = EditText(this).apply {
            hint = "输入消息..."
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(24, 16, 24, 16)
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(ContextCompat.getColor(this@ChatActivity, R.color.input_bg))
            }
        }
        inputLayout.addView(messageInput)

        val sendButton = Button(this).apply {
            text = "发送"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(8, 0, 0, 0) }
            setOnClickListener { sendMessage() }
        }
        inputLayout.addView(sendButton)
        rootLayout.addView(inputLayout)

        setContentView(rootLayout)
    }

    private fun showApiKeyDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要API密钥")
            .setMessage("您尚未配置AI服务的API密钥。请在设置中配置API密钥以使用AI聊天功能。")
            .setPositiveButton("前往设置") { _, _ ->
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show()
    }

    private fun sendMessage() {
        val content = messageInput.text.toString().trim()
        if (content.isEmpty()) return

        val apiKey = prefs.getString("api_key", "") ?: ""
        if (apiKey.isBlank()) {
            showApiKeyDialog()
            return
        }

        messages.add(ChatMessage("user", content))
        recyclerView.adapter?.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
        messageInput.setText("")

        messages.add(ChatMessage("assistant", "思考中...", true))
        recyclerView.adapter?.notifyItemInserted(messages.size - 1)

        val provider = prefs.getString("ai_provider", "openai") ?: "openai"
        callAiApi(content, apiKey, provider)
    }

    private fun callAiApi(content: String, apiKey: String, provider: String) {
        val baseUrl = when (provider) {
            "openai" -> "https://api.openai.com/v1/chat/completions"
            "anthropic" -> "https://api.anthropic.com/v1/messages"
            "deepseek" -> "https://api.deepseek.com/v1/chat/completions"
            "moonshot" -> "https://api.moonshot.cn/v1/chat/completions"
            "aliyun" -> "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"
            else -> "https://api.openai.com/v1/chat/completions"
        }

        val model = when (provider) {
            "openai" -> "gpt-4o-mini"
            "anthropic" -> "claude-3-sonnet-20240229"
            "deepseek" -> "deepseek-chat"
            "moonshot" -> "moonshot-v1-8k"
            "aliyun" -> "qwen-turbo"
            else -> "gpt-4o-mini"
        }

        val json = when (provider) {
            "aliyun" -> JSONObject().apply {
                put("model", model)
                put("input", JSONObject().apply {
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", content)
                        })
                    })
                })
            }
            else -> JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", content)
                    })
                })
                put("temperature", 0.7)
            }
        }

        val request = Request.Builder()
            .url(baseUrl)
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json.toString()))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    messages.removeAt(messages.size - 1)
                    messages.add(ChatMessage("assistant", "请求失败: ${e.message}", isError = true))
                    recyclerView.adapter?.notifyDataSetChanged()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                Handler(Looper.getMainLooper()).post {
                    messages.removeAt(messages.size - 1)
                    val reply = try {
                        val jsonObj = JSONObject(body)
                        when {
                            jsonObj.has("choices") -> jsonObj.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                            jsonObj.has("output") -> jsonObj.getJSONObject("output").getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                            jsonObj.has("content") -> jsonObj.getJSONArray("content").getJSONObject(0).getString("text")
                            else -> "响应: ${body.take(200)}"
                        }
                    } catch (e: Exception) {
                        "错误: ${e.message}\n响应: ${body.take(200)}"
                    }
                    messages.add(ChatMessage("assistant", reply))
                    recyclerView.adapter?.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            }
        })
    }

    data class ChatMessage(val role: String, val content: String, val isLoading: Boolean = false, val isError: Boolean = false)

    inner class ChatAdapter(private val items: MutableList<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

        inner class ViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = LinearLayout(this@ChatActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 8, 16, 8)
            }
            return ViewHolder(layout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val msg = items[position]
            val layout = holder.layout
            layout.removeAllViews()

            val card = CardView(this@ChatActivity).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = if (msg.role == "user") Gravity.END else Gravity.START
                }
                radius = 16f
                setCardBackgroundColor(ContextCompat.getColor(this@ChatActivity, when {
                    msg.isError -> R.color.error_bg
                    msg.role == "user" -> R.color.user_msg_bg
                    else -> R.color.assistant_msg_bg
                }))
            }

            val textView = TextView(this@ChatActivity).apply {
                text = if (msg.isLoading) "..." else msg.content
                textSize = 15f
                setTextColor(ContextCompat.getColor(this@ChatActivity, if (msg.isError) R.color.error_text else R.color.text_primary))
                setPadding(20, 16, 20, 16)
            }
            card.addView(textView)
            layout.addView(card)
        }

        override fun getItemCount() = items.size
    }
}
