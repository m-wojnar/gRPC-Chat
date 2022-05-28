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
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import java.io.Closeable
import java.io.File
import java.util.*
import kotlin.math.max

class Client(userId: Long, groupId: Long) : Closeable {
    private val state: ClientState
    private val fileName: String

    init {
        fileName = "saved/client_${userId}_group_${groupId}"
        state = if (File(fileName).exists()) {
            val bytes = File(fileName).readBytes()
            ProtoBuf.decodeFromByteArray(bytes)
        } else {
            ClientState(userId, groupId, -1, LinkedList<ClientMessage>())
        }
    }

    suspend fun start(stub: ChatGrpcKt.ChatCoroutineStub) {
        CoroutineScope(Dispatchers.Default).async { receiveMessages(stub) }

        var reconnect: Boolean
        do {
            reconnect = try {
                stub.sendMessage(sendMessages())
                false
            } catch (e: io.grpc.StatusException) {
                println("Cannot connect to server. Reconnecting in 5 s...")
                delay(5000)
                true
            }
        } while (reconnect)
    }

    override fun close() {
        synchronized(this) {
            val bytes = ProtoBuf.encodeToByteArray(serializer(), state)
            File(fileName).writeBytes(bytes)
        }
    }

    private suspend fun receiveMessages(stub: ChatGrpcKt.ChatCoroutineStub) {
        val userInfoBuilder = UserInfo.newBuilder()
            .setUserId(state.userId)
            .setGroupId(state.groupId)

        if (state.ackId >= 0) {
            userInfoBuilder.ackId = state.ackId
        }

        var reconnect: Boolean
        do {
            reconnect = try {
                stub.join(userInfoBuilder.build()).collect { messageHandler(it)}
                false
            } catch (e: io.grpc.StatusException) {
                println("Cannot connect to server. Reconnecting in 5 s...")
                delay(5000)
                true
            }
        } while (reconnect)
    }

    private fun messageHandler(message: Message) {
        if (!message.hasId() && message.hasAckId()) {
            if (state.ackId == -1L) {
                state.ackId = message.ackId
            }
            else {
                // TODO send missing messages
            }
        }
        else if (message.hasId() && message.hasAckId()) {
            state.ackId = max(state.ackId, message.ackId)
            message.print()
            // TODO remove old messages
        }
    }

    private fun sendMessages(): Flow<Message> = flow {
        while (true) {
            // TODO cli loop

            val text = readln()
            val time = System.currentTimeMillis() / 1000

            val request = Message.newBuilder()
                .setUserId(state.userId)
                .setAckId(state.ackId)
                .setPriority(Priority.NORMAL)
                .setText(text)
                .setTime(time)
                .build()

            val clientMessage = ClientMessage(
                null,
                Priority.NORMAL,
                text,
                time,
                null,
                null
            )
            state.messages.push(clientMessage)

            emit(request)
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