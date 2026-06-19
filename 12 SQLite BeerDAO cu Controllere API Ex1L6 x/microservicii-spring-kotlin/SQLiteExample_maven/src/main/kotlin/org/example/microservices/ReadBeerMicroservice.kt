package org.example.microservices

import org.example.interfaces.ReadBeerInterface  // importul corect
import org.example.model.Beer
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ReadBeerMicroservice {

    @Autowired
    private lateinit var readBeerPort: ReadBeerInterface  // acelasi nume ca interfata

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @RabbitListener(queues = ["\${read.queue}"])
    fun receiveMessage(msg: String) {
        val (operation, parameters) = msg.split('~')
        val result: String? = when (operation) {
            "getBeers" -> readBeerPort.getBeers()
            "getBeerByName" -> readBeerPort.getBeerByName(
                parameters.split("=")[1]
            )
            "getBeerByPrice" -> readBeerPort.getBeerByPrice(
                parameters.split("=")[1].toFloat()
            )
            else -> null
        }
        result?.let {
            rabbitTemplate.convertAndSend("queue.response", it)
        }
    }
}