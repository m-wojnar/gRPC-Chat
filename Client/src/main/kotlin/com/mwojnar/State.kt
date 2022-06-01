package com.mwojnar

import com.mwojnar.gen.Priority
import kotlinx.serialization.Serializable

@Serializable
data class ClientMessage(
    val replyId: Long?,
    val priority: Priority,
    val text: String,
    val time: Long,
    val media: String?,
    val mime: String?
)

@Serializable
data class ClientState(
    val userId: Long,
    val groupId: Long,
    var ackId: Long,
    val messages: ArrayList<ClientMessage>
)