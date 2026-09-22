package com.example.retrofitdemo

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("posts")
    fun getPosts(): Call<List<PostObject>>

    @POST("posts")
    fun createPost(@Body post: CreatePostObject): Call<PostObject>
}