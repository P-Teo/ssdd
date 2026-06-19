package com.sd.laborator.services

import org.springframework.stereotype.Service
import java.io.File

@Service
class GeoRestrictionService {

    private val blacklistedZones: Set<String> = File("D:\\sd\\tuiasi_sd-main\\tuiasi_sd-main\\SD_Laborator_03 - orchestra\\WeatherApp\\src\\main\\kotlin\\com\\sd\\laborator\\blacklist.txt")
        .readLines()
        .map { it.trim() }
        .toSet()

    fun isAccessAllowed(countryCode: String): Boolean {
        return !blacklistedZones.contains(countryCode)
    }
}