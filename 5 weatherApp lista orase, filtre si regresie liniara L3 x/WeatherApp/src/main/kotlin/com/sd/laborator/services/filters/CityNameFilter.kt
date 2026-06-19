package com.sd.laborator.services.filters

import com.sd.laborator.interfaces.FilterInterface
import com.sd.laborator.pojo.WeatherForecastData


class CityNameFilter(
    private val cityName: String
) : FilterInterface {

    override fun execute(data: WeatherForecastData): Boolean {
        return data.location.equals(cityName, ignoreCase = true)
    }
}
