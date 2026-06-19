package com.sd.laborator.controllers

import com.sd.laborator.interfaces.RegressionInterface
import com.sd.laborator.services.WeatherOrchestrator
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class WeatherAppController(
    private val orchestrator: WeatherOrchestrator,
    private val regressionService: RegressionInterface
) {

    @GetMapping("/city/{cityName}")
    @ResponseBody
    fun getCityData(
        @PathVariable cityName: String
    ): String {

        val data = orchestrator.getFilteredData(cityName)
        val angle = regressionService.calculateAngle(data)

        return """
            Date filtrate:
            $data

            <br><br>

            Unghi regresie (minTemp vs maxTemp): $angle grade
        """.trimIndent()
    }
}
