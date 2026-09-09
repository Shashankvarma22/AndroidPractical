package com.example.layoutdemo

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.layoutdemo.fragments.*

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

        // -------------------------
        // FIND VIEWS
        // -------------------------
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup)
        val checkOne = findViewById<CheckBox>(R.id.checkOne)
        val checkTwo = findViewById<CheckBox>(R.id.checkTwo)
        val checkThree = findViewById<CheckBox>(R.id.checkThree)
        val checkFour = findViewById<CheckBox>(R.id.checkFour)
        val spinner = findViewById<Spinner>(R.id.spinner)

        val checkBoxes = listOf(checkOne, checkTwo, checkThree, checkFour)

        // -------------------------
        // SPINNER SETUP
        // -------------------------
        val spinnerData = arrayOf("Table view", "Grid view", "Scroll view & card view", "Recycler view")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerData)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        // ==================================================
        // UNIFIED SELECTION LOGIC
        // ==================================================
        fun updateSelection(index: Int) {
            // Update Fragment
            val fragment: Fragment = when (index) {
                0 -> TableViewFragment()
                1 -> GridViewFragment()
                2 -> ScrollViewFragment()
                3 -> RecycleViewFragment()
                else -> TableViewFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()

            // Update CheckBoxes
            checkBoxes.forEachIndexed { i, cb -> cb.isChecked = (i == index) }

            // Sync RadioGroup
            val radioId = when (index) {
                0 -> R.id.radioOne
                1 -> R.id.radioTwo
                2 -> R.id.radioThree
                3 -> R.id.radioFour
                else -> -1
            }
            if (radioGroup.checkedRadioButtonId != radioId) {
                radioGroup.check(radioId)
            }

            // Sync Spinner
            if (spinner.selectedItemPosition != index) {
                spinner.setSelection(index)
            }
        }

        // -------------------------
        // LISTENERS
        // -------------------------
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val index = when (checkedId) {
                R.id.radioOne -> 0
                R.id.radioTwo -> 1
                R.id.radioThree -> 2
                R.id.radioFour -> 3
                else -> -1
            }
            if (index != -1) updateSelection(index)
        }

        checkBoxes.forEachIndexed { index, checkBox ->
            checkBox.setOnCheckedChangeListener { button, checked ->
                if (button.isPressed) {
                    if (checked) {
                        updateSelection(index)
                    } else {
                        button.isChecked = true
                    }
                }
            }
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateSelection(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // -------------------------
        // DEFAULT SELECTION
        // -------------------------
        if (savedInstanceState == null) {
            updateSelection(0)
        }
    }
}
