package com.sd.laborator.interfaces

import com.sd.laborator.pojo.WeatherForecastData


interface FilterInterface {
    fun execute(data: WeatherForecastData): Boolean
}
