package server

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.random.Random
import kotlin.system.exitProcess
import java.util.concurrent.locks.*


object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val address = "127.0.0.1"
        val port = 23456
        try {
            ServerSocket(port, 50, InetAddress.getByName(address)).use { server ->
                print("Server Started")
                while (true) {
                    val session = Session(server)
                    session.start()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }



    class Session(private val server: ServerSocket) : Thread() {
        private val socket = server.accept()
        private val input = DataInputStream(socket.getInputStream())
        private val output = DataOutputStream(socket.getOutputStream())
        private val filePath = "src/server/data/"
        private val idPath = "src/server/data/id/"
        private var getName = ""
        val lock: ReadWriteLock = ReentrantReadWriteLock()
        val readLock: Lock = lock.readLock()
        val writeLock: Lock = lock.writeLock()

        override fun run() {
            try {
                input.use { input ->
                    output.use { output ->
                        val serverMessage = input.readUTF().split(" ", limit = 3)
                        if (serverMessage[0] == "EXIT") {
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
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        private fun getRandomName(): String {
            val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
            return (1..Random.nextInt(5, 10))
                .map { allowedChars.random() }
                .joinToString("")
        }

        private fun getFileNameById(filename: String) = File("${idPath}${filename}").readText()

        private fun generateId() = Random.nextInt(1, 10000000)
        private fun createFile(filename: String) {
            try {
                val length = input.readInt()
                val message = ByteArray(length)
                input.readFully(message, 0, message.size)
                getName = if (filename == "RANDOM") getRandomName() else filename
                val file = File("${filePath}${getName}")
                writeLock.lock()
                file.writeBytes(message)
                writeLock.unlock()
                val id = generateId()
                val fileId = File("${idPath}${id}")
                writeLock.lock()
                fileId.writeText(getName)
                writeLock.unlock()
                output.writeUTF("200 $id")
            } catch (e: Exception) {
                output.writeUTF("403")
            }
        }

        private fun sendFile(choice: String, filename: String) {
            try {
                readLock.lock()
                getName = if (choice == "ID") getFileNameById(filename) else filename
                readLock.unlock()
                readLock.lock()
                val message = File("$filePath$getName").readBytes()
                readLock.unlock()
                output.writeUTF("200 $getName")
                output.flush()
                output.writeInt(message.size)
                output.write(message)
            } catch (e: Exception) {
                output.writeUTF("404")
            }
        }

        private fun deleteFile(choice: String, filename: String) {
            try {
                readLock.lock()
                getName = if (choice == "ID") getFileNameById(filename) else filename
                readLock.unlock()
                writeLock.lock()
                File("${filePath}${getName}").delete()
                writeLock.unlock()
                output.writeUTF("200")
            } catch (e: Exception) {
                output.writeUTF("404")
            }
        }
    }
    }
