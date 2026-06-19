package com.sd.laborator.pojo


data class WeatherForecastData(
    val location: String,
    val currentTemp: Int,
    val humidity: Int,
    val windSpeed: Int,
    val minTemp: Int,
    val maxTemp: Int,
    val date: String,
    val weatherState: String,
    val weatherStateIconURL: String,
    val windDirection: String
)
