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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

suspend fun receiveMessages(stub: ChatGrpcKt.ChatCoroutineStub, user: Int, group: Int) {
    val request = UserInfo.newBuilder()
        .setUserId(user.toLong())
        .setGroupId(group.toLong())
        .build()

    stub.join(request).collect {message ->
        println(message)
    }
}

fun sendMessages(user: Int): Flow<Message> = flow {
    while (true) {
        val text = readln()
        val request = Message.newBuilder()
            .setUserId(user.toLong())
            .setAckId(0)
            .setPriority(Priority.NORMAL)
            .setText(text)
            .setTime((System.currentTimeMillis() / 1000))
            .build()

        emit(request)
    }
}

suspend fun main(args: Array<String>) {
    val parser = ArgParser("gRPC-Chat client")
    val host by parser.option(ArgType.String, description = "Server hostname").default("localhost")
    val port by parser.option(ArgType.Int, description = "Server port").default(50051)
    val user by parser.option(ArgType.Int, description = "User Id").required()
    val group by parser.option(ArgType.Int, description = "Group Id").required()
    parser.parse(args)

    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    val stub = ChatGrpcKt.ChatCoroutineStub(channel)

    CoroutineScope(Dispatchers.Default).async { receiveMessages(stub, user, group) }
    stub.sendMessage(sendMessages(user))

    channel.shutdown()
}