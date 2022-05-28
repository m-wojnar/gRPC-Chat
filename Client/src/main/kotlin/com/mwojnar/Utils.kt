package com.mwojnar

import com.google.protobuf.kotlin.toByteStringUtf8
import com.mwojnar.gen.Message
import com.mwojnar.gen.Priority
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun ClientMessage.toMessage(clientState: ClientState): Message {
    val messageBuilder = Message.newBuilder()
        .setAckId(clientState.ackId)
        .setUserId(clientState.userId)
        .setPriority(priority)
        .setText(text)
        .setTime(time)

    if (replyId != null) {
        messageBuilder.replyId = replyId
    }

    if (media != null && mime != null) {
        messageBuilder.media = media.toByteStringUtf8()
        messageBuilder.mime = mime
    }

    return messageBuilder.build()
}

fun Message.print() {
    var messageString = ""

    if (hasReplyId()) {
        messageString += "$replyId <- "
    }

    val priority = when (priority) {
        Priority.HIGH -> "H! | "
        Priority.MEDIUM -> "M | "
        Priority.NORMAL -> "N | "
        Priority.LOW -> "L | "
        else -> ""
    }

    val dateTime = LocalDateTime.ofEpochSecond(time, 0, ZoneOffset.UTC)
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")

    messageString += "[${priority}user ${userId} | ${dateTime.format(formatter)}] "
    messageString += text

    if (hasMedia() && hasMime()) {
        messageString += "\n${mime}: ${media}"
    }

    println(messageString)
}