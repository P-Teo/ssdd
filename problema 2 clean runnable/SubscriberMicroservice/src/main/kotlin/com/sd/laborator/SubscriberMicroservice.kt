package com.sd.laborator

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import kotlin.system.exitProcess

/**
 * SubscriberMicroservice — se abonează la DockerRegistry și primește notificări
 * la fiecare imagine nouă publicată.
 *
 * SRP: responsabilitate unică — subscribe/unsubscribe + receive.
 *      Nu publică imagini, nu face routing.
 *
 * DIP: adresele vin din variabile de mediu.
 */
class SubscriberMicroservice {

    private lateinit var registrySocket: Socket

    companion object Constants {
        val REGISTRY_HOST = System.getenv("REGISTRY_HOST") ?: "localhost"
        const val REGISTRY_PORT = 1700
    }

    private fun connectToRegistry() {
        try {
            registrySocket = Socket(REGISTRY_HOST, REGISTRY_PORT)
            println("[Subscriber] Conectat la DockerRegistry pe $REGISTRY_HOST:$REGISTRY_PORT")
        } catch (e: Exception) {
            println("[Subscriber] Nu mă pot conecta la registry: ${e.message}")
            exitProcess(1)
        }
    }

    /**
     * Trimite comanda `subscribe` și confirmă abonarea.
     * SRP: o singură operație.
     */
    private fun subscribe() {
        registrySocket.getOutputStream().write("subscribe ${registrySocket.localPort}\n".toByteArray())
        val ack = BufferedReader(InputStreamReader(registrySocket.inputStream)).readLine()
        println("[Subscriber] Confirmare: $ack")
    }

    /**
     * Ascultă continuu notificări de la registry.
     * SRP: responsabil exclusiv cu recepția și afișarea notificărilor.
     */
    private fun listenForNotifications() {
        println("[Subscriber] Ascult notificări de la registry...")
        val reader = BufferedReader(InputStreamReader(registrySocket.inputStream))

        while (true) {
            val notification = reader.readLine() ?: break
            handleNotification(notification)
        }

        println("[Subscriber] Conexiunea cu registry-ul a fost închisă.")
    }

    /**
     * Procesează o notificare primită.
     * OCP: se pot adăuga tipuri noi de notificări fără a modifica listenForNotifications().
     */
    private fun handleNotification(message: String) {
        val parts = message.trim().split(" ", limit = 3)
        when {
            parts[0] == "notificare" && parts.size >= 3 -> {
                println("[Subscriber] *** Imagine nouă disponibilă: ${parts[2]} ***")
            }
            else -> {
                println("[Subscriber] Mesaj primit: $message")
            }
        }
    }

    fun run() {
        connectToRegistry()
        subscribe()
        listenForNotifications()
        registrySocket.close()
    }
}

fun main(args: Array<String>) {
    val subscriber = SubscriberMicroservice()
    subscriber.run()
}