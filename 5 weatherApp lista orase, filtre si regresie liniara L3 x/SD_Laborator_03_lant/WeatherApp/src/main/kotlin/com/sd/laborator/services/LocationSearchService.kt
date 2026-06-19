package com.sd.laborator.services

import com.sd.laborator.interfaces.LocationSearchInterface
import org.springframework.stereotype.Service
import java.net.URL
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.sd.laborator.services.Coordinates


@Service
class LocationSearchService : LocationSearchInterface {

    override fun getCoordinates(locationName: String): Coordinates? {
        val encodedLocationName = URLEncoder.encode(locationName, StandardCharsets.UTF_8.toString())
        val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encodedLocationName&count=1&language=en&format=json")
        val response = JSONObject(url.readText())
        val results = response.optJSONArray("results") ?: return null
        if (results.length() == 0) return null
        val first = results.getJSONObject(0)

        return Coordinates(
            latitude = first.getDouble("latitude"),
            longitude = first.getDouble("longitude"),
            countryCode = first.optString("country", "UNKNOWN"))
    }



}