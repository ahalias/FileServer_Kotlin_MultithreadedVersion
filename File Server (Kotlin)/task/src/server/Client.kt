package client

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.InetAddress
import java.net.Socket

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val address = "127.0.0.1"
        val port = 23456

        val socket = Socket(InetAddress.getByName(address), port)

        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())

        val filePath = "src/client/data/"



        fun saveFile() {
            println("Enter name of the file:")
            val fileToSend = readln()
            println("Enter name of the file to be saved on server:")
            val fileName = readln()
            if (fileName.isNotBlank()) {
                output.writeUTF("PUT $fileName")
            } else {
                output.writeUTF("PUT RANDOM")
            }
            output.flush()
            val message = File("$filePath$fileToSend").readBytes()
            output.writeInt(message.size)
            output.write(message)
            println("The request was sent.")
        }

        fun getFile() {
            println("Do you want to get the file by name or by id (1 - name, 2 - id): ")
            val getChoice = readln()
            if (getChoice == "2") {
                println("Enter id:")
                val getId = readln()
                output.writeUTF("GET ID $getId")
            } else if (getChoice == "1") {
                println("Enter file name:")
                val getName = readln()
                output.writeUTF("GET NAME $getName")
            }
            output.flush()
            println("The request was sent.")
        }

        fun deleteFile() {
            println("Do you want to delete the file by name or by id (1 - name, 2 - id): ")
            val deleteChoice = readln()
            if (deleteChoice == "1") {
                println("Enter file name:")
                val deleteName = readln()
                output.writeUTF("DELETE NAME $deleteName")
            } else if (deleteChoice == "2") {
                println("Enter id:")
                val deletetId = readln()
                output.writeUTF("DELETE ID $deletetId")
            }
            output.flush()
            println("The request was sent.")
        }


        fun closeConnection() {
            socket.close()
            System.exit(0)
        }

        println("Enter action (1 - get a file, 2 - create a file, 3 - delete a file):")
        val choice = readln().uppercase()

        when(choice) {
            "2" -> {
                saveFile()
                val response = input.readUTF().split(" ")
                if (response[0] == "200") println("Response says that file is saved! ID = ${response[1]}\n") else { println("The response says that creating the file was forbidden!\n") }
                closeConnection()
            }
            "1" -> {
                getFile()
                val response = input.readUTF().split(" ")
                if (response[0] == "200") {
                    val length = input.readInt()
                    val message = ByteArray(length)
                    input.readFully(message, 0, message.size)
                    println("The file was downloaded! Specify a name for it:")
                    val name = readln()
                    val file = File("${filePath}${name}")
                    file.writeBytes(message)
                    println("File saved on the hard drive!\n")
                } else println("The response says that this file is not found!\n")
                closeConnection()
            }
            "3" -> {
                deleteFile()
                val response = input.readUTF()
                if (response == "200") println("The response says that the file was successfully deleted!\n") else println("The response says that the file was not found!\n")
                closeConnection()
            }
            "EXIT" -> {
                output.writeUTF("EXIT ")
                println("The request was sent.")
                closeConnection()
            }
        }
    }
}