package com.sd.laborator.services

import com.sd.laborator.interfaces.RegressionInterface
import com.sd.laborator.pojo.WeatherForecastData
import kotlin.math.atan
import kotlin.math.PI


@org.springframework.stereotype.Service
class RegressionService : RegressionInterface {

    override fun calculateAngle(data: List<WeatherForecastData>): Double {

        val n = data.size

        // A line needs at least 2 points; with fewer, slope is undefined.
        if (n < 2) {
            return 0.0
        }

        var sumX = 0.0
        var sumY = 0.0
        var sumXY = 0.0
        var sumX2 = 0.0

        data.forEach { weather ->
            val x = weather.minTemp.toDouble()
            val y = weather.maxTemp.toDouble()

            sumX += x
            sumY += y
            sumXY += x * y
            sumX2 += x * x
        }

        val denominator = n * sumX2 - sumX * sumX

        // All minTemp values identical -> vertical line, slope undefined.
        if (denominator == 0.0) {
            return 90.0
        }

        val slope = (n * sumXY - sumX * sumY) / denominator

        return atan(slope) * 180 / PI
    }
}
