package com.example.marketmania

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class StockWebSocketManager(
    private val apiKey: String,
    private val onStatusChanged: (String) -> Unit,
    private val onTradeReceived: (String, Double, Long?, Double?) -> Unit
) {
    private val TAG = "FINNHUB_WS"
    private val client = OkHttpClient()
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var isConnecting = false

    fun connect(symbols: List<String>) {
        if (webSocket != null || isConnecting || isConnected) {
            Log.d(TAG, "WebSocket already active/connecting.")
            return
        }

        isConnecting = true
        Log.d(TAG, "Connecting to Finnhub...")
        notifyStatus("Connecting")

        val request = Request.Builder()
            .url("wss://ws.finnhub.io?token=$apiKey")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connection opened")
                isConnecting = false
                isConnected = true
                notifyStatus("Connected")

                for (symbol in symbols) {
                    sendSubscribe(webSocket, symbol)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message received: $text")
                try {
                    val response = gson.fromJson(text, FinnhubWebSocketResponse::class.java)
                    if (response?.type == "trade" && !response.data.isNullOrEmpty()) {
                        for (trade in response.data) {
                            val symbol = trade.symbol
                            val price = trade.price
                            val timestamp = trade.timestamp
                            val volume = trade.volume
                            if (symbol != null && price != null) {
                                Log.d(TAG, "Trade $symbol price=$price timestamp=$timestamp volume=$volume")
                                mainHandler.post {
                                    onTradeReceived(symbol, price, timestamp, volume)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing WebSocket message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing...")
                isConnected = false
                isConnecting = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed...")
                this@StockWebSocketManager.webSocket = null
                isConnected = false
                isConnecting = false
                notifyStatus("Disconnected")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}", t)
                this@StockWebSocketManager.webSocket = null
                isConnected = false
                isConnecting = false
                notifyStatus("Disconnected")
            }
        })
    }

    private fun sendSubscribe(ws: WebSocket, symbol: String) {
        val json = "{\"type\":\"subscribe\",\"symbol\":\"$symbol\"}"
        if (ws.send(json)) {
            Log.d(TAG, "Subscribed to $symbol")
        } else {
            Log.e(TAG, "Failed to send subscribe for $symbol")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "App closed")
        webSocket = null
        isConnected = false
        isConnecting = false
        notifyStatus("Disconnected")
    }

    private fun notifyStatus(status: String) {
        mainHandler.post {
            onStatusChanged(status)
        }
    }
}
