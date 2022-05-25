package com.mwojnar;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class ChatServer {
    public static final int DEFAULT_PORT = 50051;
    public static final int DEFAULT_THREADS_NUM = 16;

    public static void main(String[] args) throws IOException, InterruptedException {
        final var server = new ChatServer(DEFAULT_PORT);
        server.start();
        server.blockUntilShutdown();
    }

    private final Logger logger;
    private final Server server;
    private final int port;

    public ChatServer(int port, int nThreads) {
        this.port = port;
        this.logger = Logger.getLogger(ChatServer.class.getName());
        this.server = ServerBuilder
                .forPort(port)
                .executor(Executors.newFixedThreadPool(nThreads))
                .addService(new ChatImpl())
                .build();
    }

    public ChatServer(int port) {
        this(port, DEFAULT_THREADS_NUM);
    }

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    private void start() throws IOException {
        this.server.start();
        this.logger.info("Server started, listening on " + this.port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.logger.info("Shutting down server.");
            this.stop();
            this.logger.info("Server shut down.");
        }));
    }

    private void stop() {
        if (this.server != null) {
            this.server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (this.server != null) {
            this.server.awaitTermination();
        }
    }
}
