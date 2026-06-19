package com.sd.laborator

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class StudentMicroservice {
    // intrebarile si raspunsurile sunt mentinute intr-o lista de perechi de forma:
    // [<INTREBARE 1, RASPUNS 1>, <INTREBARE 2, RASPUNS 2>, ... ]
    private lateinit var questionDatabase: MutableList<Pair<String, String>>
    private lateinit var messageManagerSocket: Socket
    private lateinit var studentServerSocket: ServerSocket
    init {

        val databaseLines: List<String> = File("D:/sd/trytry/14 laborator8 lista adr-port teacher-student + CLEAN RUNNABLE x/problema 2/StudentMicroservice/questions_database.txt").readLines()
        questionDatabase = mutableListOf()

        /*
         "baza de date" cu intrebari si raspunsuri este de forma:

         <INTREBARE_1>\n
         <RASPUNS_INTREBARE_1>\n
         <INTREBARE_2>\n
         <RASPUNS_INTREBARE_2>\n
         ...
         */
        for (i in 0..(databaseLines.size - 1) step 2) {
            questionDatabase.add(Pair(databaseLines[i], databaseLines[i + 1]))
        }
    }

    companion object Constants {
        // pentru testare, se foloseste localhost. pentru deploy, server-ul socket (microserviciul MessageManager) se identifica dupa un "hostname"
        // acest hostname poate fi trimis (optional) ca variabila de mediu
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
        val TEACHER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val TEACHER_PORT = 1600
    }

    private fun subscribeToMessageManager() {
        try {
            messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            println("M-am conectat la MessageManager!")
        } catch (e: Exception) {
            println("Nu ma pot conecta la MessageManager!")
            exitProcess(1)
        }
    }

    private fun respondToQuestion(question: String): String? {
        questionDatabase.forEach {
            // daca se gaseste raspunsul la intrebare, acesta este returnat apelantului
            if (it.first == question) {
                return it.second
            }
        }
        return "nu stiu"
    }

    public fun run() {
        // microserviciul se inscrie in lista de "subscribers" de la MessageManager prin conectarea la acesta
        subscribeToMessageManager()
        val reader = BufferedReader(InputStreamReader(messageManagerSocket.getInputStream()))
        val port = reader.readLine().toInt()
        studentServerSocket = ServerSocket(port + 4000)
        println("StudentServer se executa pe portul: ${studentServerSocket.localPort}")
        println("Se asteapta mesaje...")
        while(true)
        {

            var connection = studentServerSocket.accept()

            val bufferReader = BufferedReader(InputStreamReader(connection.inputStream))
            // se asteapta intrebari trimise prin intermediarul "MessageManager"
            val question = bufferReader.readLine()

            if (question == null) {
                // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                println("Microserviciul TeacherMicroservice (${connection.port}) a fost oprit.")
                bufferReader.close()
                connection.close()
                break
            }

            // se foloseste un thread separat pentru tratarea intrebarii primite
            GlobalScope.launch {
                println("Am primit o intrebare de la Teacher: \"${question}\"")
                val responseToQuestion = respondToQuestion(question)
                println("Trimit raspunsul: \"${responseToQuestion}\"")
                connection.getOutputStream().write((responseToQuestion + "\n").toByteArray())
            }
            //reader.close()
           //studentServerSocket.close()
        }
    }
}

fun main(args: Array<String>) {
    val studentMicroservice = StudentMicroservice()
    studentMicroservice.run()
}