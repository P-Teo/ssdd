package com.sd.laborator.interfaces

import java.io.File


interface CityFileReaderInterface {
    fun read(file: File): MutableList<String>
}
