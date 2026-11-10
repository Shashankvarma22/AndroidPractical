package com.example.marketmania

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubApiService {
    @GET("quote")
    fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): Call<FinnhubQuote>
}
