package com.sd.laborator.services

import com.sd.laborator.interfaces.CityFileReaderInterface
import org.springframework.stereotype.Service
import java.io.File


@Service
class CityFileReader : CityFileReaderInterface {

    override fun read(file: File): MutableList<String> {

        val cities = mutableListOf<String>()

        if (!file.exists()) {
            return cities
        }

        file.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isNotEmpty() && !line.startsWith("#")) {
                cities.add(line)
            }
        }

        return cities
    }
}
