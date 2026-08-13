package com.example.calcdynamicloading

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class InputRow(context: Context, label: String) : LinearLayout(context) {

    val labView: TextView = TextView(context).apply {
        text = label
        textSize = 24f
        setPadding(10, 10, 10, 10)
    }

    val numField: EditText = EditText(context).apply {
        hint = "12345678"
        inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        layoutParams = LayoutParams(
            0,
            LayoutParams.WRAP_CONTENT,
            1f
        )
    }

    init {
        orientation = HORIZONTAL
        setPadding(10, 10, 10, 10)

        addView(labView)
        addView(numField)
    }
}