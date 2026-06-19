package com.sd.laborator.services

import com.sd.laborator.interfaces.WeatherForecastInterface
import com.sd.laborator.pojo.WeatherForecastData
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URI

@Service
class WeatherForecastService : WeatherForecastInterface {

    override fun getForecastByCoords(
        cityName: String,
        coords: Pair<Double, Double>
    ): WeatherForecastData {

        // NOTE: we request both "current" (for currentTemp/humidity/wind)
        // and "daily" min/max so the regression service has two genuinely
        // distinct variables to regress against each other instead of the
        // same value duplicated (the original bug).
        val url = URI(
            "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${coords.first}" +
                    "&longitude=${coords.second}" +
                    "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m" +
                    "&daily=temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto"
        ).toURL()

        val json = JSONObject(url.readText())
        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")

        val dailyDates = daily.getJSONArray("time")
        val dailyMax = daily.getJSONArray("temperature_2m_max")
        val dailyMin = daily.getJSONArray("temperature_2m_min")

        return WeatherForecastData(
            location = cityName,
            currentTemp = current.getDouble("temperature_2m").toInt(),
            humidity = current.getDouble("relative_humidity_2m").toInt(),
            windSpeed = current.getDouble("wind_speed_10m").toInt(),
            minTemp = dailyMin.getDouble(0).toInt(),
            maxTemp = dailyMax.getDouble(0).toInt(),
            date = dailyDates.getString(0),
            weatherState = "",
            weatherStateIconURL = "",
            windDirection = current.getDouble("wind_direction_10m").toString()
        )
    }
}
