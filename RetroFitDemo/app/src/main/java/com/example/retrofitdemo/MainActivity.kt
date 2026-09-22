package com.example.retrofitdemo

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Root LinearLayout
        val linearLayout = LinearLayout(this)

        linearLayout.orientation = LinearLayout.VERTICAL

        // ADD DATA button to navigate to AddPostActivity
        val addButton = android.widget.Button(this)
        addButton.text = "ADD DATA"
        val addButtonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        addButtonParams.setMargins(16, 16, 16, 16)
        addButton.layoutParams = addButtonParams
        linearLayout.addView(addButton)

        addButton.setOnClickListener {
            val intent = android.content.Intent(this, AddPostActivity::class.java)
            startActivity(intent)
        }

        // Spinner for filtering by User ID
        val spinner = android.widget.Spinner(this)
        val spinnerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        spinnerParams.setMargins(16, 16, 16, 16)
        spinner.layoutParams = spinnerParams
        linearLayout.addView(spinner)

        // ScrollView
        val scrollView = ScrollView(this)

        val horizontalScrollView =
            HorizontalScrollView(this)

        // TableLayout
        val tableLayout = TableLayout(this)

        tableLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Put Horizontal Scroll View inside ScrollView
        scrollView.addView(horizontalScrollView)

        //Put Table Layout inside Horizontal Scroll View
        horizontalScrollView.addView(tableLayout)

        // Put ScrollView inside LinearLayout
        linearLayout.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Set root layout as Activity content
        setContentView(linearLayout)

        // Create table header
        addHeaderRow(tableLayout)

        // Retrofit GET request
        RetrofitClient.api.getPosts()
            .enqueue(object : Callback<List<PostObject>> {

                override fun onResponse(
                    call: Call<List<PostObject>>,
                    response: Response<List<PostObject>>
                ) {

                    if (response.isSuccessful) {

                        val posts = response.body() ?: emptyList()

                        Log.d(
                            "RETROFIT",
                            "Posts received: ${posts.size}"
                        )

                        // Extract unique userIds and sort them
                        val userIds = posts.map { it.userId }.distinct().sorted()
                        val spinnerItems = mutableListOf("All Users")
                        spinnerItems.addAll(userIds.map { it.toString() })

                        // Set up the Spinner Adapter
                        val adapter = android.widget.ArrayAdapter(
                            this@MainActivity,
                            android.R.layout.simple_spinner_item,
                            spinnerItems
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinner.adapter = adapter

                        // Set Item Selected Listener to handle filtering
                        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: android.widget.AdapterView<*>?,
                                view: android.view.View?,
                                position: Int,
                                id: Long
                            ) {
                                val selectedItem = spinnerItems[position]
                                
                                // Clear current table contents and re-add header
                                tableLayout.removeAllViews()
                                addHeaderRow(tableLayout)

                                if (selectedItem == "All Users") {
                                    posts.forEach { post ->
                                        addPostRow(tableLayout, post)
                                    }
                                } else {
                                    val selectedUserId = selectedItem.toIntOrNull()
                                    posts.filter { it.userId == selectedUserId }.forEach { post ->
                                        addPostRow(tableLayout, post)
                                    }
                                }
                            }

                            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                        }

                    } else {

                        Log.e(
                            "RETROFIT",
                            "HTTP Error: ${response.code()}"
                        )
                    }
                }

                override fun onFailure(
                    call: Call<List<PostObject>>,
                    t: Throwable
                ) {

                    Log.e(
                        "RETROFIT",
                        "Request failed",
                        t
                    )
                }
            })
    }


    private fun addHeaderRow(
        tableLayout: TableLayout
    ) {

        val row = TableRow(this)

        row.gravity = Gravity.CENTER

        addTextView(
            row,
            "ID",
            true
        )

        addTextView(
            row,
            "USER ID",
            true
        )

        addTextView(
            row,
            "TITLE",
            true
        )

        addTextView(
            row,
            "BODY",
            true
        )

        tableLayout.addView(row)
    }


    private fun addPostRow(
        tableLayout: TableLayout,
        post: PostObject
    ) {

        val row = TableRow(this)

        row.gravity = Gravity.CENTER

        // Show popup with full details when row is clicked
        row.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Post Details")
                .setMessage("ID: ${post.id}\nUser ID: ${post.userId}\n\nTitle: ${post.title}\n\nBody: ${post.body}")
                .setPositiveButton("CLOSE", null)
                .show()
        }

        addTextView(
            row,
            post.id.toString(),
            false
        )

        addTextView(
            row,
            post.userId.toString(),
            false
        )

        addTextView(
            row,
            post.title,
            false
        )

        addTextView(
            row,
            post.body,
            false
        )

        tableLayout.addView(row)
    }


    private fun addTextView(
        row: TableRow,
        text: String,
        isHeader: Boolean
    ) {

        val textView = TextView(this)

        textView.text = text

        textView.textSize = 18f

        textView.setTextColor(Color.BLACK)

        textView.gravity = Gravity.CENTER

        if (isHeader) {
            textView.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val params = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(
            16,
            16,
            16,
            16
        )

        textView.layoutParams = params

        row.addView(textView)
    }
}