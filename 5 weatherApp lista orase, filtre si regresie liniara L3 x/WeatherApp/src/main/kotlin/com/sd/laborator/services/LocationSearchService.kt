package com.sd.laborator.services

import com.sd.laborator.interfaces.LocationSearchInterface
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class LocationSearchService : LocationSearchInterface {

    override fun getCoordinates(locationName: String): Pair<Double, Double> {

        val encoded = URLEncoder.encode(locationName, StandardCharsets.UTF_8)

        val url = URI(
            "https://geocoding-api.open-meteo.com/v1/search" +
                    "?name=$encoded&count=1&language=en&format=json"
        ).toURL()

        val json = JSONObject(url.readText())
        val results = json.optJSONArray("results")

        if (results == null || results.isEmpty) {
            throw NoSuchElementException("No coordinates found for city: $locationName")
        }

        val result = results.getJSONObject(0)

        return Pair(
            result.getDouble("latitude"),
            result.getDouble("longitude")
        )
    }
}
