package com.sd.laborator.services

import com.sd.laborator.interfaces.WeatherForecastInterface
import com.sd.laborator.pojo.WeatherForecastData
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URL
import kotlin.math.roundToInt
import java.time.LocalDateTime

@Service
class WeatherForecastService (private val timeService: TimeService) : WeatherForecastInterface {
    override fun getForecastData(coords: Coordinates): WeatherForecastData {
        val url = URL("https://api.open-meteo.com/v1/forecast?latitude=${coords.latitude}&longitude=${coords.longitude}&current_weather=true")
        val response = JSONObject(url.readText())
        val current = response.getJSONObject("current_weather")

        return WeatherForecastData(
            location = "coords: ${coords.latitude}, ${coords.longitude}",
            date = LocalDateTime.now().toString(),
            weatherState = "N/A",
            weatherStateIconURL = "",
            windDirection = current.getDouble("winddirection").toString(),
            windSpeed = current.getDouble("windspeed").roundToInt(),
            minTemp = current.getDouble("temperature").roundToInt(),
            maxTemp = current.getDouble("temperature").roundToInt(),
            currentTemp = current.getDouble("temperature").roundToInt(),
            humidity = 0
        )
    }
}