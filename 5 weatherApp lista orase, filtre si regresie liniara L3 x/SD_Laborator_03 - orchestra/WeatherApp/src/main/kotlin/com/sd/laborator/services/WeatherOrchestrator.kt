package com.sd.laborator.services

import com.sd.laborator.interfaces.LocationSearchInterface
import com.sd.laborator.interfaces.WeatherForecastInterface
import org.springframework.stereotype.Service

@Service
class WeatherOrchestrator(
    private val locationSearchService: LocationSearchInterface,
    private val geoRestrictionService: GeoRestrictionService,
    private val weatherForecastService: WeatherForecastInterface
)
{

    fun getWeatherForLocation(locationName: String): Any {

        val coords = locationSearchService.getCoordinates(locationName) ?: return "Locatia \"$locationName\" nu exista!"

        val zoneName = locationName.trim()

        if (!geoRestrictionService.isAccessAllowed(zoneName))
        {
            return "Acces interzis pentru zona \"$zoneName\"!"
        }
        return weatherForecastService.getForecastData(coords)
    }
}