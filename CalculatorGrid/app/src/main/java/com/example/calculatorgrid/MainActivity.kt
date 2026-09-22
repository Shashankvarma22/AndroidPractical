package com.example.calculatorgrid

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView
    private lateinit var sbDecimal: SeekBar
    private lateinit var sbRounding: SeekBar
    private lateinit var swGTSet: SwitchCompat

    private var firstValue = BigDecimal.ZERO
    private var operator = ""
    private var isNewOp = true
    private var memory = BigDecimal.ZERO
    private var grandTotal = BigDecimal.ZERO
    private var taxRate = BigDecimal.ZERO
    private val resultsList = mutableListOf<BigDecimal>()

    private var lastCost: BigDecimal? = null
    private var lastSell: BigDecimal? = null
    private var lastMgn: BigDecimal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)
        sbDecimal = findViewById(R.id.sbDecimal)
        sbRounding = findViewById(R.id.sbRounding)
        swGTSet = findViewById(R.id.swGTSet)
        val sbPower = findViewById<SeekBar>(R.id.sbPower)

        val buttons = mutableListOf<Button>()
        val rootLayout = findViewById<LinearLayout>(R.id.rootLayout)
        collectButtons(rootLayout, buttons)

        sbPower.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val enabled = p1 > 0
                buttons.forEach { it.isEnabled = enabled }
                tvDisplay.text = if (enabled) "0" else ""
                resetCalculatorState()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        setupInputListeners()
        setupOperatorListeners()
        setupFunctionListeners()
        setupBusinessListeners()
        setupMemoryListeners()
        setupTaxListeners()
        
        findViewById<Button>(R.id.btnReceipt).setOnClickListener { /* Dummy listener */ }
    }

    private fun collectButtons(view: View, list: MutableList<Button>) {
        when (view) {
            is Button -> list.add(view)
            is ViewGroup -> for (i in 0 until view.childCount) collectButtons(view.getChildAt(i), list)
        }
    }

    private fun resetCalculatorState() {
        firstValue = BigDecimal.ZERO
        operator = ""
        isNewOp = true
        memory = BigDecimal.ZERO
        grandTotal = BigDecimal.ZERO
        resultsList.clear()
        lastCost = null
        lastSell = null
        lastMgn = null
    }

    private fun setupInputListeners() {
        val numIds = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDot
        )
        numIds.forEach { id ->
            findViewById<Button>(id).setOnClickListener {
                val char = (it as Button).text.toString()
                if (isNewOp) {
                    tvDisplay.text = if (char == ".") "0." else char
                    isNewOp = false
                } else {
                    if (char == "." && tvDisplay.text.contains(".")) return@setOnClickListener
                    tvDisplay.append(char)
                }
            }
        }

        findViewById<Button>(R.id.btn00).setOnClickListener {
            if (isNewOp) {
                tvDisplay.text = "0"
                isNewOp = false
            } else {
                tvDisplay.append("00")
            }
        }
    }

    private fun setupOperatorListeners() {
        val ops = mapOf(
            R.id.btnAdd to "+", R.id.btnSub to "-",
            R.id.btnMul to "×", R.id.btnDiv to "÷"
        )
        ops.forEach { (id, op) ->
            findViewById<Button>(id).setOnClickListener {
                if (!isNewOp && operator.isNotEmpty()) performCalculation()
                firstValue = getDisplayValue()
                operator = op
                isNewOp = true
            }
        }

        findViewById<Button>(R.id.btnEqual).setOnClickListener {
            performCalculation(finalize = true)
            operator = ""
        }
    }

    private fun setupFunctionListeners() {
        findViewById<Button>(R.id.btnC).setOnClickListener {
            tvDisplay.text = "0"
            firstValue = BigDecimal.ZERO
            operator = ""
            isNewOp = true
        }

        findViewById<Button>(R.id.btnPaperFeed).setOnClickListener {
            val current = tvDisplay.text.toString()
            tvDisplay.text = if (current.length > 1) current.dropLast(1) else "0"
        }

        findViewById<Button>(R.id.btnPercent).setOnClickListener {
            updateDisplay(getDisplayValue().divide(BigDecimal("100"), 10, RoundingMode.HALF_UP))
            isNewOp = true
        }

        findViewById<Button>(R.id.btnPlusMinus).setOnClickListener {
            updateDisplay(getDisplayValue().negate())
        }

        findViewById<Button>(R.id.btnTotal).setOnClickListener {
            performCalculation(finalize = true)
            operator = ""
        }

        findViewById<Button>(R.id.btnSubtotal).setOnClickListener {
            performCalculation(finalize = false)
            isNewOp = true
        }

        findViewById<Button>(R.id.btnAvg).setOnClickListener {
            if (resultsList.isNotEmpty()) {
                val sum = resultsList.reduce { acc, decimal -> acc.add(decimal) }
                updateDisplay(sum.divide(BigDecimal(resultsList.size), 10, RoundingMode.HALF_UP))
            }
            isNewOp = true
        }

        findViewById<Button>(R.id.btnGT).setOnClickListener {
            updateDisplay(grandTotal)
            isNewOp = true
        }
        
        findViewById<Button>(R.id.btnChange).setOnClickListener {
            val received = getDisplayValue()
            updateDisplay(received.subtract(firstValue))
            isNewOp = true
        }
    }

    private fun setupBusinessListeners() {
        findViewById<Button>(R.id.btnCost).setOnClickListener {
            val v = getDisplayValue()
            if (v != BigDecimal.ZERO) lastCost = v
            else if (lastSell != null && lastMgn != null) {
                val cost = lastSell!!.multiply(BigDecimal.ONE.subtract(lastMgn!!.divide(BigDecimal("100"), 10, RoundingMode.HALF_UP)))
                updateDisplay(cost)
            }
            isNewOp = true
        }
        findViewById<Button>(R.id.btnSell).setOnClickListener {
            val v = getDisplayValue()
            if (v != BigDecimal.ZERO) lastSell = v
            else if (lastCost != null && lastMgn != null) {
                val sell = lastCost!!.divide(BigDecimal.ONE.subtract(lastMgn!!.divide(BigDecimal("100"), 10, RoundingMode.HALF_UP)), 10, RoundingMode.HALF_UP)
                updateDisplay(sell)
            }
            isNewOp = true
        }
        findViewById<Button>(R.id.btnMgn).setOnClickListener {
            val v = getDisplayValue()
            if (v != BigDecimal.ZERO) lastMgn = v
            else if (lastSell != null && lastCost != null && lastSell != BigDecimal.ZERO) {
                val mgn = lastSell!!.subtract(lastCost!!).divide(lastSell!!, 10, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
                updateDisplay(mgn)
            }
            isNewOp = true
        }
    }

    private fun setupMemoryListeners() {
        findViewById<Button>(R.id.btnMPlus).setOnClickListener { memory = memory.add(getDisplayValue()); isNewOp = true }
        findViewById<Button>(R.id.btnMMinus).setOnClickListener { memory = memory.subtract(getDisplayValue()); isNewOp = true }
        findViewById<Button>(R.id.btnMR).setOnClickListener { updateDisplay(memory); isNewOp = true }
        findViewById<Button>(R.id.btnMC).setOnClickListener { memory = BigDecimal.ZERO; isNewOp = true }
    }

    private fun setupTaxListeners() {
        findViewById<Button>(R.id.btnTaxPlus).setOnClickListener {
            val v = getDisplayValue()
            if (swGTSet.isChecked) taxRate = v
            else updateDisplay(v.multiply(BigDecimal.ONE.add(taxRate.divide(BigDecimal("100"), 10, RoundingMode.HALF_UP))))
            isNewOp = true
        }
        findViewById<Button>(R.id.btnTaxMinus).setOnClickListener {
            val v = getDisplayValue()
            if (swGTSet.isChecked) taxRate = v
            else updateDisplay(v.divide(BigDecimal.ONE.add(taxRate.divide(BigDecimal("100"), 10, RoundingMode.HALF_UP)), 10, RoundingMode.HALF_UP))
            isNewOp = true
        }
    }

    private fun getDisplayValue(): BigDecimal = tvDisplay.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO

    private fun performCalculation(finalize: Boolean = false) {
        val secondValue = getDisplayValue()
        val result = when (operator) {
            "+" -> firstValue.add(secondValue)
            "-" -> firstValue.subtract(secondValue)
            "×" -> firstValue.multiply(secondValue)
            "÷" -> if (secondValue != BigDecimal.ZERO) firstValue.divide(secondValue, 10, RoundingMode.HALF_UP) else null
            else -> secondValue
        }

        if (result == null) {
            tvDisplay.text = "Error"
        } else {
            updateDisplay(result)
            firstValue = result
            if (finalize) {
                grandTotal = grandTotal.add(result)
                resultsList.add(result)
            }
        }
        isNewOp = true
    }

    private fun updateDisplay(value: BigDecimal) {
        val decPos = when (sbDecimal.progress) {
            0 -> -1 // F
            1 -> 6
            2 -> 3
            3 -> 2
            4 -> 1
            5 -> 0
            else -> 2 // A
        }

        val roundMode = when (sbRounding.progress) {
            0 -> RoundingMode.UP
            1 -> RoundingMode.HALF_UP
            else -> RoundingMode.DOWN
        }

        tvDisplay.text = if (decPos == -1) {
            value.stripTrailingZeros().toPlainString()
        } else {
            value.setScale(decPos, roundMode).toPlainString()
        }
    }
}