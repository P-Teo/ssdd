package com.sd.laborator

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import kotlin.system.exitProcess

/**
 * PublisherMicroservice — trimite imagini Docker la registry.
 *
 * SRP: responsabilitate unică — publicarea de imagini.
 *      Nu gestionează abonați, nu face broadcast.
 *
 * DIP: adresa și portul registry-ului vin din variabile de mediu,
 *      nu sunt hardcodate.
 */
class PublisherMicroservice {

    private lateinit var registrySocket: Socket

    companion object Constants {
        val REGISTRY_HOST = System.getenv("REGISTRY_HOST") ?: "localhost"
        const val REGISTRY_PORT = 1700
    }

    private fun connectToRegistry() {
        try {
            registrySocket = Socket(REGISTRY_HOST, REGISTRY_PORT)
            println("[Publisher] Conectat la DockerRegistry pe $REGISTRY_HOST:$REGISTRY_PORT")
        } catch (e: Exception) {
            println("[Publisher] Nu mă pot conecta la registry: ${e.message}")
            exitProcess(1)
        }
    }

    /**
     * Trimite o comandă `push` la registry și afișează răspunsul.
     * SRP: o singură operație — push.
     */
    private fun pushImage(name: String, tag: String) {
        registrySocket.getOutputStream().write("push $name $tag\n".toByteArray())
        val response = BufferedReader(InputStreamReader(registrySocket.inputStream)).readLine()
        println("[Publisher] Răspuns registry: $response")
    }

    fun run() {
        connectToRegistry()

        // Exemplu de utilizare: citim comenzi de la stdin
        println("[Publisher] Gata de push. Sintaxă: <nume_imagine> <tag>  (ex: myapp latest)")
        val reader = BufferedReader(InputStreamReader(System.`in`))

        while (true) {
            print("> ")
            val line = reader.readLine() ?: break
            val parts = line.trim().split(" ", limit = 2)
            if (parts.size == 2) {
                pushImage(parts[0], parts[1])
            } else {
                println("Sintaxă: <nume_imagine> <tag>")
            }
        }

        registrySocket.close()
    }
}

fun main(args: Array<String>) {
    val publisher = PublisherMicroservice()
    publisher.run()
}