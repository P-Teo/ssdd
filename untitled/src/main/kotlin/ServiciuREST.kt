package org.example

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post

@Controller("/automaton")
class AutomatonController(private val client: AutomatonClient) {

    // Exemplu de apel: POST http://localhost:8080/automaton/start/1101
    @Post("/start/{sequence}")
    fun start(sequence: String): String {
        println("=== START AUTOMAT cu secventa: $sequence ===")
        client.sendTo00(sequence)
        return "Secventa $sequence a fost trimisa in coada starii 00."
    }
}