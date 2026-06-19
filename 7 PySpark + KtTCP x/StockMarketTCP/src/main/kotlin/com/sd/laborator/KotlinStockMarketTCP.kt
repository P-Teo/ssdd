package com.sd.laborator

import kotlinx.serialization.json.*
import java.lang.Thread.sleep
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.system.exitProcess

class GetterTCP {
    private val httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build()

    private fun httpGet(url: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    private fun sendToPython(client: Socket, data: String) {
        try {
            val cleanData = data.replace("\n", "").replace("\r", "")
            client.getOutputStream().write((cleanData + "\n").toByteArray())
            client.getOutputStream().flush()
        } catch (e: Exception) {
            println("Conexiunea cu PySpark a fost închisă!")
            exitProcess(1)
        }
    }

    fun sendGet() {
        val server = ServerSocket(9999)
        println("Serverul rulează pe portul 9999. Așteptare client PySpark...")
        val client = server.accept()
        println("Client conectat: ${client.inetAddress.hostAddress}")

        try {
            val rText = httpGet("https://finnhub.io/api/v1/stock/symbol?exchange=US&token=brl7eb7rh5re1lvco7fg")

            // ADAPTARE PENTRU KOTLINX-SERIALIZATION V0.14.0:
            val rJson = Json.plain.parseJson(rText).jsonArray

            for (it in rJson) {
                // În v0.14.0 stringul extras are ghilimele, le scoatem cu removeSurrounding
                val symbol = it.jsonObject["symbol"].toString().removeSurrounding("\"")
                if (symbol.isEmpty() || symbol == "null") continue

                println("Preluare date pentru: $symbol")

                val dataToSend = httpGet("https://finnhub.io/api/v1/stock/price-target?symbol=$symbol&token=brl7eb7rh5re1lvco7fg")

                sendToPython(client, dataToSend)
                println("Trimis: $dataToSend")
                println("______________________")

                sleep(3000)
            }
        } catch (e: Exception) {
            println("Eroare în timpul execuției: ${e.message}")
            e.printStackTrace()
        } finally {
            client.close()
            server.close()
        }
    }
}

fun main(args: Array<String>) {
    val getTCP = GetterTCP()
    getTCP.sendGet()
}