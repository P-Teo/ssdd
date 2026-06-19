package org.example


import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.Topic

@KafkaClient
interface AutomatonClient {
    @Topic("state-00")
    fun sendTo00(sequence: String)

    @Topic("state-01")
    fun sendTo01(sequence: String)

    @Topic("state-10")
    fun sendTo10(sequence: String)

    @Topic("state-11")
    fun sendTo11(sequence: String)
}