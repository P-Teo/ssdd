package com.sd.laborator.controllers

import com.sd.laborator.interfaces.LocationSearchInterface
import com.sd.laborator.interfaces.WeatherForecastInterface
import com.sd.laborator.services.Coordinates
import com.sd.laborator.services.GeoRestrictionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import java.net.URL
import org.json.JSONObject


@Controller
class WeatherAppController {

    @Autowired
    private lateinit var geoRestrictionService: GeoRestrictionService

    @Autowired
    private lateinit var locationSearchService: LocationSearchInterface

    @Autowired
    private lateinit var weatherForecastService: WeatherForecastInterface

    @RequestMapping("/getforecast/{location}", method = [RequestMethod.GET])
    @ResponseBody
    fun getForecast(@PathVariable location: String): Any {
        val coords = locationSearchService.getCoordinates(location)
            ?: return "Locatia nu exista"


        if (!geoRestrictionService.isAccessAllowed(coords.countryCode)) {
            return "Acces interzis pentru zona ${coords.countryCode}"
        }


        return weatherForecastService.getForecastData(coords)
    }


    fun getCountryCode(coords: Coordinates): String {
        val url = URL(
            "https://geocoding-api.open-meteo.com/v1/reverse?latitude=${coords.latitude}&longitude=${coords.longitude}&language=en"
        )

        val response = JSONObject(url.readText())
        val results = response.getJSONArray("results")
        val first = results.getJSONObject(0)

        return first.getString("country_code")
    }


}