package com.mwojnar;

import com.mwojnar.gen.JoinResponse;
import com.mwojnar.gen.Message;
import com.mwojnar.gen.UserInfo;
import io.grpc.stub.StreamObserver;

import java.sql.Connection;
import java.util.logging.Logger;

public class ChatImpl extends com.mwojnar.gen.ChatGrpc.ChatImplBase {
    private final Logger logger;
    private final Connection connection;


    public ChatImpl(Logger logger, Connection connection) {
        super();

        this.logger = logger;
        this.connection = connection;
    }

    @Override
    public void join(UserInfo request, StreamObserver<JoinResponse> responseObserver) {
        super.join(request, responseObserver);
    }

    @Override
    public StreamObserver<Message> sendMessage(StreamObserver<Message> responseObserver) {
        return super.sendMessage(responseObserver);
    }
}
