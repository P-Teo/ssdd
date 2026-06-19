package org.example.config

import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class RabbitMqQueuesConfig {

    @Bean
    open fun gatewayQueue() = Queue("queue.gateway", true)

    @Bean
    open fun createQueue() = Queue("queue.create", true)

    @Bean
    open fun readQueue() = Queue("queue.read", true)

    @Bean
    open fun updateQueue() = Queue("queue.update", true)

    @Bean
    open fun deleteQueue() = Queue("queue.delete", true)

    @Bean
    open fun responseQueue() = Queue("queue.response", true)
}