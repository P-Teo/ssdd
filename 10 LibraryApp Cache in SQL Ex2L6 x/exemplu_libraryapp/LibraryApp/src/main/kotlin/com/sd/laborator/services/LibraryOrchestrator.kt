package com.sd.laborator.services

import com.sd.laborator.interfaces.CachingDAO
import com.sd.laborator.interfaces.LibraryDAO
import com.sd.laborator.interfaces.LibraryPrinter
import org.springframework.stereotype.Service

@Service
class LibraryOrchestrator(
    private val libraryDAO: LibraryDAO,
    private val libraryPrinter: LibraryPrinter,
    private val cachingDAO: CachingDAO
) {

    fun getPrintedLibrary(format: String): String {
        val cacheKey = "/print/$format"
        val cached = cachingDAO.exists(cacheKey)

        if (cached.isNotEmpty()) {
            return cached.replace("\\n", "\n") // Restaurăm newline-urile originale
        }

        val books = libraryDAO.getBooks()

        // Am schimbat `.lowercase()` în `.toLowerCase()` pentru compatibilitate cu versiunile mai vechi de Kotlin
        val result = when (format.toLowerCase()) {
            "html" -> libraryPrinter.printHTML(books)
            "json" -> libraryPrinter.printJSON(books)
            "raw" -> libraryPrinter.printRaw(books)
            else -> "Not implemented"
        }

        cachingDAO.addToCache(cacheKey, result)
        return result
    }

    fun findBooks(field: String, value: String): String {
        if (value.isEmpty()) return "Not a valid field"

        val cacheKey = "/find/$field/$value"
        val cached = cachingDAO.exists(cacheKey)

        if (cached.isNotEmpty()) {
            return cached.replace("\\n", "\n")
        }

        val matchedBooks = when (field) {
            "author" -> libraryDAO.findAllByAuthor(value)
            "title" -> libraryDAO.findAllByTitle(value)
            "publisher" -> libraryDAO.findAllByPublisher(value)
            else -> return "Not a valid field"
        }

        val result = libraryPrinter.printJSON(matchedBooks)
        cachingDAO.addToCache(cacheKey, result)
        return result
    }
}