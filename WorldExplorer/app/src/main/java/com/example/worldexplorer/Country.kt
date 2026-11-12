package com.example.worldexplorer

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

data class Country(
    val name: String,
    val code: String,
    val flagUrl: String,
    val region: String,
    val capital: String,
    val population: Long,
    val currencies: String,
    val languages: String
) : Serializable {

    companion object {
        fun fromJson(json: JSONObject): Country {
            // 1. Name: names.common (v5) or name.common or name
            val namesObj = json.optJSONObject("names")
            val commonName = namesObj?.optString("common")
                ?: json.optJSONObject("name")?.optString("common")
                ?: if (json.has("name") && json.opt("name") is String) json.optString("name") else null

            val name = if (!commonName.isNullOrBlank()) commonName else "Unknown"

            // 2. Code: codes.alpha_2 (v5) or cca2 or code
            val codesObj = json.optJSONObject("codes")
            val alpha2Code = codesObj?.optString("alpha_2")
                ?: json.optString("cca2", "")
                .ifBlank { json.optString("code", "") }

            val code = alpha2Code.uppercase()

            // 3. Flag URL using v5 flag.url_png / url_svg / CDN fallback
            val flagUrl = parseFlagUrl(json, code)

            // 4. Region
            val region = json.optString("region", "N/A").ifBlank { "N/A" }

            // 5. Capital: capitals (array of objects containing name) or capital
            val capitalList = mutableListOf<String>()
            val capitalsArray = json.optJSONArray("capitals") ?: json.optJSONArray("capital")
            if (capitalsArray != null) {
                for (i in 0 until capitalsArray.length()) {
                    val item = capitalsArray.opt(i)
                    if (item is JSONObject) {
                        val capName = item.optString("name", "")
                        if (capName.isNotBlank()) capitalList.add(capName)
                    } else if (item is String && item.isNotBlank()) {
                        capitalList.add(item)
                    }
                }
            } else {
                val singleCapital = json.optString("capital", "")
                if (singleCapital.isNotBlank()) capitalList.add(singleCapital)
            }
            val capital = if (capitalList.isNotEmpty()) capitalList.joinToString(", ") else "N/A"

            // 6. Population
            val population = json.optLong("population", 0L)

            // 7. Currencies: object (or array)
            val currencyList = mutableListOf<String>()
            val currenciesObj = json.optJSONObject("currencies")
            if (currenciesObj != null) {
                val keys = currenciesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val currObj = currenciesObj.optJSONObject(key)
                    if (currObj != null) {
                        val currName = currObj.optString("name", key)
                        val symbol = currObj.optString("symbol", "")
                        if (symbol.isNotBlank()) {
                            currencyList.add("$currName ($symbol)")
                        } else {
                            currencyList.add(currName)
                        }
                    } else {
                        val currStr = currenciesObj.optString(key, "")
                        if (currStr.isNotBlank()) {
                            currencyList.add(currStr)
                        }
                    }
                }
            } else {
                val currenciesArray = json.optJSONArray("currencies")
                if (currenciesArray != null) {
                    for (i in 0 until currenciesArray.length()) {
                        val curr = currenciesArray.opt(i)
                        if (curr is JSONObject) {
                            val currName = curr.optString("name", "")
                            val symbol = curr.optString("symbol", "")
                            if (currName.isNotBlank()) {
                                if (symbol.isNotBlank()) {
                                    currencyList.add("$currName ($symbol)")
                                } else {
                                    currencyList.add(currName)
                                }
                            }
                        } else if (curr is String && curr.isNotBlank()) {
                            currencyList.add(curr)
                        }
                    }
                }
            }
            val currencies = if (currencyList.isNotEmpty()) currencyList.joinToString(", ") else "N/A"

            // 8. Languages: array of objects containing name (or object)
            val langList = mutableListOf<String>()
            val languagesArray = json.optJSONArray("languages")
            if (languagesArray != null) {
                for (i in 0 until languagesArray.length()) {
                    val langItem = languagesArray.opt(i)
                    if (langItem is JSONObject) {
                        val langName = langItem.optString("name", "")
                        if (langName.isNotBlank()) langList.add(langName)
                    } else if (langItem is String && langItem.isNotBlank()) {
                        langList.add(langItem)
                    }
                }
            } else {
                val languagesObj = json.optJSONObject("languages")
                if (languagesObj != null) {
                    val keys = languagesObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val langName = languagesObj.optString(key, "")
                        if (langName.isNotBlank()) langList.add(langName)
                    }
                }
            }
            val languages = if (langList.isNotEmpty()) langList.joinToString(", ") else "N/A"

            return Country(
                name = name,
                code = code,
                flagUrl = flagUrl,
                region = region,
                capital = capital,
                population = population,
                currencies = currencies,
                languages = languages
            )
        }

        private fun parseFlagUrl(json: JSONObject, code: String): String {
            val flagObj = json.optJSONObject("flag")
            if (flagObj != null) {
                val urlPng = flagObj.optString("url_png", "")
                if (urlPng.isNotBlank() && urlPng != "null") return urlPng

                val urlSvg = flagObj.optString("url_svg", "")
                if (urlSvg.isNotBlank() && urlSvg != "null") return urlSvg

                val png = flagObj.optString("png", "")
                if (png.isNotBlank() && png != "null") return png
            }

            val flagsObj = json.optJSONObject("flags")
            if (flagsObj != null) {
                val png = flagsObj.optString("png", "")
                if (png.isNotBlank() && png != "null") return png
                val svg = flagsObj.optString("svg", "")
                if (svg.isNotBlank() && svg != "null") return svg
            }

            if (code.isNotBlank() && code.length == 2) {
                return "https://flags.restcountries.com/v5/w320/${code.lowercase()}.png"
            }

            return ""
        }

        fun fromJsonArray(jsonArray: JSONArray): List<Country> {
            val list = mutableListOf<Country>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i)
                if (obj != null) {
                    list.add(fromJson(obj))
                }
            }
            return list
        }
    }
}
