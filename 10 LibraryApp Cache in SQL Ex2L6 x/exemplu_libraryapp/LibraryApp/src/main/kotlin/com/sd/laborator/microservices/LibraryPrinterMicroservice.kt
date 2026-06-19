package com.sd.laborator.microservices

import com.sd.laborator.services.LibraryOrchestrator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController // Înlocuim @Controller + @ResponseBody cu @RestController
class LibraryPrinterMicroservice(private val orchestrator: LibraryOrchestrator) {

    @GetMapping("/print")
    fun customPrint(@RequestParam(required = true, name = "format", defaultValue = "") format: String): String {
        return orchestrator.getPrintedLibrary(format)
    }

    @GetMapping("/find")
    fun customFind(
        @RequestParam(required = false, name = "author", defaultValue = "") author: String,
        @RequestParam(required = false, name = "title", defaultValue = "") title: String,
        @RequestParam(required = false, name = "publisher", defaultValue = "") publisher: String
    ): String {
        return when {
            author.isNotEmpty() -> orchestrator.findBooks("author", author)
            title.isNotEmpty() -> orchestrator.findBooks("title", title)
            publisher.isNotEmpty() -> orchestrator.findBooks("publisher", publisher)
            else -> "Not a valid field"
        }
    }
}