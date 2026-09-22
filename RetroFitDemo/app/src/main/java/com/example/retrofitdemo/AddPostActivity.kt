package com.example.retrofitdemo

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddPostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Root LinearLayout
        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(
            40,
            40,
            40,
            40
        )
        // Heading
        val heading = android.widget.TextView(this)
        heading.text = "ADD NEW POST"
        heading.textSize = 24f
        heading.gravity = Gravity.CENTER
        heading.setPadding(
            0,
            0,
            0,
            30
        )
        // User ID
        val userIdEditText = EditText(this)
        userIdEditText.hint = "User ID"
        userIdEditText.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER
        // Title
        val titleEditText = EditText(this)
        titleEditText.hint = "Title"
        // Body
        val bodyEditText = EditText(this)
        bodyEditText.hint = "Body"
        bodyEditText.minLines = 4
        bodyEditText.gravity = Gravity.TOP
        // Submit button
        val submitButton = Button(this)
        submitButton.text = "SUBMIT"
        // Cancel button
        val cancelButton = Button(this)
        cancelButton.text = "CANCEL"
        // Add views to LinearLayout

        linearLayout.addView(
            heading,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        linearLayout.addView(
            userIdEditText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        linearLayout.addView(
            titleEditText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        linearLayout.addView(
            bodyEditText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        linearLayout.addView(
            submitButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        linearLayout.addView(
            cancelButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // Set Activity content
        setContentView(linearLayout)

        // CANCEL
        cancelButton.setOnClickListener {
            finish()
        }


        // SUBMIT
        submitButton.setOnClickListener {
            val userIdText =
                userIdEditText.text.toString()
            val title =
                titleEditText.text.toString()
            val body =
                bodyEditText.text.toString()

            // Simple validation
            if (userIdText.isBlank() ||
                title.isBlank() ||
                body.isBlank()
            ) {
                Toast.makeText(
                    this,
                    "Please fill all fields",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val userId = userIdText.toInt()
            // Create POST object
            val newPost = CreatePostObject(
                userId = userId,
                title = title,
                body = body
            )

            // POST request
            RetrofitClient.api.createPost(newPost)
                .enqueue(object : Callback<PostObject> {

                    override fun onResponse(
                        call: Call<PostObject>,
                        response: Response<PostObject>
                    ) {

                        if (response.isSuccessful) {

                            val createdPost =
                                response.body()

                            Log.d(
                                "POST",
                                "POST successful"
                            )

                            Log.d(
                                "POST",
                                "Created ID: ${createdPost?.id}"
                            )

                            Log.d(
                                "POST",
                                "Title: ${createdPost?.title}"
                            )

                            Toast.makeText(
                                this@AddPostActivity,
                                "Data submitted successfully",
                                Toast.LENGTH_SHORT
                            ).show()


                            // Return to MainActivity
                            finish()

                        } else {

                            Log.e(
                                "POST",
                                "HTTP Error: ${response.code()}"
                            )

                            Toast.makeText(
                                this@AddPostActivity,
                                "POST failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<PostObject>,
                        t: Throwable
                    ) {
                        Log.e(
                            "POST",
                            "Request failed",
                            t
                        )

                        Toast.makeText(
                            this@AddPostActivity,
                            "Network error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }
}