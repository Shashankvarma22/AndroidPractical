package com.example.volleydemo

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class MainActivity : AppCompatActivity() {

    private lateinit var etMovieName: EditText
    private lateinit var btnSearch: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvErrorMessage: TextView

    // Search Results List Views
    private lateinit var layoutSearchResults: View
    private lateinit var tvResultsCount: TextView
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var btnLoadMore: Button
    private lateinit var pbLoadMore: ProgressBar

    // Movie Details Views
    private lateinit var scrollViewDetails: ScrollView
    private lateinit var btnBackToResults: Button
    private lateinit var ivPoster: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvYear: TextView
    private lateinit var tvGenre: TextView
    private lateinit var tvImdbRating: TextView
    private lateinit var tvImdbVotes: TextView
    private lateinit var tvDirector: TextView
    private lateinit var tvActors: TextView
    private lateinit var tvPlot: TextView

    private lateinit var requestQueue: RequestQueue
    private lateinit var movieAdapter: MovieAdapter
    private val movieList = mutableListOf<MovieItem>()

    private var currentQuery: String = ""
    private var currentPage: Int = 1
    private var totalResults: Int = 0
    private var currentDetailsImdbId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Volley RequestQueue once and reuse it across all requests
        requestQueue = Volley.newRequestQueue(this)

        // Bind Views
        etMovieName = findViewById(R.id.etMovieName)
        btnSearch = findViewById(R.id.btnSearch)
        progressBar = findViewById(R.id.progressBar)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)

        layoutSearchResults = findViewById(R.id.layoutSearchResults)
        tvResultsCount = findViewById(R.id.tvResultsCount)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        btnLoadMore = findViewById(R.id.btnLoadMore)
        pbLoadMore = findViewById(R.id.pbLoadMore)

        scrollViewDetails = findViewById(R.id.scrollViewDetails)
        btnBackToResults = findViewById(R.id.btnBackToResults)
        ivPoster = findViewById(R.id.ivPoster)
        tvTitle = findViewById(R.id.tvTitle)
        tvYear = findViewById(R.id.tvYear)
        tvGenre = findViewById(R.id.tvGenre)
        tvImdbRating = findViewById(R.id.tvImdbRating)
        tvImdbVotes = findViewById(R.id.tvImdbVotes)
        tvDirector = findViewById(R.id.tvDirector)
        tvActors = findViewById(R.id.tvActors)
        tvPlot = findViewById(R.id.tvPlot)

        // Set up RecyclerView
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        movieAdapter = MovieAdapter(movieList, requestQueue) { selectedMovie ->
            fetchMovieDetails(selectedMovie.imdbID)
        }
        rvSearchResults.adapter = movieAdapter

        // Set up Listeners
        btnSearch.setOnClickListener {
            performNewSearch()
        }

        etMovieName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performNewSearch()
                true
            } else {
                false
            }
        }

        btnLoadMore.setOnClickListener {
            currentPage++
            btnLoadMore.visibility = View.GONE
            pbLoadMore.visibility = View.VISIBLE
            fetchSearchPage(isLoadMore = true)
        }

        btnBackToResults.setOnClickListener {
            showSearchResultsView()
        }

        // Handle System Back button press when full details view is open
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (scrollViewDetails.visibility == View.VISIBLE) {
                    showSearchResultsView()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Restore State if recreated
        if (savedInstanceState != null) {
            currentQuery = savedInstanceState.getString("KEY_QUERY", "")
            currentPage = savedInstanceState.getInt("KEY_PAGE", 1)
            totalResults = savedInstanceState.getInt("KEY_TOTAL_RESULTS", 0)
            currentDetailsImdbId = savedInstanceState.getString("KEY_DETAILS_IMDB_ID", "")
            val isDetailsVisible = savedInstanceState.getBoolean("KEY_DETAILS_VISIBLE", false)

            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            val savedList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getSerializable("KEY_MOVIE_LIST", ArrayList::class.java) as? ArrayList<MovieItem>
            } else {
                savedInstanceState.getSerializable("KEY_MOVIE_LIST") as? ArrayList<MovieItem>
            }

            if (savedList != null) {
                movieList.clear()
                movieList.addAll(savedList)
                movieAdapter.notifyDataSetChanged()
            }

            if (isDetailsVisible && currentDetailsImdbId.isNotBlank()) {
                fetchMovieDetails(currentDetailsImdbId)
            } else if (movieList.isNotEmpty()) {
                layoutSearchResults.visibility = View.VISIBLE
                tvResultsCount.text = getString(R.string.label_results_count, totalResults, currentQuery)
                btnLoadMore.visibility = if (movieList.size < totalResults) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("KEY_QUERY", currentQuery)
        outState.putInt("KEY_PAGE", currentPage)
        outState.putInt("KEY_TOTAL_RESULTS", totalResults)
        outState.putString("KEY_DETAILS_IMDB_ID", currentDetailsImdbId)
        outState.putBoolean("KEY_DETAILS_VISIBLE", scrollViewDetails.visibility == View.VISIBLE)
        outState.putSerializable("KEY_MOVIE_LIST", ArrayList(movieList))
    }

    private fun performNewSearch() {
        val query = etMovieName.text.toString().trim()

        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a movie name", Toast.LENGTH_SHORT).show()
            showError("Please enter a movie title to search.")
            return
        }

        hideKeyboard()

        currentQuery = query
        currentPage = 1
        totalResults = 0
        currentDetailsImdbId = ""
        movieList.clear()
        movieAdapter.notifyDataSetChanged()

        showLoading()
        fetchSearchPage(isLoadMore = false)
    }

    private fun fetchSearchPage(isLoadMore: Boolean) {
        val url = Uri.parse("https://www.omdbapi.com/").buildUpon()
            .appendQueryParameter("apikey", BuildConfig.OMDB_API_KEY)
            .appendQueryParameter("s", currentQuery)
            .appendQueryParameter("r", "json")
            .appendQueryParameter("page", currentPage.toString())
            .build()
            .toString()

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                if (isLoadMore) {
                    pbLoadMore.visibility = View.GONE
                } else {
                    progressBar.visibility = View.GONE
                }

                val isSuccess = response.optString("Response", "False")

                if (isSuccess.equals("True", ignoreCase = true)) {
                    tvErrorMessage.visibility = View.GONE
                    layoutSearchResults.visibility = View.VISIBLE

                    totalResults = response.optString("totalResults", "0").toIntOrNull() ?: 0
                    val searchArray = response.optJSONArray("Search")

                    if (searchArray != null) {
                        for (i in 0 until searchArray.length()) {
                            val itemObj = searchArray.optJSONObject(i) ?: continue
                            val title = formatField(itemObj.optString("Title"))
                            val year = formatField(itemObj.optString("Year"))
                            val imdbID = itemObj.optString("imdbID", "")
                            val type = formatField(itemObj.optString("Type"))
                            val poster = itemObj.optString("Poster", "N/A")

                            movieList.add(MovieItem(title, year, imdbID, type, poster))
                        }
                    }

                    movieAdapter.notifyDataSetChanged()
                    tvResultsCount.text = getString(R.string.label_results_count, totalResults, currentQuery)

                    if (movieList.size < totalResults) {
                        btnLoadMore.visibility = View.VISIBLE
                    } else {
                        btnLoadMore.visibility = View.GONE
                    }
                } else {
                    val errorMsg = response.optString("Error", "No movies found!")
                    if (!isLoadMore) {
                        showError(errorMsg)
                    } else {
                        btnLoadMore.visibility = View.GONE
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            { error ->
                if (isLoadMore) {
                    pbLoadMore.visibility = View.GONE
                    btnLoadMore.visibility = View.VISIBLE
                    Toast.makeText(this, "Failed to load page $currentPage", Toast.LENGTH_SHORT).show()
                } else {
                    progressBar.visibility = View.GONE
                    val errorMsg = when {
                        error.networkResponse != null -> "Server error code: ${error.networkResponse.statusCode}"
                        error.message != null && error.message!!.isNotBlank() -> error.message!!
                        else -> "Network error. Please check your internet connection."
                    }
                    showError(errorMsg)
                }
            }
        )

        requestQueue.add(jsonObjectRequest)
    }

    private fun fetchMovieDetails(imdbID: String) {
        if (imdbID.isBlank()) {
            Toast.makeText(this, "Invalid movie ID", Toast.LENGTH_SHORT).show()
            return
        }

        hideKeyboard()
        currentDetailsImdbId = imdbID
        progressBar.visibility = View.VISIBLE
        tvErrorMessage.visibility = View.GONE
        layoutSearchResults.visibility = View.GONE

        val url = Uri.parse("https://www.omdbapi.com/").buildUpon()
            .appendQueryParameter("apikey", BuildConfig.OMDB_API_KEY)
            .appendQueryParameter("i", imdbID)
            .appendQueryParameter("r", "json")
            .build()
            .toString()

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                progressBar.visibility = View.GONE
                val isSuccess = response.optString("Response", "False")

                if (isSuccess.equals("True", ignoreCase = true)) {
                    scrollViewDetails.visibility = View.VISIBLE

                    val title = formatField(response.optString("Title"))
                    val year = formatField(response.optString("Year"))
                    val genre = formatField(response.optString("Genre"))
                    val imdbRating = formatField(response.optString("imdbRating"))
                    val imdbVotes = formatField(response.optString("imdbVotes"))
                    val director = formatField(response.optString("Director"))
                    val actors = formatField(response.optString("Actors"))
                    val plot = formatField(response.optString("Plot"))

                    tvTitle.text = title
                    tvYear.text = getString(R.string.label_year, year)
                    tvGenre.text = getString(R.string.label_genre, genre)
                    tvImdbRating.text = getString(R.string.label_imdb_rating, imdbRating)
                    tvImdbVotes.text = getString(R.string.label_imdb_votes, imdbVotes)
                    tvDirector.text = getString(R.string.label_director, director)
                    tvActors.text = getString(R.string.label_actors, actors)
                    tvPlot.text = plot

                    val posterUrl = response.optString("Poster", "N/A")
                    loadPosterImage(posterUrl)
                } else {
                    showSearchResultsView()
                    val errorMsg = response.optString("Error", "Could not load movie details.")
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                showSearchResultsView()
                val errorMsg = when {
                    error.networkResponse != null -> "Server error code: ${error.networkResponse.statusCode}"
                    error.message != null && error.message!!.isNotBlank() -> error.message!!
                    else -> "Network error fetching movie details."
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            }
        )

        requestQueue.add(jsonObjectRequest)
    }

    private fun loadPosterImage(posterUrl: String) {
        if (posterUrl != "N/A" && posterUrl.isNotBlank()) {
            ivPoster.setImageResource(android.R.color.transparent)
            val imageRequest = ImageRequest(
                posterUrl,
                { bitmap ->
                    ivPoster.setImageBitmap(bitmap)
                },
                0,
                0,
                ImageView.ScaleType.FIT_CENTER,
                Bitmap.Config.RGB_565,
                {
                    ivPoster.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            )
            requestQueue.add(imageRequest)
        } else {
            ivPoster.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun showSearchResultsView() {
        currentDetailsImdbId = ""
        scrollViewDetails.visibility = View.GONE
        tvErrorMessage.visibility = View.GONE
        if (movieList.isNotEmpty()) {
            layoutSearchResults.visibility = View.VISIBLE
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        tvErrorMessage.visibility = View.GONE
        layoutSearchResults.visibility = View.GONE
        scrollViewDetails.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        layoutSearchResults.visibility = View.GONE
        scrollViewDetails.visibility = View.GONE
        tvErrorMessage.text = message
        tvErrorMessage.visibility = View.VISIBLE
    }

    private fun formatField(value: String?): String {
        return if (value.isNullOrBlank() || value.equals("N/A", ignoreCase = true) || value.equals("null", ignoreCase = true)) {
            "N/A"
        } else {
            value
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        currentFocus?.let {
            imm?.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}