 package com.example.marketmania

import com.google.gson.annotations.SerializedName

data class FinnhubQuote(
    @SerializedName("c") val currentPrice: Double? = 0.0,
    @SerializedName("d") val change: Double? = 0.0,
    @SerializedName("dp") val percentChange: Double? = 0.0,
    @SerializedName("h") val highPrice: Double? = 0.0,
    @SerializedName("l") val lowPrice: Double? = 0.0,
    @SerializedName("o") val openPrice: Double? = 0.0,
    @SerializedName("pc") val previousClose: Double? = 0.0
)

data class FinnhubWebSocketResponse(
    @SerializedName("type") val type: String? = null,
    @SerializedName("data") val data: List<FinnhubTrade>? = null
)

data class FinnhubTrade(
    @SerializedName("s") val symbol: String? = null,
    @SerializedName("p") val price: Double? = null,
    @SerializedName("t") val timestamp: Long? = null,
    @SerializedName("v") val volume: Double? = null
)

data class StockItem(
    val symbol: String,
    val companyName: String,
    var price: Double = 0.0,
    var changePercent: Double = 0.0
)
