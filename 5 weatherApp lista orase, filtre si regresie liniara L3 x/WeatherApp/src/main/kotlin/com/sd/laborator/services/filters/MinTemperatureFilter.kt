package com.sd.laborator.services.filters

import com.sd.laborator.interfaces.FilterInterface
import com.sd.laborator.pojo.WeatherForecastData

class MinTemperatureFilter(
    private val minTemperature: Int
) : FilterInterface {

    override fun execute(data: WeatherForecastData): Boolean {
        return data.currentTemp >= minTemperature
    }
}
