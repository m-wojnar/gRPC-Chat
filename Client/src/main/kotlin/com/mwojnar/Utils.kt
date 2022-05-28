package com.mwojnar

import com.google.protobuf.kotlin.toByteString
import com.mwojnar.gen.Message
import com.mwojnar.gen.Priority
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

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
        messageBuilder.media = Base64.getDecoder().decode(media).toByteString()
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

    val dateTime = LocalDateTime.ofEpochSecond(time, 0, OffsetDateTime.now().offset)
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")

    messageString += "${id}: [${priority}user $userId | ${dateTime.format(formatter)}] "
    messageString += text

    if (hasMedia() && hasMime()) {
        messageString += "\n${mime}: ${String(Base64.getEncoder().encode(media.toByteArray()))}"
    }

    println(messageString)
}

fun charToPriority(char: Char): Priority =
    when (char.uppercase()) {
        "H" -> Priority.HIGH
        "M" -> Priority.MEDIUM
        "L" -> Priority.LOW
        else -> Priority.NORMAL
    }
