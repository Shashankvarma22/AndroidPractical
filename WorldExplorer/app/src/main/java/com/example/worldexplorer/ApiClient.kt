package com.example.worldexplorer

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

object ApiClient {

    private const val BASE_URL = "https://api.restcountries.com/countries/v5"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun validateApiKey(): String? {
        val key = BuildConfig.REST_COUNTRIES_API_KEY.trim()
        if (key.isEmpty() || key == "YOUR_API_KEY" || key.equals("null", ignoreCase = true)) {
            return null
        }
        return key
    }

    private fun buildRequest(url: String, apiKey: String): Request {
        return Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
    }

    suspend fun fetchAllCountries(): ApiResult<List<Country>> = withContext(Dispatchers.IO) {
        val apiKey = validateApiKey()
            ?: return@withContext ApiResult.Error("API key is missing or invalid in local.properties. Please configure REST_COUNTRIES_API_KEY.")

        try {
            val allCountries = mutableListOf<Country>()
            var offset = 0
            val limit = 100
            var hasMore = true

            while (hasMore) {
                val url = "$BASE_URL?limit=$limit&offset=$offset"
                val request = buildRequest(url, apiKey)
                client.newCall(request).execute().use { response ->
                    if (response.code == 401 || response.code == 403) {
                        return@withContext ApiResult.Error("API Key authorization failed (HTTP ${response.code}). Please check REST_COUNTRIES_API_KEY.")
                    }
                    if (!response.isSuccessful) {
                        if (allCountries.isNotEmpty()) {
                            return@withContext ApiResult.Success(allCountries)
                        }
                        return@withContext ApiResult.Error("API request failed with HTTP ${response.code}")
                    }
                    val body = response.body?.string()
                        ?: return@withContext ApiResult.Error("Empty response body from server")

                    val extracted = parseResponseJson(body)
                    allCountries.addAll(extracted.countries)

                    hasMore = extracted.hasMore && extracted.countries.isNotEmpty()
                    offset += if (extracted.countries.isNotEmpty()) extracted.countries.size else limit
                }
            }

            ApiResult.Success(allCountries)
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to connect to server")
        }
    }

    suspend fun searchCountries(name: String): ApiResult<List<Country>> = withContext(Dispatchers.IO) {
        val apiKey = validateApiKey()
            ?: return@withContext ApiResult.Error("API key is missing or invalid in local.properties. Please configure REST_COUNTRIES_API_KEY.")

        try {
            val url = "$BASE_URL/name?q=${Uri.encode(name)}"
            val request = buildRequest(url, apiKey)
            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@withContext ApiResult.Success(emptyList())
                }
                if (response.code == 401 || response.code == 403) {
                    return@withContext ApiResult.Error("API Key authorization failed (HTTP ${response.code}). Please check REST_COUNTRIES_API_KEY.")
                }
                if (!response.isSuccessful) {
                    return@withContext ApiResult.Error("Search failed with HTTP ${response.code}")
                }
                val body = response.body?.string()
                    ?: return@withContext ApiResult.Error("Empty response body from server")
                val extracted = parseResponseJson(body)
                ApiResult.Success(extracted.countries)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to connect to server")
        }
    }

    suspend fun getCountryByCode(code: String): ApiResult<Country> = withContext(Dispatchers.IO) {
        val apiKey = validateApiKey()
            ?: return@withContext ApiResult.Error("API key is missing or invalid in local.properties. Please configure REST_COUNTRIES_API_KEY.")

        try {
            val url = "$BASE_URL/codes.alpha_2/${Uri.encode(code.uppercase())}"
            val request = buildRequest(url, apiKey)
            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@withContext ApiResult.Error("Country code '$code' not found")
                }
                if (response.code == 401 || response.code == 403) {
                    return@withContext ApiResult.Error("API Key authorization failed (HTTP ${response.code}). Please check REST_COUNTRIES_API_KEY.")
                }
                if (!response.isSuccessful) {
                    return@withContext ApiResult.Error("Country lookup failed with HTTP ${response.code}")
                }
                val body = response.body?.string()
                    ?: return@withContext ApiResult.Error("Empty response body from server")
                val extracted = parseResponseJson(body)
                val country = extracted.countries.firstOrNull()
                if (country != null) {
                    ApiResult.Success(country)
                } else {
                    ApiResult.Error("Country data could not be parsed")
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to connect to server")
        }
    }

    suspend fun fetchCountriesByRegion(region: String): ApiResult<List<Country>> = withContext(Dispatchers.IO) {
        val apiKey = validateApiKey()
            ?: return@withContext ApiResult.Error("API key is missing or invalid in local.properties. Please configure REST_COUNTRIES_API_KEY.")

        try {
            val url = "$BASE_URL?region=${Uri.encode(region)}&limit=100"
            val request = buildRequest(url, apiKey)
            client.newCall(request).execute().use { response ->
                if (response.code == 401 || response.code == 403) {
                    return@withContext ApiResult.Error("API Key authorization failed (HTTP ${response.code}). Please check REST_COUNTRIES_API_KEY.")
                }
                if (!response.isSuccessful) {
                    return@withContext ApiResult.Error("Region lookup failed with HTTP ${response.code}")
                }
                val body = response.body?.string()
                    ?: return@withContext ApiResult.Error("Empty response body from server")
                val extracted = parseResponseJson(body)
                ApiResult.Success(extracted.countries)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Failed to connect to server")
        }
    }

    private data class ParsedResponse(val countries: List<Country>, val hasMore: Boolean)

    private fun parseResponseJson(body: String): ParsedResponse {
        val trimmed = body.trim()
        val list = mutableListOf<Country>()
        var hasMore = false

        if (trimmed.startsWith("{")) {
            val rootObj = JSONObject(trimmed)
            val dataObj = rootObj.optJSONObject("data")
            if (dataObj != null) {
                val objectsArray = dataObj.optJSONArray("objects")
                if (objectsArray != null) {
                    list.addAll(Country.fromJsonArray(objectsArray))
                } else {
                    val singleObject = dataObj.optJSONObject("objects")
                    if (singleObject != null) {
                        list.add(Country.fromJson(singleObject))
                    }
                }

                val metaObj = dataObj.optJSONObject("meta")
                if (metaObj != null) {
                    hasMore = metaObj.optBoolean("more", false)
                }
            } else {
                val objectsArray = rootObj.optJSONArray("objects")
                if (objectsArray != null) {
                    list.addAll(Country.fromJsonArray(objectsArray))
                } else if (rootObj.has("names") || rootObj.has("name") || rootObj.has("codes") || rootObj.has("cca2")) {
                    list.add(Country.fromJson(rootObj))
                }
            }
        } else if (trimmed.startsWith("[")) {
            val jsonArray = JSONArray(trimmed)
            list.addAll(Country.fromJsonArray(jsonArray))
        }

        return ParsedResponse(list, hasMore)
    }
}
