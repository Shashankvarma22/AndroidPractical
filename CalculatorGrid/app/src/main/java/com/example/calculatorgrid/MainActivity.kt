package com.example.calculatorgrid

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView
    private var firstValue = 0.0
    private var operator = ""
    private var isNewOp = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)

        val numButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDot
        )

        for (id in numButtons) {
            findViewById<Button>(id).setOnClickListener {
                val btnText = (it as Button).text.toString()
                if (isNewOp || tvDisplay.text == "Error") {
                    tvDisplay.text = if (btnText == ".") "0." else btnText
                    isNewOp = false
                } else {
                    if (btnText == "." && tvDisplay.text.contains(".")) return@setOnClickListener
                    tvDisplay.append(btnText)
                }
            }
        }

        val opButtons = mapOf(
            R.id.btnAdd to "+", R.id.btnSub to "−",
            R.id.btnMul to "×", R.id.btnDiv to "÷"
        )

        for ((id, op) in opButtons) {
            findViewById<Button>(id).setOnClickListener {
                if (operator.isNotEmpty() && !isNewOp) {
                    performCalculation()
                }
                firstValue = tvDisplay.text.toString().toDoubleOrNull() ?: 0.0
                operator = op
                isNewOp = true
            }
        }

        findViewById<Button>(R.id.btnEqual).setOnClickListener {
            performCalculation()
            operator = ""
        }

        findViewById<Button>(R.id.btnC).setOnClickListener {
            tvDisplay.text = "0"
            firstValue = 0.0
            operator = ""
            isNewOp = true
        }

        findViewById<Button>(R.id.btnBackspace).setOnClickListener {
            val currentText = tvDisplay.text.toString()
            if (currentText.length > 1 && currentText != "Error") {
                tvDisplay.text = currentText.substring(0, currentText.length - 1)
            } else {
                tvDisplay.text = "0"
                isNewOp = true
            }
        }

        findViewById<Button>(R.id.btnPercent).setOnClickListener {
            val currentVal = tvDisplay.text.toString().toDoubleOrNull() ?: 0.0
            val result = currentVal / 100.0
            tvDisplay.text = if (result % 1 == 0.0) result.toLong().toString() else result.toString()
            isNewOp = true // Result of percent is usually treated as a finished number entry
        }
    }

    private fun performCalculation() {
        val secondValue = tvDisplay.text.toString().toDoubleOrNull() ?: return
        var result = 0.0
        var isError = false

        when (operator) {
            "+" -> result = firstValue + secondValue
            "−" -> result = firstValue - secondValue
            "×" -> result = firstValue * secondValue
            "÷" -> if (secondValue != 0.0) result = firstValue / secondValue else isError = true
            else -> result = secondValue
        }

        if (isError) {
            tvDisplay.text = "Error"
            firstValue = 0.0
            operator = ""
        } else {
            tvDisplay.text = if (result % 1 == 0.0) result.toLong().toString() else result.toString()
            firstValue = result
        }
        isNewOp = true
    }
}
