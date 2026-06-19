package com.sd.laborator

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.time.Instant
import kotlin.system.exitProcess

/**
 * Reprezintă o imagine înregistrată în registry.
 * SRP: este un simplu container de date, fără logică de business.
 */
data class ImageRecord(
    val name: String,
    val tag: String,
    val pushedAt: Long = Instant.now().epochSecond
)

/**
 * DockerRegistryMicroservice — noul microserviciu cerut de enunț.
 *
 * Responsabilități (SRP la nivel de microserviciu):
 *   - menține lista de imagini Docker înregistrate
 *   - menține lista de clienți abonați (subscribe/unsubscribe)
 *   - notifică abonații la fiecare push nou (publish-subscribe)
 *
 * OCP: mesaje noi → se adaugă un `when` branch în handleMessage(),
 *      restul clasei rămâne nemodificat.
 *
 * DIP: depinde de abstracțiunea Socket/ServerSocket (Java NIO),
 *      nu de o implementare concretă proprie.
 */
class DockerRegistryMicroservice {

    // Lista de clienți abonați: port → socket
    private val subscribedClients: HashMap<Int, Socket> = hashMapOf()

    // "Baza de date" a imaginilor: lista de ImageRecord
    private val imageRegistry: MutableList<ImageRecord> = mutableListOf()

    // Socket spre MessageManager (pentru trimiterea de notificări broadcast)
    private lateinit var messageManagerSocket: Socket

    // Server socket pe care ascultă registry-ul
    private lateinit var serverSocket: ServerSocket

    companion object Constants {
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
        const val REGISTRY_PORT = 1700
    }

    // -------------------------------------------------------------------------
    // Operații publish-subscribe
    // -------------------------------------------------------------------------

    /**
     * Înregistrează un client nou ca abonat.
     * SRP: o singură responsabilitate — adăugare în map, sincronizat.
     */
    private fun subscribeClient(port: Int, socket: Socket) {
        synchronized(subscribedClients) {
            subscribedClients[port] = socket
        }
        println("[Registry] Client abonat: port=$port. Total abonați: ${subscribedClients.size}")
    }

    /**
     * Elimină un client din lista de abonați.
     * SRP: o singură responsabilitate — ștergere din map, sincronizat.
     */
    private fun unsubscribeClient(port: Int) {
        synchronized(subscribedClients) {
            subscribedClients.remove(port)
        }
        println("[Registry] Client dezabonat: port=$port. Total abonați: ${subscribedClients.size}")
    }

    /**
     * Înregistrează o imagine nouă și notifică toți abonații.
     * SRP: orchestrare publish — deleghează stocarea și notificarea.
     */
    private fun publishImage(name: String, tag: String) {
        val record = ImageRecord(name, tag)
        synchronized(imageRegistry) {
            imageRegistry.add(record)
        }
        println("[Registry] Imagine publicată: $name:$tag")
        notifySubscribers(record)
    }

    /**
     * Trimite o notificare tuturor abonaților curenți.
     * SRP: responsabil exclusiv cu notificarea — nu modifică starea registry-ului.
     */
    private fun notifySubscribers(record: ImageRecord) {
        val message = "notificare imagine_noua ${record.name}:${record.tag}\n"
        synchronized(subscribedClients) {
            subscribedClients.forEach { (port, socket) ->
                try {
                    socket.getOutputStream().write(message.toByteArray())
                    println("[Registry] Notificat abonat port=$port")
                } catch (e: Exception) {
                    println("[Registry] Eroare la notificarea port=$port: ${e.message}")
                }
            }
        }
    }

    /**
     * Returnează lista curentă de imagini ca șir formatat.
     * SRP: responsabil exclusiv cu serializarea listei.
     */
    private fun listImages(): String {
        synchronized(imageRegistry) {
            if (imageRegistry.isEmpty()) return "registry_gol"
            return imageRegistry.joinToString(";") { "${it.name}:${it.tag}@${it.pushedAt}" }
        }
    }

    // -------------------------------------------------------------------------
    // Routare mesaje
    // -------------------------------------------------------------------------

    /**
     * Procesează un mesaj primit de la un client conectat.
     * OCP: se extinde adăugând noi ramuri `when`, fără a modifica logica existentă.
     *
     * Protocoale suportate:
     *   subscribe <PORT>              — abonare
     *   unsubscribe <PORT>            — dezabonare
     *   push <NUME_IMAGINE> <TAG>     — publicare imagine nouă
     *   list                          — listare imagini
     */
    private fun handleMessage(rawMessage: String, clientSocket: Socket) {
        val parts = rawMessage.trim().split(" ", limit = 3)
        val command = parts[0]

        when (command) {
            "subscribe" -> {
                subscribeClient(clientSocket.port, clientSocket)
                clientSocket.getOutputStream().write("ok abonat\n".toByteArray())
            }
            "unsubscribe" -> {
                unsubscribeClient(clientSocket.port)
                clientSocket.getOutputStream().write("ok dezabonat\n".toByteArray())
            }
            "push" -> {
                if (parts.size < 3) {
                    clientSocket.getOutputStream().write("eroare format: push <nume> <tag>\n".toByteArray())
                    return
                }
                val name = parts[1]
                val tag = parts[2]
                publishImage(name, tag)
                clientSocket.getOutputStream().write("ok publicat $name:$tag\n".toByteArray())
            }
            "list" -> {
                val response = listImages()
                clientSocket.getOutputStream().write("$response\n".toByteArray())
            }
            else -> {
                clientSocket.getOutputStream().write("eroare comanda necunoscuta: $command\n".toByteArray())
            }
        }
    }

    // -------------------------------------------------------------------------
    // Conectare la MessageManager (pentru notificări externe opționale)
    // -------------------------------------------------------------------------

    private fun subscribeToMessageManager() {
        try {
            messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            println("[Registry] Conectat la MessageManager.")
        } catch (e: Exception) {
            println("[Registry] Nu mă pot conecta la MessageManager: ${e.message}")
            // Registry-ul funcționează și fără MessageManager (abonații direcți sunt suficienți)
        }
    }

    // -------------------------------------------------------------------------
    // Buclă principală
    // -------------------------------------------------------------------------

    fun run() {
        subscribeToMessageManager()

        serverSocket = ServerSocket(REGISTRY_PORT)
        println("[Registry] DockerRegistryMicroservice pornit pe portul $REGISTRY_PORT")
        println("[Registry] Se așteaptă conexiuni...")

        while (true) {
            val clientConnection = serverSocket.accept()

            GlobalScope.launch {
                println("[Registry] Conexiune nouă: ${clientConnection.inetAddress.hostAddress}:${clientConnection.port}")
                val reader = BufferedReader(InputStreamReader(clientConnection.inputStream))

                // Fiecare client poate trimite mai multe comenzi pe aceeași conexiune
                while (true) {
                    val line = reader.readLine() ?: break
                    println("[Registry] Primit: $line")
                    handleMessage(line, clientConnection)

                    // Dacă clientul s-a dezabonat, închidem conexiunea
                    if (line.trim().startsWith("unsubscribe")) {
                        break
                    }
                }

                reader.close()
                clientConnection.close()
            }
        }
    }
}

fun main(args: Array<String>) {
    val registry = DockerRegistryMicroservice()
    registry.run()
}