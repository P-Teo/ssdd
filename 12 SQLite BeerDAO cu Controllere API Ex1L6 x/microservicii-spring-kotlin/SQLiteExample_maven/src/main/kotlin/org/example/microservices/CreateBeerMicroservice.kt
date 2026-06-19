package org.example.microservices

import org.example.interfaces.CreateBeerInterface  // <-- numele interfetei tale
import org.example.model.Beer
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CreateBeerMicroservice {

    @Autowired
    private lateinit var createBeerPort: CreateBeerInterface  // <-- acelasi nume

    @RabbitListener(queues = ["\${create.queue}"])
    fun receiveMessage(msg: String) {
        val (operation, parameters) = msg.split('~')
        when (operation) {
            "createBeerTable" -> createBeerPort.createBeerTable()
            "addBeer" -> {
                val beer = parseFullBeer(parameters)
                beer?.let { createBeerPort.addBeer(it) }
            }
        }
    }

    private fun parseFullBeer(parameters: String): Beer? {
        return try {
            val params = parameters.split(";")
            Beer(
                params[0].split("=")[1].toInt(),
                params[1].split("=")[1],
                params[2].split("=")[1].toFloat()
            )
        } catch (e: Exception) {
            println("Error parsing beer: $e")
            null
        }
    }
}