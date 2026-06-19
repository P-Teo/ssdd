package com.sd.laborator

import java.util.*
import javax.inject.Singleton

@Singleton
class Service {
    private val random_vect = Vector<Int>(100)

    fun calcul(seed: Long): List<Int> {

        val A = mutableListOf<Int>() // ADT A
        val B = mutableListOf<Int>() // ADT B

        var sum = 0

        for (i in 0 until 100) {
            val value = (Math.random() * 100).toInt()
            A.add(value)
        }

        for (i in 0 until 100) {
            sum += A[i] * A[i]
            B.add(sum)
        }

        return B
    }
}