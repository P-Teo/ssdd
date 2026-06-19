package com.sd.laborator.services

import com.sd.laborator.interfaces.FilterInterface
import com.sd.laborator.pojo.WeatherForecastData
import org.springframework.stereotype.Service


@Service
class FilterService {

    fun filter(
        data: List<WeatherForecastData>,
        filters: List<FilterInterface>
    ): List<WeatherForecastData> {

        return data.filter { record ->
            filters.all { it.execute(record) }
        }
    }
}
