package com.sd.laborator.interfaces

import com.sd.laborator.services.Coordinates

interface LocationSearchInterface {
    fun getCoordinates(locationName: String): Coordinates?
}