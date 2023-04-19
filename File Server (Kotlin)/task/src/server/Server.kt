package server

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.random.Random
import kotlin.system.exitProcess
import java.util.concurrent.Executors


object Main {
    @JvmStatic
    fun main(args: Array<String>) {

        val executor = Executors.newFixedThreadPool(10)


        val address = "127.0.0.1"
        val port = 23456

        val server = ServerSocket(port, 50, InetAddress.getByName(address))
        println("Server started!")

        val filePath = "src/server/data/"
        val idPath = "src/server/data/id/"

        var name = ""

        fun getRandomName(): String {
            val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
            return (1..Random.nextInt(5, 10))
                .map { allowedChars.random() }
                .joinToString("")
        }

        fun getFileNameById(filename: String) = File("${idPath}${filename}").readText()

        fun getId() = Random.nextInt(1, 10000000)

        while (true) {
                val socket = server.accept()
                val input = DataInputStream(socket.getInputStream())
                val output = DataOutputStream(socket.getOutputStream())

                fun createFile(filename: String) {
                    try {
                        val length = input.readInt()
                        val message = ByteArray(length)
                        input.readFully(message, 0, message.size)
                        if (filename == "RANDOM") {
                            name = getRandomName()
                        } else {
                            name = filename
                        }
                        val file = File("${filePath}${name}")
                        file.writeBytes(message)
                        val id = getId()
                        val fileId = File("${idPath}${id}")
                        fileId.writeText("$name")
                        output.writeUTF("200 $id")
                    } catch (e: Exception) {
                        output.writeUTF("403")
                    }
                }

                fun sendFile(choice: String, filename: String) {
                    try {
                        if (choice == "ID") {
                            name = getFileNameById(filename)
                        } else {
                            name = filename
                        }
                        val message = File("$filePath$name").readBytes()
                        output.writeUTF("200 $name")
                        output.flush()
                        output.writeInt(message.size)
                        output.write(message)
                    } catch (e: Exception) {
                        output.writeUTF("404")
                    }
                }

                fun deleteFile(choice: String, filename: String) {
                    try {
                        if (choice == "ID") {
                            name = getFileNameById(filename)
                        } else {
                            name = filename
                        }
                        File("${filePath}${name}").delete()
                        output.writeUTF("200")
                    } catch (e: Exception) {
                        output.writeUTF("404")
                    }
                }

                val serverMessage = input.readUTF().split(" ", limit = 3)
            if (serverMessage[0] ==  "EXIT") {
                server.close()
                exitProcess(1)
            } else {

                when (serverMessage[0]) {
                    "PUT" -> createFile(serverMessage[1])
                    "GET" -> sendFile(serverMessage[1], serverMessage[2])
                    "DELETE" -> deleteFile(serverMessage[1], serverMessage[2])
            }
                }
                socket.close()
            }
        }
    }