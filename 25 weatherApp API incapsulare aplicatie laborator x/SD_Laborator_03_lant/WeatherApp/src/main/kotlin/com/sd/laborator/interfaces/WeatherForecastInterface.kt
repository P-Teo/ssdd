package com.sd.laborator.interfaces

import com.sd.laborator.services.Coordinates
import com.sd.laborator.pojo.WeatherForecastData

interface WeatherForecastInterface {
    fun getForecastData(coords: Coordinates): WeatherForecastData
}