package com.mwojnar

import com.mwojnar.gen.ChatGrpcKt
import com.mwojnar.gen.Message
import com.mwojnar.gen.Priority
import com.mwojnar.gen.UserInfo
import io.grpc.ManagedChannelBuilder
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import java.io.Closeable
import java.io.File
import java.util.*

class Client(userId: Long, groupId: Long) : Closeable {
    private val state: ClientState
    private val fileName: String
    private val newMessageMutex: Mutex
    private var first: Boolean
    private var end: Boolean

    init {
        fileName = "saved/client_${userId}_group_${groupId}"
        state = if (File(fileName).exists()) {
            val bytes = File(fileName).readBytes()
            ProtoBuf.decodeFromByteArray(bytes)
        } else {
            ClientState(userId, groupId, -1, LinkedList())
        }

        newMessageMutex = Mutex(true)
        first = true
        end = false
    }

    suspend fun start(stub: ChatGrpcKt.ChatCoroutineStub) {
        CoroutineScope(Dispatchers.Default).async { cliLoop() }

        var reconnect: Boolean
        do {
            first = true
            reconnect = try {
                join(stub)
                messagesStream(stub)
                false
            } catch (e: io.grpc.StatusException) {
                println("Cannot connect to server. Reconnecting in 5 s...")
                delay(5000)
                true
            }
        } while (reconnect)
    }

    override fun close() {
        println("Connection to server closed.")

        synchronized(this) {
            val bytes = ProtoBuf.encodeToByteArray(serializer(), state)
            File(fileName).writeBytes(bytes)
            println("Client state saved.")
        }
    }

    private suspend fun join(stub: ChatGrpcKt.ChatCoroutineStub) {
        val userInfoBuilder = UserInfo.newBuilder()
            .setUserId(state.userId)
            .setGroupId(state.groupId)

        if (state.ackId >= 0) {
            userInfoBuilder.ackId = state.ackId
        }

        val serverInfo = stub.join(userInfoBuilder.build())
        state.ackId = serverInfo.clientAckId
        state.messages.removeIf { it.time <= serverInfo.time }

        println("Client connected to server")
    }

    private suspend fun messagesStream(stub: ChatGrpcKt.ChatCoroutineStub) {
        stub.messagesStream(messagesFlow()).collect { it.print() }
    }

    private suspend fun messagesFlow(): Flow<Message> = flow {
        if (first) {
            first = false
            state.messages.forEach { emit(it.toMessage(state)) }
        }

        while (true) {
            newMessageMutex.lock()

            if (end) {
                return@flow
            } else {
                emit(state.messages.last().toMessage(state))
            }
        }
    }

    private fun cliLoop() {
        while (true) {
            val input = readln()
            if (input.isEmpty()) {
                continue
            }

            var settings: String
            var text: String
            var priority = Priority.NORMAL
            var replyId: Long? = null
            var media: String? = null
            var mime: String? = null

            val inputArray = input.split('|')
            if (inputArray.size > 1) {
                settings = inputArray[0]
                text = inputArray[1]
            } else {
                settings = ""
                text = input
            }

            settings.split("#").forEach {
                if (it.isNotEmpty()) {
                    when (it[0].uppercase()) {
                        "P" -> priority = charToPriority(it[1])
                        "R" -> replyId = it.substring(1).toLong()
                        "M" -> {
                            val mediaArray = it.substring(1).split('&')
                            mime = mediaArray[0]
                            media = mediaArray[1]
                        }
                        "X" -> end = true
                    }
                }
            }

            if (!end) {
                state.messages.push(ClientMessage(replyId, priority, text, System.currentTimeMillis() / 1000, media, mime))
                newMessageMutex.unlock()
            } else {
                newMessageMutex.unlock()
                return
            }
        }
    }
}

suspend fun main(args: Array<String>) {
    val parser = ArgParser("gRPC-Chat client")
    val host by parser.option(ArgType.String, fullName = "host", description = "Server hostname").default("localhost")
    val port by parser.option(ArgType.Int, fullName = "port", description = "Server port").default(50051)
    val userId by parser.option(ArgType.Int, fullName = "user", description = "User Id").required()
    val groupId by parser.option(ArgType.Int, fullName = "group", description = "Group Id").required()
    parser.parse(args)

    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    val stub = ChatGrpcKt.ChatCoroutineStub(channel)

    Client(userId.toLong(), groupId.toLong()).use {
        it.start(stub)
    }

    channel.shutdown()
}