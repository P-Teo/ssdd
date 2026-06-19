package com.sd.laborator.services

import com.sd.laborator.interfaces.CityFileReaderInterface
import com.sd.laborator.interfaces.FilterInterface
import com.sd.laborator.interfaces.LocationSearchInterface
import com.sd.laborator.interfaces.WeatherForecastInterface
import com.sd.laborator.pojo.WeatherForecastData
import com.sd.laborator.services.filters.CityNameFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class WeatherOrchestrator(
    private val cityFileReader: CityFileReaderInterface,
    private val locationSearchService: LocationSearchInterface,
    private val weatherForecastService: WeatherForecastInterface,
    private val filterService: FilterService,
    @Value("\${weather.cities-file:cities.txt}") private val citiesFilePath: String
) {

    /**
     * Returns forecast data for cities matching [cityName], after running
     * the full pipeline. Additional filters can be passed in to compose
     * further criteria (e.g. a minimum-temperature filter) without changing
     * this method's signature behaviour for existing callers.
     */
    fun getFilteredData(
        cityName: String,
        extraFilters: List<FilterInterface> = emptyList()
    ): List<WeatherForecastData> {

        val cities = cityFileReader.read(File(citiesFilePath))

        val allData = mutableListOf<WeatherForecastData>()

        cities.forEach { city ->
            val coords = locationSearchService.getCoordinates(city)
            val forecast = weatherForecastService.getForecastByCoords(city, coords)
            allData.add(forecast)
        }

        val filters: List<FilterInterface> =
            listOf(CityNameFilter(cityName)) + extraFilters

        return filterService.filter(allData, filters)
    }
}
