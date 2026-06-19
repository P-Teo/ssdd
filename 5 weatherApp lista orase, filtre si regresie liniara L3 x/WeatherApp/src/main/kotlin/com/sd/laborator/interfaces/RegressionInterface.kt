package com.sd.laborator.interfaces

import com.sd.laborator.pojo.WeatherForecastData


interface RegressionInterface {
    fun calculateAngle(data: List<WeatherForecastData>): Double
}
