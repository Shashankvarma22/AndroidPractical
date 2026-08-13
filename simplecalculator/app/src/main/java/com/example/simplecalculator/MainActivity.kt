package com.example.simplecalculator

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val num1Text: EditText = findViewById(R.id.num1Text)
        val num2Text: EditText = findViewById(R.id.num2Text)
        val resText: EditText = findViewById(R.id.resText)

        val addBtn: Button = findViewById(R.id.button7)
        val subBtn: Button = findViewById(R.id.button8)
        val mulBtn: Button = findViewById(R.id.button9)
        val divBtn: Button = findViewById(R.id.button10)

        addBtn.setOnClickListener {
            calculate(num1Text, num2Text, resText, "+")
        }
        subBtn.setOnClickListener {
            calculate(num1Text, num2Text, resText, "-")
        }

        mulBtn.setOnClickListener {
            calculate(num1Text, num2Text, resText, "*")
        }

        divBtn.setOnClickListener {
            calculate(num1Text, num2Text, resText, "/")
        }
    }

    private fun calculate(num1ET: EditText, num2ET: EditText, resET: EditText, operator: String) {
        val s1 = num1ET.text.toString()
        val s2 = num2ET.text.toString()

        if (s1.isEmpty() || s2.isEmpty()) {
            Toast.makeText(this, "Please enter both numbers", Toast.LENGTH_SHORT).show()
            return
        }

        val n1 = s1.toDouble()
        val n2 = s2.toDouble()
        var res = 0.0

        when (operator) {
            "+" -> res = n1 + n2
            "-" -> res = n1 - n2
            "*" -> res = n1 * n2
            "/" -> {
                if (n2 != 0.0) {
                    res = n1 / n2
                } else {
                    Toast.makeText(this,
                        "Cannot divide by zero",
                        Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }
        resET.setText(res.toString())
    }
}