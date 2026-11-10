package com.example.marketmania

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private val TAG_API = "FINNHUB_API"
    private val MAX_POINTS_PER_STOCK = 300

    private lateinit var tvSelectedSymbol: TextView
    private lateinit var tvSelectedName: TextView
    private lateinit var tvCurrentPrice: TextView
    private lateinit var tvPriceChange: TextView
    private lateinit var tvMarketStatus: TextView
    private lateinit var tvWsStatus: TextView
    private lateinit var lineChart: LineChart
    private lateinit var rvWatchlist: RecyclerView

    private lateinit var finnhubApi: FinnhubApiService
    private lateinit var webSocketManager: StockWebSocketManager
    private lateinit var watchlistAdapter: WatchlistAdapter

    private val watchlistItems = listOf(
        StockItem("NVDA", "NVIDIA Corp."),
        StockItem("AAPL", "Apple Inc."),
        StockItem("MSFT", "Microsoft Corp."),
        StockItem("AMZN", "Amazon.com Inc.")
    )

    private var currentStock = watchlistItems[0]

    // In-memory trade histories and entry index trackers for each stock
    private val stockHistories = mutableMapOf<String, MutableList<Entry>>()
    private val stockEntryIndices = mutableMapOf<String, Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize empty histories for all watchlist items
        watchlistItems.forEach { stock ->
            stockHistories[stock.symbol] = mutableListOf()
            stockEntryIndices[stock.symbol] = 0f
        }

        initViews()
        setupRetrofit()
        setupChart()
        setupWatchlist()
        updateMarketStatus()

        // Fetch initial current quotes via Finnhub REST
        loadAllWatchlistQuotes()

        // Connect WebSocket once and subscribe to all 4 stocks
        setupWebSocket()
    }

    private fun initViews() {
        tvSelectedSymbol = findViewById(R.id.tvSelectedSymbol)
        tvSelectedName = findViewById(R.id.tvSelectedName)
        tvCurrentPrice = findViewById(R.id.tvCurrentPrice)
        tvPriceChange = findViewById(R.id.tvPriceChange)
        tvMarketStatus = findViewById(R.id.tvMarketStatus)
        tvWsStatus = findViewById(R.id.tvWsStatus)
        lineChart = findViewById(R.id.lineChart)
        rvWatchlist = findViewById(R.id.rvWatchlist)

        tvSelectedSymbol.text = currentStock.symbol
        tvSelectedName.text = currentStock.companyName
    }

    private fun updateMarketStatus() {
        val isOpen = checkUsMarketOpen()
        if (isOpen) {
            tvMarketStatus.text = "MARKET OPEN"
            tvMarketStatus.setTextColor(Color.parseColor("#22C55E"))
        } else {
            tvMarketStatus.text = "MARKET CLOSED"
            tvMarketStatus.setTextColor(Color.parseColor("#94A3B8"))
        }
    }

    private fun checkUsMarketOpen(): Boolean {
        try {
            val tz = TimeZone.getTimeZone("America/New_York")
            val cal = Calendar.getInstance(tz)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                return false
            }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val minuteOfDay = hour * 60 + minute
            val openMinute = 9 * 60 + 30
            val closeMinute = 16 * 60
            return minuteOfDay in openMinute..closeMinute
        } catch (_: Exception) {
            return false
        }
    }

    private fun setupRetrofit() {
        val finnhubRetrofit = Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        finnhubApi = finnhubRetrofit.create(FinnhubApiService::class.java)
    }

    private fun setupChart() {
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.legend.isEnabled = false
        lineChart.setDrawGridBackground(false)
        lineChart.setNoDataText("Waiting for live data...")
        lineChart.setNoDataTextColor(Color.parseColor("#94A3B8"))

        lineChart.xAxis.setDrawGridLines(false)
        lineChart.xAxis.textColor = Color.parseColor("#94A3B8")

        lineChart.axisLeft.textColor = Color.parseColor("#94A3B8")
        lineChart.axisLeft.setDrawGridLines(true)
        lineChart.axisLeft.gridColor = Color.parseColor("#334155")

        lineChart.axisRight.isEnabled = false
    }

    private fun renderChartForCurrentStock() {
        val entries = stockHistories[currentStock.symbol] ?: emptyList()
        if (entries.isEmpty()) {
            lineChart.clear()
            lineChart.setNoDataText("Waiting for live data...")
            lineChart.invalidate()
            return
        }

        val dataSet: LineDataSet
        if (lineChart.data != null && lineChart.data.dataSetCount > 0) {
            dataSet = lineChart.data.getDataSetByIndex(0) as LineDataSet
            dataSet.values = entries
            lineChart.data.notifyDataChanged()
            lineChart.notifyDataSetChanged()
        } else {
            dataSet = LineDataSet(entries, "Stock Price")
            dataSet.color = Color.parseColor("#3B82F6")
            dataSet.valueTextColor = Color.TRANSPARENT
            dataSet.lineWidth = 2.5f
            dataSet.setDrawCircles(false)
            dataSet.setDrawValues(false)
            dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

            val lineData = LineData(dataSet)
            lineChart.data = lineData
        }
        lineChart.invalidate()
    }

    private fun setupWatchlist() {
        watchlistAdapter = WatchlistAdapter(watchlistItems) { selectedStock ->
            if (selectedStock.symbol != currentStock.symbol) {
                currentStock = selectedStock
                tvSelectedSymbol.text = currentStock.symbol
                tvSelectedName.text = currentStock.companyName

                // Update display price and change for selected stock if available
                if (currentStock.price > 0) {
                    tvCurrentPrice.text = String.format(Locale.US, "$%.2f", currentStock.price)
                    val changeSign = if (currentStock.changePercent >= 0) "+" else ""
                    tvPriceChange.text = String.format(Locale.US, "%s%.2f%%", changeSign, currentStock.changePercent)
                    if (currentStock.changePercent >= 0) {
                        tvPriceChange.setTextColor(Color.parseColor("#22C55E"))
                    } else {
                        tvPriceChange.setTextColor(Color.parseColor("#EF4444"))
                    }
                }

                // Render existing collected points for the newly selected stock
                renderChartForCurrentStock()
            }
        }
        rvWatchlist.layoutManager = LinearLayoutManager(this)
        rvWatchlist.adapter = watchlistAdapter
    }

    private fun setupWebSocket() {
        webSocketManager = StockWebSocketManager(
            apiKey = BuildConfig.FINNHUB_API_KEY,
            onStatusChanged = { status ->
                tvWsStatus.text = "WS: $status"
            },
            onTradeReceived = { symbol, price, _, _ ->
                // Store live trade in stock history continuously
                val index = stockEntryIndices.getOrDefault(symbol, 0f)
                val entries = stockHistories.getOrPut(symbol) { mutableListOf() }
                entries.add(Entry(index, price.toFloat()))
                stockEntryIndices[symbol] = index + 1f

                if (entries.size > MAX_POINTS_PER_STOCK) {
                    entries.removeAt(0)
                }

                // Update watchlist UI
                watchlistAdapter.updateStockPrice(symbol, price)

                // If trade is for currently selected stock, update main header and chart
                if (symbol == currentStock.symbol) {
                    tvCurrentPrice.text = String.format(Locale.US, "$%.2f", price)
                    renderChartForCurrentStock()
                }
            }
        )
        // Connect once and subscribe to all watchlist symbols
        webSocketManager.connect(watchlistItems.map { it.symbol })
    }

    private fun loadAllWatchlistQuotes() {
        watchlistItems.forEach { stock ->
            loadStockQuote(stock.symbol)
        }
    }

    private fun loadStockQuote(symbol: String) {
        Log.d(TAG_API, "Loading quote for $symbol")
        finnhubApi.getQuote(symbol, BuildConfig.FINNHUB_API_KEY)
            .enqueue(object : Callback<FinnhubQuote> {
                override fun onResponse(
                    call: Call<FinnhubQuote>,
                    response: Response<FinnhubQuote>
                ) {
                    if (response.isSuccessful) {
                        val quote = response.body()
                        if (quote != null) {
                            val price = quote.currentPrice ?: 0.0
                            val change = quote.change ?: 0.0
                            val percent = quote.percentChange ?: 0.0

                            Log.d(TAG_API, "Quote success $symbol price=$price")

                            if (symbol == currentStock.symbol) {
                                tvCurrentPrice.text = String.format(Locale.US, "$%.2f", price)

                                val changeSign = if (change >= 0) "+" else ""
                                tvPriceChange.text = String.format(
                                    Locale.US,
                                    "%s%.2f (%s%.2f%%)",
                                    changeSign, change, changeSign, percent
                                )

                                if (change >= 0) {
                                    tvPriceChange.setTextColor(Color.parseColor("#22C55E"))
                                } else {
                                    tvPriceChange.setTextColor(Color.parseColor("#EF4444"))
                                }
                            }

                            watchlistAdapter.updateStockQuote(symbol, price, percent)
                        } else {
                            Log.e(TAG_API, "Quote body is null for $symbol")
                        }
                    } else {
                        Log.e(TAG_API, "Quote HTTP error $symbol code=${response.code()}")
                    }
                }

                override fun onFailure(call: Call<FinnhubQuote>, t: Throwable) {
                    Log.e(TAG_API, "Quote failure $symbol error=${t.message}", t)
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.disconnect()
    }
}