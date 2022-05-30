package com.mwojnar.gen

import com.mwojnar.gen.ChatGrpc.getServiceDescriptor
import io.grpc.CallOptions
import io.grpc.CallOptions.DEFAULT
import io.grpc.Channel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.ServerServiceDefinition.builder
import io.grpc.ServiceDescriptor
import io.grpc.Status
import io.grpc.Status.UNIMPLEMENTED
import io.grpc.StatusException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls.bidiStreamingRpc
import io.grpc.kotlin.ClientCalls.unaryRpc
import io.grpc.kotlin.ServerCalls.bidiStreamingServerMethodDefinition
import io.grpc.kotlin.ServerCalls.unaryServerMethodDefinition
import io.grpc.kotlin.StubFor
import kotlin.String
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.Flow

/**
 * Holder for Kotlin coroutine-based client and server APIs for chat.Chat.
 */
object ChatGrpcKt {
  const val SERVICE_NAME: String = ChatGrpc.SERVICE_NAME

  @JvmStatic
  val serviceDescriptor: ServiceDescriptor
    get() = ChatGrpc.getServiceDescriptor()

  val joinMethod: MethodDescriptor<UserInfo, ServerInfo>
    @JvmStatic
    get() = ChatGrpc.getJoinMethod()

  val messagesStreamMethod: MethodDescriptor<Message, Message>
    @JvmStatic
    get() = ChatGrpc.getMessagesStreamMethod()

  /**
   * A stub for issuing RPCs to a(n) chat.Chat service as suspending coroutines.
   */
  @StubFor(ChatGrpc::class)
  class ChatCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT
  ) : AbstractCoroutineStub<ChatCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): ChatCoroutineStub =
        ChatCoroutineStub(channel, callOptions)

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    suspend fun join(request: UserInfo, headers: Metadata = Metadata()): ServerInfo = unaryRpc(
      channel,
      ChatGrpc.getJoinMethod(),
      request,
      callOptions,
      headers
    )
    /**
     * Returns a [Flow] that, when collected, executes this RPC and emits responses from the
     * server as they arrive.  That flow finishes normally if the server closes its response with
     * [`Status.OK`][Status], and fails by throwing a [StatusException] otherwise.  If
     * collecting the flow downstream fails exceptionally (including via cancellation), the RPC
     * is cancelled with that exception as a cause.
     *
     * The [Flow] of requests is collected once each time the [Flow] of responses is
     * collected. If collection of the [Flow] of responses completes normally or
     * exceptionally before collection of `requests` completes, the collection of
     * `requests` is cancelled.  If the collection of `requests` completes
     * exceptionally for any other reason, then the collection of the [Flow] of responses
     * completes exceptionally for the same reason and the RPC is cancelled with that reason.
     *
     * @param requests A [Flow] of request messages.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return A flow that, when collected, emits the responses from the server.
     */
    fun messagesStream(requests: Flow<Message>, headers: Metadata = Metadata()): Flow<Message> =
        bidiStreamingRpc(
      channel,
      ChatGrpc.getMessagesStreamMethod(),
      requests,
      callOptions,
      headers
    )}

  /**
   * Skeletal implementation of the chat.Chat service based on Kotlin coroutines.
   */
  abstract class ChatCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for chat.Chat.Join.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC
     * will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    open suspend fun join(request: UserInfo): ServerInfo = throw
        StatusException(UNIMPLEMENTED.withDescription("Method chat.Chat.Join is unimplemented"))

    /**
     * Returns a [Flow] of responses to an RPC for chat.Chat.MessagesStream.
     *
     * If creating or collecting the returned flow fails with a [StatusException], the RPC
     * will fail with the corresponding [Status].  If it fails with a
     * [java.util.concurrent.CancellationException], the RPC will fail with status
     * `Status.CANCELLED`.  If creating
     * or collecting the returned flow fails for any other reason, the RPC will fail with
     * `Status.UNKNOWN` with the exception as a cause.
     *
     * @param requests A [Flow] of requests from the client.  This flow can be
     *        collected only once and throws [java.lang.IllegalStateException] on attempts to
     * collect
     *        it more than once.
     */
    open fun messagesStream(requests: Flow<Message>): Flow<Message> = throw
        StatusException(UNIMPLEMENTED.withDescription("Method chat.Chat.MessagesStream is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = ChatGrpc.getJoinMethod(),
      implementation = ::join
    ))
      .addMethod(bidiStreamingServerMethodDefinition(
      context = this.context,
      descriptor = ChatGrpc.getMessagesStreamMethod(),
      implementation = ::messagesStream
    )).build()
  }
}
