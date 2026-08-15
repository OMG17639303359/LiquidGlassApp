package com.liquidglass.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class CalculatorActivity : AppCompatActivity() {
    private var display = "0"
    private var prevValue = ""
    private var operation = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 48, 16, 16)
            setBackgroundColor(ContextCompat.getColor(this@CalculatorActivity, R.color.bg_white))
        }

        val backBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            setOnClickListener { finish() }
        }
        root.addView(backBtn)

        val displayText = TextView(this).apply {
            text = display
            textSize = 48f
            setPadding(16, 32, 16, 32)
        }
        root.addView(displayText)

        val buttons = arrayOf(
            arrayOf("C", "±", "%", "÷"),
            arrayOf("7", "8", "9", "×"),
            arrayOf("4", "5", "6", "-"),
            arrayOf("1", "2", "3", "+"),
            arrayOf("0", ".", "=", "")
        )

        buttons.forEach { row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            row.forEach { label ->
                if (label.isNotEmpty()) {
                    val btn = Button(this).apply {
                        text = label
                        textSize = 24f
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                        setOnClickListener {
                            when (label) {
                                "C" -> { display = "0"; prevValue = ""; operation = "" }
                                "=" -> {
                                    if (prevValue.isNotEmpty() && operation.isNotEmpty()) {
                                        val a = prevValue.toDoubleOrNull() ?: 0.0
                                        val b = display.toDoubleOrNull() ?: 0.0
                                        display = when (operation) {
                                            "+" -> (a + b).toString()
                                            "-" -> (a - b).toString()
                                            "×" -> (a * b).toString()
                                            "÷" -> if (b != 0.0) (a / b).toString() else "Error"
                                            else -> display
                                        }.removeSuffix(".0")
                                        prevValue = ""
                                        operation = ""
                                    }
                                }
                                "+", "-", "×", "÷" -> {
                                    prevValue = display
                                    operation = label
                                    display = "0"
                                }
                                else -> {
                                    display = if (display == "0") label else display + label
                                }
                            }
                            displayText.text = display
                        }
                    }
                    rowLayout.addView(btn)
                } else {
                    val spacer = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    }
                    rowLayout.addView(spacer)
                }
            }
            root.addView(rowLayout)
        }

        setContentView(root)
    }
}
