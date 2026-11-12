package com.example.worldexplorer

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCountryParsingV5() {
        val jsonString = """
            {
                "names": { "common": "India" },
                "codes": { "alpha_2": "IN" },
                "flag": { "url_png": "https://flags.restcountries.com/v5/w320/in.png" },
                "region": "Asia",
                "capitals": [ { "name": "New Delhi" } ],
                "population": 1380004385,
                "currencies": { "INR": { "name": "Indian rupee", "symbol": "₹" } },
                "languages": [ { "name": "Hindi" }, { "name": "English" } ]
            }
        """.trimIndent()

        val jsonObject = JSONObject(jsonString)
        val country = Country.fromJson(jsonObject)

        assertEquals("India", country.name)
        assertEquals("IN", country.code)
        assertEquals("https://flags.restcountries.com/v5/w320/in.png", country.flagUrl)
        assertEquals("Asia", country.region)
        assertEquals("New Delhi", country.capital)
        assertEquals(1380004385L, country.population)
        assertEquals("Indian rupee (₹)", country.currencies)
        assertEquals("Hindi, English", country.languages)
    }

    @Test
    fun testCountryParsingFallbackCDN() {
        val jsonString = """
            {
                "names": { "common": "France" },
                "codes": { "alpha_2": "FR" },
                "region": "Europe",
                "capitals": [ { "name": "Paris" } ],
                "population": 67390000
            }
        """.trimIndent()

        val jsonObject = JSONObject(jsonString)
        val country = Country.fromJson(jsonObject)

        assertEquals("France", country.name)
        assertEquals("FR", country.code)
        assertEquals("https://flags.restcountries.com/v5/w320/fr.png", country.flagUrl)
    }
}
