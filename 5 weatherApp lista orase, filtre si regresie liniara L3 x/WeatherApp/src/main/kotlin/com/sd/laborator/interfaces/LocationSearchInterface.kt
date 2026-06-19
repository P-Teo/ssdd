package com.sd.laborator.interfaces

interface LocationSearchInterface {
    fun getCoordinates(locationName: String): Pair<Double, Double>
}
