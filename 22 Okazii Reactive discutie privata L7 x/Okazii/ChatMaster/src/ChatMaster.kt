import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

// === CLASA UTILITARĂ (PENTRU EVENIMENTELE DE FLUX) ===
// Prin adăugarea "val", proprietățile devin automat accesibile public (rezolvă eroarea cu "socket")
class Observed(val message: String, val socket: Socket)

// === 1. PROCESORUL MASTER (ORCHESTRATORUL) ===
class ChatMaster {
    companion object Constants {
        const val MASTER_PORT = 1790
    }

    private lateinit var masterServerSocket: ServerSocket

    // Magistrala centrală (Stream Processor Global) prin care trec toate mesajele tuturor camerelor
    val globalChatStream: PublishSubject<Observed> = PublishSubject.create()
    val roomsMapping = ConcurrentHashMap<String, MutableList<String>>()

    fun run() {
        masterServerSocket = ServerSocket(MASTER_PORT)
        println("[ChatMaster] Serverul rulează pe portul: ${masterServerSocket.localPort}")
        println("[ChatMaster] Se așteaptă conexiuni pentru discuții private...")

        while (true) {
            try {
                val clientSocket = masterServerSocket.accept()
                val userEndpoint = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                println("[ChatMaster] Client nou conectat: $userEndpoint")

                // Alocare dinamică în camere private (Port par -> Camera A, Port impar -> Camera B)
                val assignedRoom = if (clientSocket.port % 2 == 0) "Camera_Privata_A" else "Camera_Privata_B"
                roomsMapping.computeIfAbsent(assignedRoom) { mutableListOf() }.add(userEndpoint)

                // Instanțiem și pornim un PROCESOR REPLICAT dedicat exclusiv acestui utilizator (Urmează SRP)
                val communicationProcessor = ChatCommunicationProcessor(clientSocket, assignedRoom, this)
                thread { communicationProcessor.startProcessing() }

            } catch (e: Exception) {
                println("[ChatMaster] Eroare la acceptarea clientului: ${e.message}")
            }
        }
    }
}

// === 2. PROCESORUL DE COMUNICAȚIE REPLICAT (WORKER-UL ASINCRON) ===
class ChatCommunicationProcessor(
    private val clientSocket: Socket,
    private val roomId: String,
    private val master: ChatMaster
) {
    private val subscriptions = CompositeDisposable()
    private val userEndpoint = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"

    fun startProcessing() {
        try {
            val reader = BufferedReader(InputStreamReader(clientSocket.inputStream))
            val writer = PrintWriter(clientSocket.getOutputStream(), true)

            // Anunțăm utilizatorul în ce cameră a fost aruncat de către Master
            writer.println("Te-ai alăturat camerei private: $roomId")

            // LOGICA REACTIVĂ DE FLUX: Ne abonăm la magistrala globală, dar filtrăm mesajele
            val flowSubscription = master.globalChatStream
                .filter { observed ->
                    // Extragem identificatorul camerei atașat la începutul mesajului
                    val targetRoom = observed.message.substringBefore("|", "Global")
                    // Permitem doar mesajele care aparțin camerei noastre private
                    targetRoom == roomId
                }
                .subscribeBy(
                    onNext = { observed ->
                        // Deserializăm doar corpul curat al mesajului (după caracterul '|')
                        val actualMsg = Message.deserialize(observed.message.substringAfter("|").toByteArray())

                        // Evităm ecoul (nu trimitem mesajul înapoi către cel care l-a scris)
                        if (observed.socket != clientSocket) {
                            writer.println(actualMsg.toString())
                        }
                    },
                    onError = { println("[$userEndpoint] Eroare pe flux reactiv: ${it.message}") }
                )
            subscriptions.add(flowSubscription)

            // Bucla principală de citire asincronă din Socket (Ieșirea din cameră la "end of transmission")
            while (true) {
                val receivedLine = reader.readLine() ?: break
                if (receivedLine == "end of transmission") break

                // Împachetăm textul într-un obiect de tip Message original
                val messageObject = Message.create(userEndpoint, receivedLine)

                // Prefixăm payload-ul cu numele camerei: "NumeCamera|MesajSerializat"
                val payload = "$roomId|${String(messageObject.serialize())}"

                // Trimitem evenimentul în fluxul central al Master-ului pentru a fi distribuit
                master.globalChatStream.onNext(Observed(payload, clientSocket))
            }

        } catch (e: Exception) {
            println("[$userEndpoint] Conexiune închisă de client sau eroare: ${e.message}")
        } finally {
            cleanUp()
        }
    }

    private fun cleanUp() {
        subscriptions.dispose() // Dezabonare completă de la flux (previne memory leaks)
        try { clientSocket.close() } catch (e: Exception) {}
        println("[$userEndpoint] Procesor de comunicație distrus cu succes.")
    }
}

// === 3. PUNCTUL DE INTRARE ÎN APLICAȚIE ===
fun main(args: Array<String>) {
    val chatMaster = ChatMaster()
    chatMaster.run()
}