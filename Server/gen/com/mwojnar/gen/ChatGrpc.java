package com.mwojnar.gen;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.46.0)",
    comments = "Source: proto/chat.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ChatGrpc {

  private ChatGrpc() {}

  public static final String SERVICE_NAME = "chat.Chat";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.mwojnar.gen.Message,
      com.mwojnar.gen.Empty> getSendMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendMessage",
      requestType = com.mwojnar.gen.Message.class,
      responseType = com.mwojnar.gen.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<com.mwojnar.gen.Message,
      com.mwojnar.gen.Empty> getSendMessageMethod() {
    io.grpc.MethodDescriptor<com.mwojnar.gen.Message, com.mwojnar.gen.Empty> getSendMessageMethod;
    if ((getSendMessageMethod = ChatGrpc.getSendMessageMethod) == null) {
      synchronized (ChatGrpc.class) {
        if ((getSendMessageMethod = ChatGrpc.getSendMessageMethod) == null) {
          ChatGrpc.getSendMessageMethod = getSendMessageMethod =
              io.grpc.MethodDescriptor.<com.mwojnar.gen.Message, com.mwojnar.gen.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mwojnar.gen.Message.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mwojnar.gen.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new ChatMethodDescriptorSupplier("SendMessage"))
              .build();
        }
      }
    }
    return getSendMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mwojnar.gen.UserInfo,
      com.mwojnar.gen.Message> getJoinMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Join",
      requestType = com.mwojnar.gen.UserInfo.class,
      responseType = com.mwojnar.gen.Message.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.mwojnar.gen.UserInfo,
      com.mwojnar.gen.Message> getJoinMethod() {
    io.grpc.MethodDescriptor<com.mwojnar.gen.UserInfo, com.mwojnar.gen.Message> getJoinMethod;
    if ((getJoinMethod = ChatGrpc.getJoinMethod) == null) {
      synchronized (ChatGrpc.class) {
        if ((getJoinMethod = ChatGrpc.getJoinMethod) == null) {
          ChatGrpc.getJoinMethod = getJoinMethod =
              io.grpc.MethodDescriptor.<com.mwojnar.gen.UserInfo, com.mwojnar.gen.Message>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Join"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mwojnar.gen.UserInfo.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mwojnar.gen.Message.getDefaultInstance()))
              .setSchemaDescriptor(new ChatMethodDescriptorSupplier("Join"))
              .build();
        }
      }
    }
    return getJoinMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ChatStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ChatStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ChatStub>() {
        @java.lang.Override
        public ChatStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ChatStub(channel, callOptions);
        }
      };
    return ChatStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ChatBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ChatBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ChatBlockingStub>() {
        @java.lang.Override
        public ChatBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ChatBlockingStub(channel, callOptions);
        }
      };
    return ChatBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ChatFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ChatFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ChatFutureStub>() {
        @java.lang.Override
        public ChatFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ChatFutureStub(channel, callOptions);
        }
      };
    return ChatFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class ChatImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<com.mwojnar.gen.Message> sendMessage(
        io.grpc.stub.StreamObserver<com.mwojnar.gen.Empty> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getSendMessageMethod(), responseObserver);
    }

    /**
     */
    public void join(com.mwojnar.gen.UserInfo request,
        io.grpc.stub.StreamObserver<com.mwojnar.gen.Message> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getJoinMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSendMessageMethod(),
            io.grpc.stub.ServerCalls.asyncClientStreamingCall(
              new MethodHandlers<
                com.mwojnar.gen.Message,
                com.mwojnar.gen.Empty>(
                  this, METHODID_SEND_MESSAGE)))
          .addMethod(
            getJoinMethod(),
            io.grpc.stub.ServerCalls.asyncServerStreamingCall(
              new MethodHandlers<
                com.mwojnar.gen.UserInfo,
                com.mwojnar.gen.Message>(
                  this, METHODID_JOIN)))
          .build();
    }
  }

  /**
   */
  public static final class ChatStub extends io.grpc.stub.AbstractAsyncStub<ChatStub> {
    private ChatStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ChatStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ChatStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.mwojnar.gen.Message> sendMessage(
        io.grpc.stub.StreamObserver<com.mwojnar.gen.Empty> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getSendMessageMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public void join(com.mwojnar.gen.UserInfo request,
        io.grpc.stub.StreamObserver<com.mwojnar.gen.Message> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getJoinMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ChatBlockingStub extends io.grpc.stub.AbstractBlockingStub<ChatBlockingStub> {
    private ChatBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ChatBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ChatBlockingStub(channel, callOptions);
    }

    /**
     */
    public java.util.Iterator<com.mwojnar.gen.Message> join(
        com.mwojnar.gen.UserInfo request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getJoinMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ChatFutureStub extends io.grpc.stub.AbstractFutureStub<ChatFutureStub> {
    private ChatFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ChatFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ChatFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_JOIN = 0;
  private static final int METHODID_SEND_MESSAGE = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ChatImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ChatImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_JOIN:
          serviceImpl.join((com.mwojnar.gen.UserInfo) request,
              (io.grpc.stub.StreamObserver<com.mwojnar.gen.Message>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_MESSAGE:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.sendMessage(
              (io.grpc.stub.StreamObserver<com.mwojnar.gen.Empty>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class ChatBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ChatBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.mwojnar.gen.ChatProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Chat");
    }
  }

  private static final class ChatFileDescriptorSupplier
      extends ChatBaseDescriptorSupplier {
    ChatFileDescriptorSupplier() {}
  }

  private static final class ChatMethodDescriptorSupplier
      extends ChatBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ChatMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ChatGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ChatFileDescriptorSupplier())
              .addMethod(getSendMessageMethod())
              .addMethod(getJoinMethod())
              .build();
        }
      }
    }
    return result;
  }
}
