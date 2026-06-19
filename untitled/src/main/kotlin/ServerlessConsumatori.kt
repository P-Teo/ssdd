package org.example

import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.OffsetReset
import io.micronaut.configuration.kafka.annotation.Topic

@KafkaListener(offsetReset = OffsetReset.EARLIEST)
class StateFunctions(private val client: AutomatonClient) {

    @Topic("state-00")
    fun processState00(sequence: String) {
        if (sequence.isEmpty()) return
        val currentBit = sequence[0]
        val remaining = sequence.substring(1)
        println("Starea 00: bit procesat [$currentBit]. Au mai ramas: [$remaining]")

        when (currentBit) {
            '1' -> client.sendTo01(remaining)
            '0' -> client.sendTo10(remaining)
        }
    }

    @Topic("state-01")
    fun processState01(sequence: String) {
        if (sequence.isEmpty()) return
        val currentBit = sequence[0]
        val remaining = sequence.substring(1)
        println("Starea 01: bit procesat [$currentBit]. Au mai ramas: [$remaining]")

        when (currentBit) {
            '1' -> client.sendTo11(remaining)
            '0' -> client.sendTo10(remaining)
        }
    }

    @Topic("state-10")
    fun processState10(sequence: String) {
        if (sequence.isEmpty()) return
        val currentBit = sequence[0]
        val remaining = sequence.substring(1)
        println("Starea 10: bit procesat [$currentBit]. Au mai ramas: [$remaining]")

        when (currentBit) {
            '1' -> client.sendTo01(remaining)
            '0' -> client.sendTo11(remaining)
        }
    }

    @Topic("state-11")
    fun processState11(sequence: String) {
        if (sequence.isEmpty()) return
        val currentBit = sequence[0]
        val remaining = sequence.substring(1)
        println("Starea 11: bit procesat [$currentBit]. Au mai ramas: [$remaining]")

        when (currentBit) {
            '1' -> {
                println(">>> OUTPUT = 1 <<<") // Conditia specificata in graf
                client.sendTo11(remaining)
            }
            '0' -> client.sendTo10(remaining)
        }
    }
}