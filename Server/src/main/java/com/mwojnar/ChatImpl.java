package com.mwojnar;

import com.mwojnar.gen.JoinResponse;
import com.mwojnar.gen.Message;
import com.mwojnar.gen.UserInfo;
import io.grpc.stub.StreamObserver;

public class ChatImpl extends com.mwojnar.gen.ChatGrpc.ChatImplBase {
    @Override
    public void join(UserInfo request, StreamObserver<JoinResponse> responseObserver) {
        super.join(request, responseObserver);
    }

    @Override
    public StreamObserver<Message> sendMessage(StreamObserver<Message> responseObserver) {
        return super.sendMessage(responseObserver);
    }
}
