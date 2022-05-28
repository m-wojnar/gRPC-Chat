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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import java.io.Closeable
import java.io.File
import java.util.*

class Client(userId: Long, groupId: Long) : Closeable {
    private val state: ClientState
    private val fileName: String
    private val startSemaphore: Semaphore
    private val missingMessagesSemaphore: Semaphore

    init {
        fileName = "saved/client_${userId}_group_${groupId}"
        state = if (File(fileName).exists()) {
            val bytes = File(fileName).readBytes()
            ProtoBuf.decodeFromByteArray(bytes)
        } else {
            ClientState(userId, groupId, -1, LinkedList())
        }

        startSemaphore = Semaphore(1)
        missingMessagesSemaphore = Semaphore(1)
    }

    suspend fun start(stub: ChatGrpcKt.ChatCoroutineStub) {
        startSemaphore.acquire()
        missingMessagesSemaphore.acquire()

        CoroutineScope(Dispatchers.Default).async { receiveMessages(stub) }

        var reconnect: Boolean
        do {
            reconnect = try {
                stub.sendMessage(sendMessages())
                false
            } catch (e: io.grpc.StatusException) {
                true
            }
        } while (reconnect)
    }

    override fun close() {
        // TODO client never reaches this method
        println("Client closed. Saving client state.")

        synchronized(this) {
            val bytes = ProtoBuf.encodeToByteArray(serializer(), state)
            File(fileName).writeBytes(bytes)
        }
    }

    private suspend fun receiveMessages(stub: ChatGrpcKt.ChatCoroutineStub) {
        var reconnect: Boolean
        do {
            val userInfoBuilder = UserInfo.newBuilder()
                .setUserId(state.userId)
                .setGroupId(state.groupId)

            if (state.ackId >= 0) {
                userInfoBuilder.ackId = state.ackId
            }

            reconnect = try {
                stub.join(userInfoBuilder.build()).collect { messagesHandler(it) }
                false
            } catch (e: io.grpc.StatusException) {
                println("Cannot connect to server. Reconnecting in 5 s...")
                delay(5000)
                true
            }
        } while (reconnect)
    }

    private fun messagesHandler(message: Message) {
        if (message.hasTime()) {
            state.messages.removeIf { it.time <= message.time }
        } else {
            println("Cannot remove old messages.")
        }

        if (!message.hasId()) {
            if (state.ackId == -1L) {
                state.ackId = message.ackId
            } else {
                missingMessagesSemaphore.release()
            }

            try {
                startSemaphore.release()
            } catch (_: java.lang.IllegalStateException) {
            }

            println("Client connected to server.")
        } else if (message.hasAckId() && state.ackId < message.ackId) {
            state.ackId = message.ackId
            message.print()
        }
    }

    private fun sendMessages(): Flow<Message> = flow {
        startSemaphore.acquire()

        while (true) {
            if (missingMessagesSemaphore.tryAcquire()) {
                println("Send missing messages.")
                state.messages.forEach { emit(it.toMessage(state)) }
            }

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
                        "X" -> return@flow
                    }
                }
            }

            val clientMessage = ClientMessage(replyId, priority, text, System.currentTimeMillis() / 1000, media, mime)
            state.messages.push(clientMessage)
            emit(clientMessage.toMessage(state))
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