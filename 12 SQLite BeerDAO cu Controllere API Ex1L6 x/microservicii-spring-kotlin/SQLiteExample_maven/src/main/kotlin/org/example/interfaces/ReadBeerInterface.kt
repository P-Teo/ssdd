package org.example.interfaces

interface ReadBeerInterface {
    fun getBeers(): String
    fun getBeerByName(name: String): String?
    fun getBeerByPrice(price: Float): String?
}