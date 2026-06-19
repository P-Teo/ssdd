package com.sd.laborator.services

import com.sd.laborator.interfaces.CachingDAO
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

@Service
@Primary // Îi spune Spring-ului să folosească această implementare în detrimentul celei cu JDBC
class FileCachingDAOService : CachingDAO {

    private val cacheFilePath = "cache.txt"
    private val maxAgeTimestamp: Long = 1000 * 60 * 60 // 1 oră în milisecunde

    init {
        val file = File(cacheFilePath)
        if (!file.exists()) {
            file.createNewFile()
        }
    }

    override fun exists(query: String): String {
        val file = File(cacheFilePath)
        if (!file.exists() || file.length() == 0L) return ""

        val currentTime = System.currentTimeMillis()
        var latestValidResult = ""

        // Citim toate liniile. Deoarece doar adăugăm, parcurgem tot pentru a găsi cea mai recentă valoare
        file.forEachLine { line ->
            val parts = line.split("|", limit = 3)
            if (parts.size == 3) {
                val timestamp = parts[0].toLongOrNull() ?: 0L
                val cachedQuery = parts[1]
                val cachedResult = parts[2]

                // Verificăm dacă query-ul coincide și dacă nu a expirat marcajul temporal
                if (cachedQuery == query && (currentTime - timestamp) <= maxAgeTimestamp) {
                    latestValidResult = cachedResult
                }
            }
        }
        return latestValidResult
    }

    override fun addToCache(query: String, result: String) {
        // Înlocuim newline-urile din rezultat pentru a nu strica formatul fișierului linie cu linie
        val sanitizedResult = result.replace("\n", "\\n").replace("\r", "")

        FileWriter(cacheFilePath, true).use { fw ->
            PrintWriter(fw).use { pw ->
                pw.println("${System.currentTimeMillis()}|$query|$sanitizedResult")
            }
        }
    }
}