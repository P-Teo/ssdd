import io.reactivex.rxjava3.core.Observable
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

// 1. Abstracție pentru scrierea statisticilor (Respectă DIP & SRP)
interface IStatisticsRepository {
    fun writeStatistics(statistics: Map<String, Int>)
}

class LocalFileStatisticsRepository(private val filePath: String) : IStatisticsRepository {
    override fun writeStatistics(statistics: Map<String, Int>) {
        try {
            val file = File(filePath)
            val sb = StringBuilder()
            sb.appendLine("=== STATISTICI ERORI LICITAȚIE ===")
            if (statistics.isEmpty()) {
                sb.appendLine("Nu au apărut erori în timpul licitației.")
            } else {
                statistics.forEach { (errorType, count) ->
                    sb.appendLine("Tip Eroare: [$errorType] -> Apariții: $count")
                }
            }
            sb.appendLine("==================================")
            file.writeText(sb.toString())
            println("\n[ErrorKeeper] Statisticile au fost salvate cu succes în: ${file.absolutePath}")
        } catch (e: Exception) {
            println("[ErrorKeeper] Eroare critică la scrierea fișierului: ${e.message}")
        }
    }
}

// 2. Procesorul de flux reactiv folosind RxJava pur (fără RxKotlin)
class ErrorStreamProcessor(private val repository: IStatisticsRepository) {

    fun processErrorStream(errorMessages: List<String>) {
        if (errorMessages.isEmpty()) {
            repository.writeStatistics(emptyMap())
            return
        }

        Observable.fromIterable(errorMessages)
            .map { rawError -> extractErrorType(rawError) }
            .groupBy { it }
            .flatMapSingle { grouped ->
                grouped.count().map { count -> grouped.key!! to count.toInt() }
            }
            .toMap({ it.first }, { it.second })
            .subscribe(
                { statisticsMap ->
                    repository.writeStatistics(statisticsMap)
                },
                { err ->
                    println("[ErrorKeeper] Eroare la procesarea fluxului: ${err.message}")
                }
            )
    }

    private fun extractErrorType(rawError: String): String {
        return when {
            rawError.contains("deconectat", ignoreCase = true) -> "DisconnectionException"
            rawError.contains("timeout", ignoreCase = true) -> "TimeoutException"
            rawError.contains("Connection refused", ignoreCase = true) -> "ConnectionRefusedException"
            rawError.contains(":") -> rawError.substringBefore(":").trim()
            else -> "UnknownError"
        }
    }
}

// 3. Microserviciul propriu-zis
class ErrorKeeperService {
    private val port = 1750
   // private val timeoutMs = 25_000 // Așteaptă 25 secunde erori, apoi generează raportul
    private val serverSocket = ServerSocket(port)
    private val collectedErrors = mutableListOf<String>()

    init {
        // Timeout-ul asigură că atunci când licitația se termină și nu mai vin erori,
        // serviciul nu rămâne blocat la nesfârșit și trece la generarea fișierului.
        //serverSocket.soTimeout = timeoutMs
        println("==================================================")
        println("ErrorKeeperService rulează pe portul: $port")
        println("Așteaptă raportări de erori de la celelalte servicii...")
        println("==================================================")
    }

    fun runService() {
        val repo = LocalFileStatisticsRepository("statistici_erori.txt")
        val processor = ErrorStreamProcessor(repo)

        while (true) {
            try {
                val connection = serverSocket.accept()
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val receivedMessage = reader.readLine()

                if (receivedMessage != null && receivedMessage.isNotBlank()) {
                    println("[ErrorKeeper] S-a interceptat eroare: $receivedMessage")
                    collectedErrors.add(receivedMessage)
                }
                reader.close()
                connection.close()
            } catch (e: SocketTimeoutException) {
                println("\n[ErrorKeeper] Timpul de monitorizare a expirat. Se generează statisticile...")
                break
            } catch (e: Exception) {
                println("[ErrorKeeper] Eroare de conexiune interceptată: ${e.message}")
                break
            }
        }

        // Procesăm fluxul strâns la finalul licitației
        processor.processErrorStream(collectedErrors)

        try {
            serverSocket.close()
        } catch (e: Exception) {}
        println("[ErrorKeeper] Serviciul și-a încheiat activitatea.")
    }
}

fun main(args: Array<String>) {
    val errorKeeper = ErrorKeeperService()
    errorKeeper.runService()
}