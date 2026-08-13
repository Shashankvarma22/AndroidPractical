package com.example.calcdynamicloading

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val masterLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
        }

        val row1 = InputRow(this, "Enter Number 1:")
        val row2 = InputRow(this, "Enter Number 2:")

        masterLayout.addView(row1)
        masterLayout.addView(row2)

        val addBtn = Button(this).apply {
            text = "+"
            textSize = 24f
        }

        val subBtn = Button(this).apply {
            text = "-"
            textSize = 24f
        }

        val mulBtn = Button(this).apply {
            text = "*"
            textSize = 24f
        }

        val divBtn = Button(this).apply {
            text = "/"
            textSize = 24f
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 10, 10, 10)

            addView(addBtn)
            addView(subBtn)
            addView(mulBtn)
            addView(divBtn)
        }

        masterLayout.addView(btnRow)

        val resRow = InputRow(this, "Result:")
        resRow.numField.isEnabled = false

        masterLayout.addView(resRow)

        addBtn.setOnClickListener {

            val n1 = row1.numField.text.toString().toDoubleOrNull() ?: 0.0
            val n2 = row2.numField.text.toString().toDoubleOrNull() ?: 0.0

            resRow.numField.setText((n1 + n2).toString())
        }

        subBtn.setOnClickListener {

            val n1 = row1.numField.text.toString().toDoubleOrNull() ?: 0.0
            val n2 = row2.numField.text.toString().toDoubleOrNull() ?: 0.0

            resRow.numField.setText((n1 - n2).toString())
        }

        mulBtn.setOnClickListener {

            val n1 = row1.numField.text.toString().toDoubleOrNull() ?: 0.0
            val n2 = row2.numField.text.toString().toDoubleOrNull() ?: 0.0

            resRow.numField.setText((n1 * n2).toString())
        }

        divBtn.setOnClickListener {

            val n1 = row1.numField.text.toString().toDoubleOrNull() ?: 0.0
            val n2 = row2.numField.text.toString().toDoubleOrNull() ?: 0.0

            if (n2 == 0.0) {
                resRow.numField.setText("Error")
            } else {
                resRow.numField.setText((n1 / n2).toString())
            }
        }

        setContentView(masterLayout)
    }
}