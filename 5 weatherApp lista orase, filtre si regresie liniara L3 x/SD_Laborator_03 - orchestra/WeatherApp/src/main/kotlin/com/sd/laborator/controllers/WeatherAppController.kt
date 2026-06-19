package com.sd.laborator.controllers

import com.sd.laborator.services.WeatherOrchestrator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class WeatherAppController(private val weatherOrchestrator: WeatherOrchestrator)
{

    @GetMapping("/getforecast/{location}")
    fun getForecast(@PathVariable location: String): Any
    {
        return weatherOrchestrator.getWeatherForLocation(location)
    }
}