import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.net.Socket
import kotlin.Exception
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.system.exitProcess

class ChatClient {
    private lateinit var masterSocket: Socket
    private lateinit var messagesObservable: Observable<String>
    private var myIdentity: String = "[CLIENT_NECONECTAT]"

    companion object Constants {
        const val MASTER_HOST = "localhost"
        const val MASTER_PORT = 1790
    }

    init {
        try {
            masterSocket = Socket(MASTER_HOST, MASTER_PORT)
            myIdentity = "[${masterSocket.localPort}]"
            println("$myIdentity M-am conectat la Master!")

            // FLUX REACTIV REPARAT: Citeste continuu din socket pe o singura instanta de BufferedReader
            messagesObservable = Observable.create<String> { emitter ->
                try {
                    val bufferReader = BufferedReader(InputStreamReader(masterSocket.inputStream))

                    while (!masterSocket.isClosed) {
                        val receivedMessage = bufferReader.readLine()

                        // Daca serverul s-a oprit, readLine() intoarce null
                        if (receivedMessage == null) {
                            bufferReader.close()
                            masterSocket.close()
                            emitter.onError(Exception("MasterMicroservice s-a deconectat."))
                            return@create
                        }

                        // Emitem mesajul curat in fluxul reactiv
                        emitter.onNext(receivedMessage)
                    }
                } catch (e: Exception) {
                    if (!emitter.isDisposed) {
                        emitter.onError(e)
                    }
                }
            }
        } catch (e: Exception) {
            println("$myIdentity Nu ma pot conecta la Master! Asigura-te ca ChatMaster ruleaza.")
            exitProcess(1)
        }
    }

    private fun sendMessage(message: String) {
        try {
            // Trimitem mesajul text brut, procesorul de comunicatie din Master se va ocupa de impachetare
            val outputStream = masterSocket.getOutputStream()
            outputStream.write((message + "\n").toByteArray())
            outputStream.flush()
        } catch (e: Exception) {
            println("$myIdentity Eroare la trimiterea mesajului: ${e.message}")
        }
    }

    private fun waitForResponse() {
        println("$myIdentity Astept mesaje de la camera mea privata...")

        val chatSubscription = messagesObservable.subscribeBy(
            onNext = { msg ->
                println("$myIdentity [Mesaj primit]: $msg")
            },
            onError = { err ->
                println("$myIdentity Eroare flux: ${err.message}")
            }
        )

        // Lasam subscription-ul activ pe toata durata rularii firului de executie
    }

    fun run() {
        // Pornim ascultarea mesajelor pe un fir de executie de fundal
        thread { waitForResponse() }

        // Trimitem un set de 7 mesaje simulate la interval de 3 secunde
        for (i in 0..6) {
            val randomMessage = Random.nextInt(0, 1000)
            sleep(3000)
            println("$myIdentity Trimit valoarea: $randomMessage")
            sendMessage("Am generat valoarea aleatorie: $randomMessage")
        }

        // Semnalizam politicos inchiderea transmisiei conform protocolului
        sleep(3000)
        try {
            masterSocket.getOutputStream().write("end of transmission\n".toByteArray())
            masterSocket.getOutputStream().flush()
            masterSocket.close()
            println("$myIdentity Am parasit camera de chat.")
        } catch (e: Exception) {
            // Socket-ul era deja inchis
        }
    }
}

// FUNCTIA MAIN: Curatata si pusa COMPLET in afara clasei.
// Săgeata verde de Run va apărea acum în stânga acestei linii!
fun main() {
    val chatClient = ChatClient()
    chatClient.run()
}