package org.example.microservices

import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ApiGatewayMicroservice {

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @RabbitListener(queues = ["\${gateway.queue}"])
    fun receiveFromClient(msg: String) {
        val (operation, parameters) = msg.split('~')

        val targetQueue = when (operation) {
            "createBeerTable", "addBeer" -> "queue.create"
            "getBeers", "getBeerByName", "getBeerByPrice" -> "queue.read"
            "updateBeer" -> "queue.update"
            "deleteBeer" -> "queue.delete"
            else -> return
        }

        rabbitTemplate.convertAndSend(targetQueue, msg)
    }
}