package com.sd.laborator.interfaces

import com.sd.laborator.pojo.WeatherForecastData


interface WeatherForecastInterface {

    fun getForecastByCoords(
        cityName: String,
        coords: Pair<Double, Double>
    ): WeatherForecastData
}